package com.example.loyalty;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class LoyaltyRepositoryTest {

    // Enable PostgreSQL compatibility mode for ON CONFLICT support
    private static final String DB_URL = "jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private LoyaltyRepository repository;

    @BeforeEach
    void setUp() {
        repository = new LoyaltyRepository(DB_URL, DB_USER, DB_PASSWORD);
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS loyalty_points");
        }
    }

    @Test
    void testAddAndGetPoints() {
        String customerId = "user1";
        repository.addPoints(customerId, 100);
        
        int points = repository.getPoints(customerId);
        assertEquals(100, points);
        
        repository.addPoints(customerId, 50);
        points = repository.getPoints(customerId);
        assertEquals(150, points);
    }

    @Test
    void testGetPointsNotFound() {
        int points = repository.getPoints("unknown");
        assertEquals(0, points);
    }

    @Test
    void testRedeemPointsSuccess() {
        String customerId = "user2";
        repository.addPoints(customerId, 200);
        
        boolean success = repository.redeemPoints(customerId, 50);
        assertTrue(success);
        
        int points = repository.getPoints(customerId);
        assertEquals(150, points);
    }

    @Test
    void testRedeemPointsInsufficientFunds() {
        String customerId = "user3";
        repository.addPoints(customerId, 50);
        
        boolean success = repository.redeemPoints(customerId, 100);
        assertFalse(success);
        
        int points = repository.getPoints(customerId);
        assertEquals(50, points);
    }

    @Test
    void testRedeemPointsUserNotFound() {
        boolean success = repository.redeemPoints("unknown", 50);
        assertFalse(success);
    }

    @Test
    void testGetTotalPointsIssued() {
        repository.addPoints("userA", 100);
        repository.addPoints("userB", 200);
        
        long total = repository.getTotalPointsIssued();
        assertEquals(300, total);
    }
}
