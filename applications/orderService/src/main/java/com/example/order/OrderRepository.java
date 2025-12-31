package com.example.order;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderRepository {

    private final Connection connection;

    public OrderRepository(Connection connection) {
        this.connection = connection;
    }

    public List<Order> findAll() throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT id, customer_name, status FROM orders ORDER BY id DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Order o = new Order(rs.getInt("id"), rs.getString("customer_name"), rs.getString("status"));
                o.setItems(findItemsByOrderId(o.getId()));
                orders.add(o);
            }
        }
        System.out.println("OrderRepository: Found " + orders.size() + " orders.");
        return orders;
    }

    public int createOrder(Order order) throws SQLException {
        String sql = "INSERT INTO orders (customer_name, status) VALUES (?, ?) RETURNING id";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, order.getCustomerName());
            stmt.setString(2, order.getStatus());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int orderId = rs.getInt(1);
                    System.out.println("OrderRepository: Created order with ID " + orderId);
                    for (OrderItem item : order.getItems()) {
                        createOrderItem(orderId, item);
                    }
                    return orderId;
                }
            }
        }
        throw new SQLException("Failed to create order");
    }

    private void createOrderItem(int orderId, OrderItem item) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, product_id, quantity) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            stmt.setInt(2, item.getProductId());
            stmt.setInt(3, item.getQuantity());
            stmt.executeUpdate();
        }
    }

    public void updateStatus(int orderId, String status) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, orderId);
            int rows = stmt.executeUpdate();
            System.out.println("OrderRepository: Updated status for order " + orderId + " to " + status + ". Rows affected: " + rows);
        }
    }

    private List<OrderItem> findItemsByOrderId(int orderId) throws SQLException {
        List<OrderItem> list = new ArrayList<>();
        String sql = "SELECT id, product_id, quantity FROM order_items WHERE order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setId(rs.getInt("id"));
                    item.setOrderId(orderId);
                    item.setProductId(rs.getInt("product_id"));
                    item.setQuantity(rs.getInt("quantity"));
                    list.add(item);
                }
            }
        }
        return list;
    }
}
