package com.example.warehouse;

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
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FulfillmentControllerTest {

    @Mock
    private FulfillmentOrderRepository repository;

    @Mock
    private OrderUpdateService orderUpdateService;

    @Mock
    private HttpExchange exchange;

    private FulfillmentController controller;
    private Headers responseHeaders;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new FulfillmentController(repository, orderUpdateService);
        responseHeaders = new Headers();
        responseBody = new ByteArrayOutputStream();

        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        when(exchange.getResponseBody()).thenReturn(responseBody);
    }

    @Test
    void testListOrders() throws IOException, SQLException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        
        FulfillmentOrder fo = new FulfillmentOrder(1, 101, "PENDING");
        when(repository.findAllFulfillmentOrders()).thenReturn(Collections.singletonList(fo));

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("101"));
        assertTrue(response.contains("PENDING"));
        assertTrue(response.contains("Mark Shipped"));
    }

    @Test
    void testUpdateOrder() throws IOException, SQLException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        String body = "orderId=101&status=SHIPPED";
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(body.getBytes()));

        controller.handle(exchange);

        verify(repository).updateFulfillmentStatus(101, "SHIPPED");
        verify(orderUpdateService).updateStatus(101, "SHIPPED");
        verify(exchange).sendResponseHeaders(eq(302), eq(-1L));
        assertEquals("/fulfillment", responseHeaders.getFirst("Location"));
    }

    @Test
    void testMethodNotAllowed() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("PUT");

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(405), eq(-1L));
    }
    
    @Test
    void testListError() throws IOException, SQLException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(repository.findAllFulfillmentOrders()).thenThrow(new SQLException("DB Error"));

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(500), eq(-1L));
    }
    
    @Test
    void testUpdateError() throws IOException, SQLException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        String body = "orderId=101&status=SHIPPED";
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(body.getBytes()));
        
        doThrow(new SQLException("DB Error")).when(repository).updateFulfillmentStatus(101, "SHIPPED");

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(500), eq(-1L));
    }
}
