package com.example.productmanagement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductRepository {

    private final Connection connection;

    public ProductRepository(Connection connection) {
        this.connection = connection;
    }

    // --- Product Group Methods ---

    public List<ProductGroup> findAllGroups() throws SQLException {
        List<ProductGroup> groups = new ArrayList<>();
        String sql = "SELECT id, name, description, base_price, base_unit FROM product_groups ORDER BY id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                groups.add(mapGroupRow(rs));
            }
        }
        return groups;
    }

    public ProductGroup findGroupById(int id) throws SQLException {
        String sql = "SELECT id, name, description, base_price, base_unit FROM product_groups WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapGroupRow(rs);
                }
            }
        }
        return null;
    }

    public int createGroup(ProductGroup group) throws SQLException {
        String sql = "INSERT INTO product_groups (name, description, base_price, base_unit) VALUES (?, ?, ?, ?) RETURNING id";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, group.getName());
            stmt.setString(2, group.getDescription());
            stmt.setDouble(3, group.getBasePrice());
            stmt.setString(4, group.getBaseUnit());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Creating product group failed, no ID obtained.");
    }

    // --- Product Methods ---

    public List<Product> findAll() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT id, group_id, type, name, description, price, unit, attributes FROM products ORDER BY id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(mapRow(rs));
            }
        }
        return products;
    }

    public List<Product> findByGroupId(int groupId) throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT id, group_id, type, name, description, price, unit, attributes FROM products WHERE group_id = ? ORDER BY id";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapRow(rs));
                }
            }
        }
        return products;
    }

    public Product findById(int id) throws SQLException {
        String sql = "SELECT id, group_id, type, name, description, price, unit, attributes FROM products WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public int create(Product product) throws SQLException {
        String sql = "INSERT INTO products (group_id, type, name, description, price, unit, attributes) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (product.getGroupId() != null) {
                stmt.setInt(1, product.getGroupId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setString(2, product.getType());
            stmt.setString(3, product.getName());
            stmt.setString(4, product.getDescription());
            stmt.setDouble(5, product.getPrice());
            stmt.setString(6, product.getUnit());
            stmt.setString(7, product.getAttributes());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Creating product failed, no ID obtained.");
    }

    public void update(Product product) throws SQLException {
        String sql = "UPDATE products SET group_id = ?, type = ?, name = ?, description = ?, price = ?, unit = ?, attributes = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (product.getGroupId() != null) {
                stmt.setInt(1, product.getGroupId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setString(2, product.getType());
            stmt.setString(3, product.getName());
            stmt.setString(4, product.getDescription());
            stmt.setDouble(5, product.getPrice());
            stmt.setString(6, product.getUnit());
            stmt.setString(7, product.getAttributes());
            stmt.setInt(8, product.getId());
            stmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM products WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        return new Product(
            rs.getInt("id"),
            (Integer) rs.getObject("group_id"),
            rs.getString("type"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getDouble("price"),
            rs.getString("unit"),
            rs.getString("attributes")
        );
    }

    private ProductGroup mapGroupRow(ResultSet rs) throws SQLException {
        return new ProductGroup(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getDouble("base_price"),
            rs.getString("base_unit")
        );
    }
}
