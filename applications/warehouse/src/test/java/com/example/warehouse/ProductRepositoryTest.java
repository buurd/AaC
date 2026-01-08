package com.example.warehouse;

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

    private static final String DB_URL = "jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private ProductRepository repository;
    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        repository = new ProductRepository(connection);
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                         "id SERIAL PRIMARY KEY, " +
                         "pm_id INT UNIQUE, " +
                         "name VARCHAR(255)" +
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
    void testUpsertAndFind() throws SQLException {
        Product p1 = new Product();
        p1.setPmId(101);
        p1.setName("Product A");
        
        repository.upsert(p1);
        
        Product found = repository.findByPmId(101);
        assertNotNull(found);
        assertEquals("Product A", found.getName());
        
        // Update
        p1.setName("Product A Updated");
        repository.upsert(p1);
        
        found = repository.findByPmId(101);
        assertEquals("Product A Updated", found.getName());
    }

    @Test
    void testFindAll() throws SQLException {
        Product p1 = new Product(0, 101, "A");
        Product p2 = new Product(0, 102, "B");
        
        repository.upsert(p1);
        repository.upsert(p2);
        
        List<Product> all = repository.findAll();
        assertEquals(2, all.size());
    }

    @Test
    void testFindById() throws SQLException {
        Product p1 = new Product(0, 101, "A");
        repository.upsert(p1);
        
        Product foundByPmId = repository.findByPmId(101);
        assertNotNull(foundByPmId);
        
        Product foundById = repository.findById(foundByPmId.getId());
        assertNotNull(foundById);
        assertEquals(foundByPmId.getName(), foundById.getName());
    }
    
    @Test
    void testDelete() throws SQLException {
        Product p1 = new Product(0, 101, "A");
        repository.upsert(p1);
        
        assertNotNull(repository.findByPmId(101));
        
        repository.deleteByPmId(101);
        
        assertNull(repository.findByPmId(101));
    }
}
