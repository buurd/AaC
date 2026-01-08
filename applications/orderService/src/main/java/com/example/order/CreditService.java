package com.example.order;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class CreditService {
    private final InvoiceRepository invoiceRepository;
    private static final double CREDIT_LIMIT = 500.00;

    public CreditService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public boolean checkCreditLimit(String customerName) {
        try {
            List<Invoice> invoices = invoiceRepository.findByCustomer(customerName);
            double outstandingAmount = invoices.stream()
                    .filter(i -> !i.isPaid())
                    .mapToDouble(Invoice::getAmount)
                    .sum();
            return outstandingAmount <= CREDIT_LIMIT;
        } catch (SQLException e) {
            e.printStackTrace();
            return false; // Fail safe
        }
    }

    public boolean checkOverdueInvoices(String customerName) {
        try {
            List<Invoice> invoices = invoiceRepository.findByCustomer(customerName);
            LocalDate now = LocalDate.now();
            return invoices.stream()
                    .filter(i -> !i.isPaid())
                    .noneMatch(i -> i.getDueDate().isBefore(now));
        } catch (SQLException e) {
            e.printStackTrace();
            return false; // Fail safe
        }
    }
}
