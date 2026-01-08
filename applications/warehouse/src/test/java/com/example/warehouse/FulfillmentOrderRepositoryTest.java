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

class FulfillmentOrderRepositoryTest {

    private static final String DB_URL = "jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private FulfillmentOrderRepository repository;
    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        repository = new FulfillmentOrderRepository(connection);
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS fulfillment_orders (" +
                         "id SERIAL PRIMARY KEY, " +
                         "order_id INT UNIQUE, " +
                         "status VARCHAR(50)" +
                         ")");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS fulfillment_orders");
        }
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void testCreateAndFind() throws SQLException {
        repository.createFulfillmentOrder(101);
        
        List<FulfillmentOrder> list = repository.findAllFulfillmentOrders();
        assertEquals(1, list.size());
        assertEquals(101, list.get(0).getOrderId());
        assertEquals("PENDING", list.get(0).getStatus());
        
        // Create duplicate (should be ignored)
        repository.createFulfillmentOrder(101);
        list = repository.findAllFulfillmentOrders();
        assertEquals(1, list.size());
    }

    @Test
    void testUpdateStatus() throws SQLException {
        repository.createFulfillmentOrder(202);
        
        repository.updateFulfillmentStatus(202, "SHIPPED");
        
        List<FulfillmentOrder> list = repository.findAllFulfillmentOrders();
        assertEquals(1, list.size());
        assertEquals("SHIPPED", list.get(0).getStatus());
    }
}
