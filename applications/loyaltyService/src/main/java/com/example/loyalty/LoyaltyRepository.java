package com.example.loyalty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class LoyaltyRepository {

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public LoyaltyRepository(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS loyalty_points (" +
                         "customer_id VARCHAR(255) PRIMARY KEY, " +
                         "points INT NOT NULL DEFAULT 0" +
                         ")";
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    public int getPoints(String customerId) {
        String sql = "SELECT points FROM loyalty_points WHERE customer_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int points = rs.getInt("points");
                    System.out.println("LoyaltyRepository: getPoints for " + customerId + " -> " + points);
                    return points;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("LoyaltyRepository: getPoints for " + customerId + " -> 0 (not found)");
        return 0; // Default if not found
    }

    public void addPoints(String customerId, int pointsToAdd) {
        System.out.println("LoyaltyRepository: Adding " + pointsToAdd + " points to " + customerId);
        
        // Try update first (standard SQL, works on H2 and Postgres)
        String updateSql = "UPDATE loyalty_points SET points = points + ? WHERE customer_id = ?";
        String insertSql = "INSERT INTO loyalty_points (customer_id, points) VALUES (?, ?)";
        
        try (Connection conn = getConnection()) {
            boolean updated = false;
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setInt(1, pointsToAdd);
                updateStmt.setString(2, customerId);
                int rows = updateStmt.executeUpdate();
                updated = (rows > 0);
            }
            
            if (!updated) {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, customerId);
                    insertStmt.setInt(2, pointsToAdd);
                    insertStmt.executeUpdate();
                }
            }
            System.out.println("LoyaltyRepository: addPoints finished for " + customerId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean redeemPoints(String customerId, int pointsToRedeem) {
        System.out.println("LoyaltyRepository: Redeeming " + pointsToRedeem + " points from " + customerId);
        // Transactional check and update
        String checkSql = "SELECT points FROM loyalty_points WHERE customer_id = ?";
        String updateSql = "UPDATE loyalty_points SET points = points - ? WHERE customer_id = ?";
        
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, customerId);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    int currentPoints = rs.getInt("points");
                    System.out.println("LoyaltyRepository: Current points for " + customerId + ": " + currentPoints);
                    if (currentPoints >= pointsToRedeem) {
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setInt(1, pointsToRedeem);
                            updateStmt.setString(2, customerId);
                            updateStmt.executeUpdate();
                        }
                        conn.commit();
                        System.out.println("LoyaltyRepository: Redeemed successfully for " + customerId);
                        return true;
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("LoyaltyRepository: Redemption failed for " + customerId);
        return false;
    }

    public long getTotalPointsIssued() {
        String sql = "SELECT SUM(points) FROM loyalty_points";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
