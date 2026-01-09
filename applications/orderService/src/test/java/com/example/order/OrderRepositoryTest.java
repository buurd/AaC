package com.example.order;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderRepositoryTest {

    private Connection connection;
    private OrderRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        repository = new OrderRepository(connection);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE orders (id INT AUTO_INCREMENT PRIMARY KEY, customer_name VARCHAR(255), status VARCHAR(255), total_amount DOUBLE, points_redeemed INT, points_earned INT)");
            stmt.execute("CREATE TABLE order_items (id INT AUTO_INCREMENT PRIMARY KEY, order_id INT, product_id INT, quantity INT)");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE orders");
            stmt.execute("DROP TABLE order_items");
        }
        connection.close();
    }

    @Test
    void testCreateAndFindOrder() throws SQLException {
        Order order = new Order();
        order.setCustomerName("John Doe");
        order.setStatus("PENDING");
        order.setTotalAmount(100.0);
        order.setPointsToRedeem(10);
        order.addItem(new OrderItem(1, 2));

        int id = repository.createOrder(order);
        assertTrue(id > 0);

        List<Order> orders = repository.findAll();
        assertEquals(1, orders.size());
        Order saved = orders.get(0);
        assertEquals("John Doe", saved.getCustomerName());
        assertEquals("PENDING", saved.getStatus());
        assertEquals(1, saved.getItems().size());
        assertEquals(1, saved.getItems().get(0).getProductId());
    }

    @Test
    void testUpdateStatus() throws SQLException {
        Order order = new Order();
        order.setCustomerName("John Doe");
        order.setStatus("PENDING");
        int id = repository.createOrder(order);

        repository.updateStatus(id, "CONFIRMED");

        List<Order> orders = repository.findAll();
        assertEquals("CONFIRMED", orders.get(0).getStatus());
    }

    @Test
    void testUpdatePointsEarned() throws SQLException {
        Order order = new Order();
        order.setCustomerName("John Doe");
        order.setStatus("PENDING");
        int id = repository.createOrder(order);

        repository.updatePointsEarned(id, 50);

        List<Order> orders = repository.findAll();
        assertEquals(50, orders.get(0).getPointsEarned());
    }
}
