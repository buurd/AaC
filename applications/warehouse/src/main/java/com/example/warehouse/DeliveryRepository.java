package com.example.warehouse;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DeliveryRepository {

    private final Connection connection;

    public DeliveryRepository(Connection connection) {
        this.connection = connection;
    }

    public List<Delivery> findAll() throws SQLException {
        List<Delivery> deliveries = new ArrayList<>();
        String sql = "SELECT id, sender FROM deliveries ORDER BY id DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Delivery d = new Delivery(rs.getInt("id"), rs.getString("sender"));
                d.setIndividuals(findIndividualsByDeliveryId(d.getId()));
                deliveries.add(d);
            }
        }
        return deliveries;
    }

    public Delivery findById(int id) throws SQLException {
        String sql = "SELECT id, sender FROM deliveries WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Delivery d = new Delivery(rs.getInt("id"), rs.getString("sender"));
                    d.setIndividuals(findIndividualsByDeliveryId(d.getId()));
                    return d;
                }
            }
        }
        return null;
    }

    public int createDelivery(String sender) throws SQLException {
        String sql = "INSERT INTO deliveries (sender) VALUES (?) RETURNING id";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, sender);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to create delivery");
    }

    public void addIndividual(ProductIndividual individual) throws SQLException {
        String sql = "INSERT INTO product_individuals (delivery_id, product_id, serial_number, state) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, individual.getDeliveryId());
            stmt.setInt(2, individual.getProductId());
            stmt.setString(3, individual.getSerialNumber());
            stmt.setString(4, individual.getState());
            stmt.executeUpdate();
        }
    }

    public void deleteDelivery(int id) throws SQLException {
        String sql = "DELETE FROM deliveries WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public int countStock(int productId) throws SQLException {
        // Count only 'New' items
        String sql = "SELECT COUNT(*) FROM product_individuals WHERE product_id = ? AND state = 'New'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public synchronized boolean reserveStock(int productId, int quantity) throws SQLException {
        // Check availability
        int available = countStock(productId);
        if (available < quantity) {
            return false;
        }

        // Reserve items (Update state to 'Reserved')
        // We use a subquery to select specific IDs to update
        String sql = "UPDATE product_individuals SET state = 'Reserved' WHERE id IN (" +
                     "SELECT id FROM product_individuals WHERE product_id = ? AND state = 'New' LIMIT ?" +
                     ")";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            stmt.setInt(2, quantity);
            int updated = stmt.executeUpdate();
            return updated == quantity;
        }
    }

    private List<ProductIndividual> findIndividualsByDeliveryId(int deliveryId) throws SQLException {
        List<ProductIndividual> list = new ArrayList<>();
        String sql = "SELECT id, delivery_id, product_id, serial_number, state FROM product_individuals WHERE delivery_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, deliveryId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new ProductIndividual(
                        rs.getInt("id"),
                        rs.getInt("delivery_id"),
                        rs.getInt("product_id"),
                        rs.getString("serial_number"),
                        rs.getString("state")
                    ));
                }
            }
        }
        return list;
    }
}
