package com.example.webshop;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductRepositoryTest {

    private static final String DB_URL = "jdbc:h2:mem:webshopdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private ProductRepository repository;
    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("H2 Driver not found", e);
        }
        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        repository = new ProductRepository(connection);
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                         "id SERIAL PRIMARY KEY, " +
                         "pm_id INT UNIQUE, " +
                         "type VARCHAR(255), " +
                         "name VARCHAR(255), " +
                         "description TEXT, " +
                         "price DECIMAL(10,2), " +
                         "unit VARCHAR(50), " +
                         "stock INT DEFAULT 0" +
                         ")");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS products");
        }
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void testUpsertAndFindAll() throws SQLException {
        Product p1 = new Product();
        p1.setPmId(101);
        p1.setName("Product 1");
        p1.setType("Type A");
        p1.setDescription("Desc 1");
        p1.setPrice(10.0);
        p1.setUnit("pcs");

        repository.upsert(p1);

        List<Product> products = repository.findAll();
        assertEquals(1, products.size());
        assertEquals("Product 1", products.get(0).getName());
        assertEquals(0, products.get(0).getStock()); // Default stock

        // Update
        p1.setName("Product 1 Updated");
        p1.setPrice(15.0);
        repository.upsert(p1);

        products = repository.findAll();
        assertEquals(1, products.size());
        assertEquals("Product 1 Updated", products.get(0).getName());
        assertEquals(15.0, products.get(0).getPrice());
    }

    @Test
    void testUpdateStock() throws SQLException {
        Product p1 = new Product();
        p1.setPmId(102);
        p1.setName("Product 2");
        repository.upsert(p1);

        repository.updateStock(102, 50);

        List<Product> products = repository.findAll();
        assertEquals(1, products.size());
        assertEquals(50, products.get(0).getStock());
    }

    @Test
    void testDeleteByPmId() throws SQLException {
        Product p1 = new Product();
        p1.setPmId(103);
        p1.setName("Product 3");
        repository.upsert(p1);

        repository.deleteByPmId(103);

        List<Product> products = repository.findAll();
        assertTrue(products.isEmpty());
    }
}
