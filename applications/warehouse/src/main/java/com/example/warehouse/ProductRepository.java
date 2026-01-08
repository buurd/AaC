package com.example.warehouse;

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
        String sql = "SELECT id, pm_id, name FROM products ORDER BY id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                products.add(new Product(
                    rs.getInt("id"),
                    (Integer) rs.getObject("pm_id"),
                    rs.getString("name")
                ));
            }
        }
        return products;
    }

    public Product findById(int id) throws SQLException {
        String sql = "SELECT id, pm_id, name FROM products WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Product(
                        rs.getInt("id"),
                        (Integer) rs.getObject("pm_id"),
                        rs.getString("name")
                    );
                }
            }
        }
        return null;
    }

    public Product findByPmId(int pmId) throws SQLException {
        String sql = "SELECT id, pm_id, name FROM products WHERE pm_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, pmId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Product(
                        rs.getInt("id"),
                        (Integer) rs.getObject("pm_id"),
                        rs.getString("name")
                    );
                }
            }
        }
        return null;
    }

    public void upsert(Product product) throws SQLException {
        String checkSql = "SELECT id FROM products WHERE pm_id = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setInt(1, product.getPmId());
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    // Update
                    String updateSql = "UPDATE products SET name=? WHERE pm_id=?";
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                        updateStmt.setString(1, product.getName());
                        updateStmt.setInt(2, product.getPmId());
                        updateStmt.executeUpdate();
                    }
                } else {
                    // Insert
                    String insertSql = "INSERT INTO products (pm_id, name) VALUES (?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                        insertStmt.setInt(1, product.getPmId());
                        insertStmt.setString(2, product.getName());
                        insertStmt.executeUpdate();
                    }
                }
            }
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
