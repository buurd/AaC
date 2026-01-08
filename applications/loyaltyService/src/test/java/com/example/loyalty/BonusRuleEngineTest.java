package com.example.loyalty;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class BonusRuleEngineTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2023-06-01T10:00:00Z"), ZoneId.of("UTC"));
    private final Clock januaryClock = Clock.fixed(Instant.parse("2023-01-01T10:00:00Z"), ZoneId.of("UTC"));

    @Test
    void testBaseRule() {
        BonusRuleEngine engine = new BonusRuleEngine(fixedClock);
        engine.setForceJanuaryBonus(false);
        
        int points = engine.evaluate(100.0);
        // Base rule is 1x. Not January.
        assertEquals(100, points); 
    }

    @Test
    void testForcedBonus() {
        BonusRuleEngine engine = new BonusRuleEngine(fixedClock);
        engine.setForceJanuaryBonus(true);
        assertTrue(engine.isForceJanuaryBonus());
        
        int points = engine.evaluate(100.0);
        
        // 100 * 2 = 200
        assertEquals(200, points);
    }

    @Test
    void testJanuaryBonus() {
        BonusRuleEngine engine = new BonusRuleEngine(januaryClock);
        engine.setForceJanuaryBonus(false);
        
        int points = engine.evaluate(100.0);
        
        // 100 * 2 = 200
        assertEquals(200, points);
    }

    @Test
    void testItemBasedRule() {
        BonusRuleEngine engine = new BonusRuleEngine(fixedClock);
        engine.setForceJanuaryBonus(false); // Disable time bonus to isolate item rule

        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("p1", 1, 33.33, "Cat1"));
        items.add(new OrderItem("p2", 1, 33.33, "Cat2"));
        items.add(new OrderItem("p3", 1, 33.34, "Cat3"));
        // Total quantity = 3. Should trigger 3x multiplier (2x for qty >= 3, 1.5x for distinct >= 3).
        // 100 * 2 * 1.5 = 300.
        
        int points = engine.evaluate(100.0, items);
        
        assertEquals(300, points);
    }
    
    @Test
    void testItemBasedRule_InsufficientQuantity() {
        BonusRuleEngine engine = new BonusRuleEngine(fixedClock);
        engine.setForceJanuaryBonus(false);

        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("p1", 1, 100.0, "Cat1"));
        // Total quantity = 1. No 3x multiplier.

        int points = engine.evaluate(100.0, items);
        
        assertEquals(100, points);
    }

    @Test
    void testItemBasedRule_QuantityBonusOnly() {
        BonusRuleEngine engine = new BonusRuleEngine(fixedClock);
        List<OrderItem> items = new ArrayList<>();
        // 3 items of same product
        items.add(new OrderItem("p1", 3, 100.0, "Cat1"));
        
        // Qty >= 3 -> 2x multiplier
        // Distinct < 3 -> 1x multiplier
        // Total: 2x
        
        int points = engine.evaluate(100.0, items);
        assertEquals(200, points);
    }

    @Test
    void testRuleDescriptions() {
        BonusRuleEngine engine = new BonusRuleEngine(fixedClock);
        engine.setForceJanuaryBonus(false);
        
        List<String> rules = engine.getRuleDescriptions();
        assertTrue(rules.contains("Base Rule: 1 Point per 1 EUR"));
        assertFalse(rules.contains("January Bonus"));
        
        engine.setForceJanuaryBonus(true);
        rules = engine.getRuleDescriptions();
        assertTrue(rules.contains("January Bonus"));
    }
    
    @Test
    void testDefaultConstructor() {
        // Just to cover the default constructor
        BonusRuleEngine engine = new BonusRuleEngine();
        assertNotNull(engine);
    }
}
