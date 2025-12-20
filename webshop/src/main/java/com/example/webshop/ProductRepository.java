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
        String sql = "SELECT id, pm_id, type, name, description, price, unit FROM products ORDER BY id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                Integer pmId = (Integer) rs.getObject("pm_id");
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                
                System.out.println("DB Read: ID=" + id + ", PM_ID=" + pmId + ", Name=" + name + ", Price=" + price);
                
                products.add(new Product(
                    id,
                    pmId,
                    rs.getString("type"),
                    name,
                    rs.getString("description"),
                    price,
                    rs.getString("unit")
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
                    // Update
                    System.out.println("Updating product with pm_id: " + product.getPmId() + " to price: " + product.getPrice());
                    String updateSql = "UPDATE products SET type=?, name=?, description=?, price=?, unit=? WHERE pm_id=?";
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                        updateStmt.setString(1, product.getType());
                        updateStmt.setString(2, product.getName());
                        updateStmt.setString(3, product.getDescription());
                        updateStmt.setDouble(4, product.getPrice());
                        updateStmt.setString(5, product.getUnit());
                        updateStmt.setInt(6, product.getPmId());
                        int rows = updateStmt.executeUpdate();
                        System.out.println("Updated " + rows + " rows.");
                    }
                } else {
                    // Insert
                    System.out.println("Inserting product with pm_id: " + product.getPmId() + " with price: " + product.getPrice());
                    String insertSql = "INSERT INTO products (pm_id, type, name, description, price, unit) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                        insertStmt.setInt(1, product.getPmId());
                        insertStmt.setString(2, product.getType());
                        insertStmt.setString(3, product.getName());
                        insertStmt.setString(4, product.getDescription());
                        insertStmt.setDouble(5, product.getPrice());
                        insertStmt.setString(6, product.getUnit());
                        insertStmt.executeUpdate();
                        System.out.println("Inserted product.");
                    }
                }
            }
        }
    }

    public void deleteByPmId(int pmId) throws SQLException {
        System.out.println("Deleting product with pm_id: " + pmId);
        String sql = "DELETE FROM products WHERE pm_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, pmId);
            int rows = stmt.executeUpdate();
            System.out.println("Deleted " + rows + " rows.");
        }
    }
}
