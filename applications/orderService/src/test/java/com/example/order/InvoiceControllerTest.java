package com.example.order;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class InvoiceControllerTest {

    @Mock
    private InvoiceRepository repository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private HttpExchange exchange;

    private InvoiceController controller;
    private Headers responseHeaders;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new InvoiceController(repository, orderRepository);
        responseHeaders = new Headers();
        responseBody = new ByteArrayOutputStream();

        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        when(exchange.getResponseBody()).thenReturn(responseBody);
    }

    @Test
    void testHandle_ListInvoices() throws IOException, SQLException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/invoices"));

        Invoice i1 = new Invoice(101, "John Doe", 100.0, LocalDate.now());
        i1.setId(1);
        i1.setPaid(false);
        Invoice i2 = new Invoice(102, "Jane Doe", 200.0, LocalDate.now());
        i2.setId(2);
        i2.setPaid(true);

        when(repository.findAll()).thenReturn(Arrays.asList(i1, i2));

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("Invoices"));
        assertTrue(response.contains("John Doe"));
        assertTrue(response.contains("Jane Doe"));
        assertTrue(response.contains("Mark Paid"));
    }

    @Test
    void testHandle_MarkPaid() throws IOException, SQLException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(URI.create("/invoices"));
        String formData = "id=1";
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(formData.getBytes()));

        Invoice invoice = new Invoice(101, "John Doe", 100.0, LocalDate.now());
        invoice.setId(1);
        when(repository.findById(1)).thenReturn(invoice);

        controller.handle(exchange);

        verify(repository).markPaid(1);
        verify(orderRepository).updateStatus(101, "PAID");
        verify(exchange).sendResponseHeaders(eq(302), anyLong());
        assertTrue(responseHeaders.getFirst("Location").endsWith("/invoices"));
    }
    
    @Test
    void testHandle_MarkPaid_NoOrderRepo() throws IOException, SQLException {
        controller = new InvoiceController(repository); // Use constructor without orderRepository
        
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(URI.create("/invoices"));
        String formData = "id=1";
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(formData.getBytes()));

        controller.handle(exchange);

        verify(repository).markPaid(1);
        verifyNoInteractions(orderRepository);
        verify(exchange).sendResponseHeaders(eq(302), anyLong());
    }
}
