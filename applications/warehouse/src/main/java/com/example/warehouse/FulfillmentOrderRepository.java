package com.example.warehouse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class FulfillmentOrderRepository {

    private final Connection connection;

    public FulfillmentOrderRepository(Connection connection) {
        this.connection = connection;
    }

    public void createFulfillmentOrder(int orderId) throws SQLException {
        // Check if exists (Standard SQL to avoid ON CONFLICT issues in H2)
        String checkSql = "SELECT id FROM fulfillment_orders WHERE order_id = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setInt(1, orderId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    return; // Already exists
                }
            }
        }

        String sql = "INSERT INTO fulfillment_orders (order_id, status) VALUES (?, 'PENDING')";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            stmt.executeUpdate();
        }
    }

    public List<FulfillmentOrder> findAllFulfillmentOrders() throws SQLException {
        List<FulfillmentOrder> list = new ArrayList<>();
        String sql = "SELECT id, order_id, status FROM fulfillment_orders ORDER BY id DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new FulfillmentOrder(
                    rs.getInt("id"),
                    rs.getInt("order_id"),
                    rs.getString("status")
                ));
            }
        }
        return list;
    }

    public void updateFulfillmentStatus(int orderId, String status) throws SQLException {
        String sql = "UPDATE fulfillment_orders SET status = ? WHERE order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, orderId);
            stmt.executeUpdate();
        }
    }
}
