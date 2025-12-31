package com.example.order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class InvoiceRepository {
    private final Connection connection;

    public InvoiceRepository(Connection connection) {
        this.connection = connection;
    }

    public void createInvoice(Invoice invoice) throws SQLException {
        String sql = "INSERT INTO invoices (order_id, customer_name, amount, due_date, paid) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, invoice.getOrderId());
            stmt.setString(2, invoice.getCustomerName());
            stmt.setDouble(3, invoice.getAmount());
            stmt.setDate(4, Date.valueOf(invoice.getDueDate()));
            stmt.setBoolean(5, invoice.isPaid());
            stmt.executeUpdate();
        }
    }

    public List<Invoice> findByCustomer(String customerName) throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT * FROM invoices WHERE customer_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, customerName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    invoices.add(mapRow(rs));
                }
            }
        }
        return invoices;
    }

    public Invoice findById(int id) throws SQLException {
        String sql = "SELECT * FROM invoices WHERE id = ?";
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

    public void markPaid(int id) throws SQLException {
        String sql = "UPDATE invoices SET paid = true WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }
    
    public List<Invoice> findAll() throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT * FROM invoices";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    invoices.add(mapRow(rs));
                }
            }
        }
        return invoices;
    }

    private Invoice mapRow(ResultSet rs) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setId(rs.getInt("id"));
        invoice.setOrderId(rs.getInt("order_id"));
        invoice.setCustomerName(rs.getString("customer_name"));
        invoice.setAmount(rs.getDouble("amount"));
        invoice.setDueDate(rs.getDate("due_date").toLocalDate());
        invoice.setPaid(rs.getBoolean("paid"));
        return invoice;
    }
}
