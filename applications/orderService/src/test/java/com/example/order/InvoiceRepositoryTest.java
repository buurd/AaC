package com.example.order;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceRepositoryTest {

    private Connection connection;
    private InvoiceRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        repository = new InvoiceRepository(connection);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE invoices (id INT AUTO_INCREMENT PRIMARY KEY, order_id INT, customer_name VARCHAR(255), amount DOUBLE, due_date DATE, paid BOOLEAN)");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE invoices");
        }
        connection.close();
    }

    @Test
    void testCreateAndFindInvoice() throws SQLException {
        Invoice invoice = new Invoice(101, "John Doe", 150.0, LocalDate.now().plusDays(30));
        repository.createInvoice(invoice);

        List<Invoice> invoices = repository.findAll();
        assertEquals(1, invoices.size());
        assertEquals("John Doe", invoices.get(0).getCustomerName());
        assertEquals(101, invoices.get(0).getOrderId());
    }

    @Test
    void testFindByCustomer() throws SQLException {
        repository.createInvoice(new Invoice(101, "John Doe", 150.0, LocalDate.now()));
        repository.createInvoice(new Invoice(102, "Jane Doe", 200.0, LocalDate.now()));

        List<Invoice> johnsInvoices = repository.findByCustomer("John Doe");
        assertEquals(1, johnsInvoices.size());
        assertEquals(101, johnsInvoices.get(0).getOrderId());
    }

    @Test
    void testMarkPaid() throws SQLException {
        repository.createInvoice(new Invoice(101, "John Doe", 150.0, LocalDate.now()));
        List<Invoice> invoices = repository.findAll();
        int id = invoices.get(0).getId();

        repository.markPaid(id);

        Invoice updated = repository.findById(id);
        assertTrue(updated.isPaid());
    }
}
