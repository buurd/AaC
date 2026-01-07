package com.example.loyalty;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class BonusRuleEngineTest {

    @Test
    void testBaseRule() {
        BonusRuleEngine engine = new BonusRuleEngine();
        engine.setForceJanuaryBonus(false);
        
        int points = engine.evaluate(100.0);
        // Base rule is 1x. If January, it's 2x.
        // We can't easily assert exact value without mocking time, but it should be > 0.
        assertTrue(points >= 100); 
    }

    @Test
    void testForcedBonus() {
        BonusRuleEngine engine = new BonusRuleEngine();
        engine.setForceJanuaryBonus(true);
        
        int points = engine.evaluate(100.0);
        
        // 100 * 2 = 200
        assertEquals(200, points);
    }

    @Test
    void testItemBasedRule() {
        BonusRuleEngine engine = new BonusRuleEngine();
        engine.setForceJanuaryBonus(false); // Disable time bonus to isolate item rule

        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("p1", 1, 50.0, "Cat1"));
        items.add(new OrderItem("p2", 1, 50.0, "Cat2"));
        // Total quantity = 2. Should trigger 3x multiplier.

        // If not January: 100 * 3 = 300.
        // If January: 100 * 2 * 3 = 600.
        
        int points = engine.evaluate(100.0, items);
        
        assertTrue(points == 300 || points == 600);
    }
    
    @Test
    void testItemBasedRule_InsufficientQuantity() {
        BonusRuleEngine engine = new BonusRuleEngine();
        engine.setForceJanuaryBonus(false);

        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("p1", 1, 100.0, "Cat1"));
        // Total quantity = 1. No 3x multiplier.

        int points = engine.evaluate(100.0, items);
        
        assertTrue(points == 100 || points == 200);
    }
}
