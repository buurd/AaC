package com.example.loyalty;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class LoyaltyProgramTest {

    private LoyaltyRepository repository;
    private BonusRuleEngine ruleEngine;
    private PointService pointService;

    @BeforeEach
    void setUp() {
        repository = mock(LoyaltyRepository.class);
        // Fix time to June 1st, 2023 (Not January)
        Clock fixedClock = Clock.fixed(Instant.parse("2023-06-01T10:00:00Z"), ZoneId.of("UTC"));
        ruleEngine = new BonusRuleEngine(fixedClock);
        pointService = new PointService(repository, ruleEngine);
    }

    @Test
    void test1_EarnAndBurn() {
        // Scenario: Shop articles, earn points, verify balance, use points.
        String customerId = "shopper1";
        
        // 1. Shop a number of articles (Total 100 EUR)
        // Base rule: 1 point per Euro.
        double orderAmount = 100.0;
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("p1", 1, 50.0, "Cat1"));
        items.add(new OrderItem("p2", 1, 50.0, "Cat2"));
        
        // Ensure no multipliers active
        ruleEngine.setForceJanuaryBonus(false);

        // Act: Accrue points
        pointService.accruePoints(customerId, orderAmount, items);

        // Verify: 100 points added
        verify(repository).addPoints(customerId, 100);

        // Mock repository behavior for balance check
        when(repository.getPoints(customerId)).thenReturn(100);

        // 2. Verify shopper can see collected loyalty
        int balance = pointService.getBalance(customerId);
        assertEquals(100, balance);

        // 3. Use loyalty to shop something else (Redeem 50 points)
        when(repository.redeemPoints(customerId, 50)).thenReturn(true);
        boolean redeemed = pointService.redeemPoints(customerId, 50);
        
        // Verify redemption
        assertEquals(true, redeemed);
        verify(repository).redeemPoints(customerId, 50);
    }

    @Test
    void test2_ComplexRuleStacking() {
        // Scenario: Create a cart where all three loyalty programs are in effect.
        // Rules:
        // 1. Default: 1 point per Euro.
        // 2. Double loyalty if you buy three (quantity >= 3).
        // 3. Buy three products (distinct) and get 1.5 loyalty.
        
        String customerId = "shopper2";
        ruleEngine.setForceJanuaryBonus(false); // Disable time-based rule to focus on item rules

        // Create cart with 3 distinct products, total quantity 3 (1 of each)
        // This satisfies Rule 2 (qty >= 3) AND Rule 3 (distinct >= 3).
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("p1", 1, 10.0, "Cat1"));
        items.add(new OrderItem("p2", 1, 10.0, "Cat2"));
        items.add(new OrderItem("p3", 1, 10.0, "Cat3"));
        
        double orderAmount = 30.0; // 3 items * 10 EUR

        // Calculation:
        // Base points: 30
        // Multiplier 1 (Qty >= 3): * 2.0
        // Multiplier 2 (Distinct >= 3): * 1.5
        // Total Multiplier: 1.0 * 2.0 * 1.5 = 3.0
        // Expected Points: 30 * 3.0 = 90
        
        // Act
        pointService.accruePoints(customerId, orderAmount, items);

        // Verify
        ArgumentCaptor<Integer> pointsCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(repository).addPoints(eq(customerId), pointsCaptor.capture());
        
        assertEquals(90, pointsCaptor.getValue());
    }

    @Test
    void test2_ComplexRuleStacking_WithRounding() {
        // Scenario: Verify rounding behavior (rounded down to whole number).
        // Same rules, but amount that results in decimal.
        
        String customerId = "shopper3";
        ruleEngine.setForceJanuaryBonus(false);

        // Create cart with 3 distinct products
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("p1", 1, 10.0, "Cat1"));
        items.add(new OrderItem("p2", 1, 10.0, "Cat2"));
        items.add(new OrderItem("p3", 1, 11.0, "Cat3")); // Total 31
        
        double orderAmount = 31.0;

        // Calculation:
        // Base points: 31
        // Multiplier: 3.0
        // Expected Points: 31 * 3.0 = 93.0 -> 93
        
        pointService.accruePoints(customerId, orderAmount, items);
        verify(repository).addPoints(customerId, 93);
    }
}
