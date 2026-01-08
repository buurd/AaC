package com.example.loyalty;

import java.util.List;

public class PointService {

    private final LoyaltyRepository repository;
    private final BonusRuleEngine ruleEngine;

    public PointService(LoyaltyRepository repository, BonusRuleEngine ruleEngine) {
        this.repository = repository;
        this.ruleEngine = ruleEngine;
    }

    public int accruePoints(String customerId, double orderAmount, List<OrderItem> items) {
        int points = ruleEngine.evaluate(orderAmount, items);
        repository.addPoints(customerId, points);
        System.out.println("Accrued " + points + " points for customer " + customerId);
        return points;
    }

    // Overload for backward compatibility/testing
    public int accruePoints(String customerId, double orderAmount) {
        return accruePoints(customerId, orderAmount, null);
    }

    public boolean redeemPoints(String customerId, int points) {
        boolean success = repository.redeemPoints(customerId, points);
        if (success) {
            System.out.println("Redeemed " + points + " points for customer " + customerId);
        } else {
            System.out.println("Failed to redeem " + points + " points for customer " + customerId);
        }
        return success;
    }

    public int getBalance(String customerId) {
        return repository.getPoints(customerId);
    }

    public long getTotalPointsIssued() {
        return repository.getTotalPointsIssued();
    }
}
