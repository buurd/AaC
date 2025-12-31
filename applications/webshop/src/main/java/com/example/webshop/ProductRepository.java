package com.example.webshop;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductRepository {

    private final Connection connection;

    public ProductRepository(Connection connection) {
        this.connection = connection;
    }

    public List<Product> findAll() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT id, pm_id, type, name, description, price, unit, stock FROM products ORDER BY id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                products.add(new Product(
                    rs.getInt("id"),
                    (Integer) rs.getObject("pm_id"),
                    rs.getString("type"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getDouble("price"),
                    rs.getString("unit"),
                    rs.getInt("stock")
                ));
            }
        }
        return products;
    }

    public void upsert(Product product) throws SQLException {
        // Check if exists by pm_id
        String checkSql = "SELECT id FROM products WHERE pm_id = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setInt(1, product.getPmId());
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    // Update (preserve stock if not provided, but here we assume product sync doesn't touch stock)
                    String updateSql = "UPDATE products SET type=?, name=?, description=?, price=?, unit=? WHERE pm_id=?";
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                        updateStmt.setString(1, product.getType());
                        updateStmt.setString(2, product.getName());
                        updateStmt.setString(3, product.getDescription());
                        updateStmt.setDouble(4, product.getPrice());
                        updateStmt.setString(5, product.getUnit());
                        updateStmt.setInt(6, product.getPmId());
                        updateStmt.executeUpdate();
                    }
                } else {
                    // Insert (default stock 0)
                    String insertSql = "INSERT INTO products (pm_id, type, name, description, price, unit, stock) VALUES (?, ?, ?, ?, ?, ?, 0)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                        insertStmt.setInt(1, product.getPmId());
                        insertStmt.setString(2, product.getType());
                        insertStmt.setString(3, product.getName());
                        insertStmt.setString(4, product.getDescription());
                        insertStmt.setDouble(5, product.getPrice());
                        insertStmt.setString(6, product.getUnit());
                        insertStmt.executeUpdate();
                    }
                }
            }
        }
    }

    public void updateStock(int pmId, int stock) throws SQLException {
        String sql = "UPDATE products SET stock = ? WHERE pm_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, stock);
            stmt.setInt(2, pmId);
            stmt.executeUpdate();
        }
    }

    public void deleteByPmId(int pmId) throws SQLException {
        String sql = "DELETE FROM products WHERE pm_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, pmId);
            stmt.executeUpdate();
        }
    }
}
