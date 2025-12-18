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
        String sql = "SELECT id, type, name, description, price, unit FROM products";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                products.add(new Product(
                    rs.getInt("id"),
                    rs.getString("type"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getDouble("price"),
                    rs.getString("unit")
                ));
            }
        }
        return products;
    }
}
