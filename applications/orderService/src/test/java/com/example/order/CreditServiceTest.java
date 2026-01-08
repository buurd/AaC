package com.example.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class CreditServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    private CreditService creditService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        creditService = new CreditService(invoiceRepository);
    }

    @Test
    void testCheckCreditLimit_UnderLimit() throws SQLException {
        Invoice i1 = new Invoice(101, "John Doe", 100.0, LocalDate.now());
        i1.setId(1);
        i1.setPaid(false);
        when(invoiceRepository.findByCustomer("John Doe")).thenReturn(Collections.singletonList(i1));

        assertTrue(creditService.checkCreditLimit("John Doe"));
    }

    @Test
    void testCheckCreditLimit_OverLimit() throws SQLException {
        Invoice i1 = new Invoice(101, "John Doe", 600.0, LocalDate.now());
        i1.setId(1);
        i1.setPaid(false);
        when(invoiceRepository.findByCustomer("John Doe")).thenReturn(Collections.singletonList(i1));

        assertFalse(creditService.checkCreditLimit("John Doe"));
    }

    @Test
    void testCheckCreditLimit_PaidInvoicesIgnored() throws SQLException {
        Invoice i1 = new Invoice(101, "John Doe", 600.0, LocalDate.now());
        i1.setId(1);
        i1.setPaid(true);
        when(invoiceRepository.findByCustomer("John Doe")).thenReturn(Collections.singletonList(i1));

        assertTrue(creditService.checkCreditLimit("John Doe"));
    }

    @Test
    void testCheckOverdueInvoices_NoOverdue() throws SQLException {
        Invoice i1 = new Invoice(101, "John Doe", 100.0, LocalDate.now().plusDays(1));
        i1.setId(1);
        i1.setPaid(false);
        when(invoiceRepository.findByCustomer("John Doe")).thenReturn(Collections.singletonList(i1));

        assertTrue(creditService.checkOverdueInvoices("John Doe"));
    }

    @Test
    void testCheckOverdueInvoices_HasOverdue() throws SQLException {
        Invoice i1 = new Invoice(101, "John Doe", 100.0, LocalDate.now().minusDays(1));
        i1.setId(1);
        i1.setPaid(false);
        when(invoiceRepository.findByCustomer("John Doe")).thenReturn(Collections.singletonList(i1));

        assertFalse(creditService.checkOverdueInvoices("John Doe"));
    }
}
