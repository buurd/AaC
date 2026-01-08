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

class DeliveryRepositoryTest {

    private static final String DB_URL = "jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private DeliveryRepository repository;
    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        repository = new DeliveryRepository(connection);
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS deliveries (" +
                         "id SERIAL PRIMARY KEY, " +
                         "sender VARCHAR(255)" +
                         ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS product_individuals (" +
                         "id SERIAL PRIMARY KEY, " +
                         "delivery_id INT, " +
                         "product_id INT, " +
                         "serial_number VARCHAR(255), " +
                         "state VARCHAR(50)" +
                         ")");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS product_individuals");
            stmt.execute("DROP TABLE IF EXISTS deliveries");
        }
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void testCreateAndFindDelivery() throws SQLException {
        // H2 compatibility mode for PostgreSQL might not support RETURNING id in the same way for INSERT
        // Or the driver version might be an issue.
        // Let's assume createDelivery uses RETURNING id.
        // If it fails, we might need to adjust the test or the repository code if possible, 
        // but since we can't change repository code easily without breaking prod potentially (if it works on Postgres),
        // we should try to make H2 work.
        // The error was: Syntax error in SQL statement "INSERT INTO deliveries (sender) VALUES (?) [*]RETURNING id"
        // H2 supports RETURNING since version 2.x, but maybe MODE=PostgreSQL is tricky.
        // Let's try to mock the behavior or just skip if it's strictly DB dependent.
        // However, we want coverage.
        
        // Actually, the error suggests H2 doesn't like RETURNING id in that position or mode.
        // But we are using H2 2.2.224 which should support it.
        
        // Let's try to use a different approach for testing if possible, or just accept that 
        // unit testing JDBC code with H2 has limitations if the SQL is dialect specific.
        
        // For now, I will comment out the tests that fail due to SQL syntax if I can't fix H2 config.
        // But wait, I can try to fix the H2 url to be more compatible?
        // Or maybe the issue is that `RETURNING id` is not standard SQL and H2 support varies.
        
        // Let's try to run it. If it fails, I will ignore the failure for now to proceed with coverage report.
        // But the user wants me to fix errors.
        
        // The error log showed: org.h2.jdbc.JdbcSQLSyntaxErrorException: Syntax error in SQL statement "INSERT INTO deliveries (sender) VALUES (?) [*]RETURNING id"
        
        // This means H2 is not parsing RETURNING correctly.
        // I will try to modify the test to use a different DB URL or just skip these tests if they are integration tests.
        // But these are unit tests.
        
        // I will try to use `MODE=PostgreSQL` which I already did.
        // Maybe I should just catch the exception and ignore it for the sake of this environment?
        // No, that's bad practice.
        
        // Let's try to see if I can modify `DeliveryRepository` to use `getGeneratedKeys` instead of `RETURNING id`?
        // That would be more portable.
        // But I should check `DeliveryRepository.java` first.

        int id = repository.createDelivery("Sender X");
        assertTrue(id > 0);
        
        Delivery d = repository.findById(id);
        assertNotNull(d);
        assertEquals("Sender X", d.getSender());
    }

    @Test
    void testAddIndividualAndCountStock() throws SQLException {
        int deliveryId = repository.createDelivery("Sender Y");
        
        ProductIndividual pi = new ProductIndividual();
        pi.setDeliveryId(deliveryId);
        pi.setProductId(100);
        pi.setSerialNumber("SN1");
        pi.setState("New");
        
        repository.addIndividual(pi);
        
        int count = repository.countStock(100);
        assertEquals(1, count);
        
        // Add another one
        pi.setSerialNumber("SN2");
        repository.addIndividual(pi);
        
        count = repository.countStock(100);
        assertEquals(2, count);
    }

    @Test
    void testReserveStock() throws SQLException {
        int deliveryId = repository.createDelivery("Sender Z");
        
        // Add 3 items
        for (int i = 0; i < 3; i++) {
            ProductIndividual pi = new ProductIndividual();
            pi.setDeliveryId(deliveryId);
            pi.setProductId(200);
            pi.setSerialNumber("SN" + i);
            pi.setState("New");
            repository.addIndividual(pi);
        }
        
        // Reserve 2
        boolean success = repository.reserveStock(200, 2);
        assertTrue(success);
        
        // Check remaining stock (should be 1 'New')
        int count = repository.countStock(200);
        assertEquals(1, count);
        
        // Try to reserve 2 more (only 1 available)
        success = repository.reserveStock(200, 2);
        assertFalse(success);
    }

    @Test
    void testFindAll() throws SQLException {
        repository.createDelivery("A");
        repository.createDelivery("B");
        
        List<Delivery> list = repository.findAll();
        assertEquals(2, list.size());
    }

    @Test
    void testDeleteDelivery() throws SQLException {
        int id = repository.createDelivery("To Delete");
        repository.deleteDelivery(id);
        
        Delivery d = repository.findById(id);
        assertNull(d);
    }
}
