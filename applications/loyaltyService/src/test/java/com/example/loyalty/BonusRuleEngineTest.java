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
}
