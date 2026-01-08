package com.example.loyalty;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

public class BonusRuleEngine {

    private final Clock clock;
    private boolean forceJanuaryBonus = false;

    public BonusRuleEngine() {
        this(Clock.systemDefaultZone());
    }

    public BonusRuleEngine(Clock clock) {
        this.clock = clock;
    }

    public int evaluate(double orderAmount, List<OrderItem> items) {
        int points = (int) orderAmount; // Base rule: 1 point per 1 unit currency

        double multiplier = 1.0;

        // Time-based Rule: Double points in January OR if forced by Admin
        if (LocalDate.now(clock).getMonth() == Month.JANUARY || forceJanuaryBonus) {
            multiplier *= 2.0;
        }

        if (items != null) {
            int totalQuantity = items.stream().mapToInt(OrderItem::getQuantity).sum();
            long distinctProducts = items.stream().map(OrderItem::getProductId).distinct().count();

            // Rule: Get double loyalty if you buy three (quantity >= 3)
            if (totalQuantity >= 3) {
                multiplier *= 2.0;
            }

            // Rule: Buy three products (distinct) and get 1.5 loyalty
            if (distinctProducts >= 3) {
                multiplier *= 1.5;
            }
        }

        return (int) (points * multiplier);
    }

    // Overload for backward compatibility
    public int evaluate(double orderAmount) {
        return evaluate(orderAmount, null);
    }

    public void setForceJanuaryBonus(boolean force) {
        this.forceJanuaryBonus = force;
    }

    public boolean isForceJanuaryBonus() {
        return forceJanuaryBonus;
    }

    public List<String> getRuleDescriptions() {
        List<String> descriptions = new ArrayList<>();
        descriptions.add("Base Rule: 1 Point per 1 EUR");
        if (LocalDate.now(clock).getMonth() == Month.JANUARY || forceJanuaryBonus) {
            descriptions.add("January Bonus");
        }
        descriptions.add("Double Points on 3+ Items");
        descriptions.add("1.5x Points on 3+ Distinct Items");
        return descriptions;
    }
}
