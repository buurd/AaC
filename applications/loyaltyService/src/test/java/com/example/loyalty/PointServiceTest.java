package com.example.loyalty;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private LoyaltyRepository repository;

    @Mock
    private BonusRuleEngine ruleEngine;

    @InjectMocks
    private PointService pointService;

    @Test
    void testAccruePoints() {
        String customerId = "user1";
        double amount = 100.0;
        int expectedPoints = 100;

        when(ruleEngine.evaluate(amount)).thenReturn(expectedPoints);

        pointService.accruePoints(customerId, amount);

        verify(repository).addPoints(customerId, expectedPoints);
    }

    @Test
    void testRedeemPointsSuccess() {
        String customerId = "user1";
        int points = 50;

        when(repository.redeemPoints(customerId, points)).thenReturn(true);

        boolean result = pointService.redeemPoints(customerId, points);

        assertTrue(result);
        verify(repository).redeemPoints(customerId, points);
    }

    @Test
    void testRedeemPointsFailure() {
        String customerId = "user1";
        int points = 50;

        when(repository.redeemPoints(customerId, points)).thenReturn(false);

        boolean result = pointService.redeemPoints(customerId, points);

        assertFalse(result);
        verify(repository).redeemPoints(customerId, points);
    }
}
