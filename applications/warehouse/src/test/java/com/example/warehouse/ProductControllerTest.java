package com.example.warehouse;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductControllerTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private HttpExchange exchange;

    private ProductController controller;
    private Headers responseHeaders;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new ProductController(repository);
        responseHeaders = new Headers();
        responseBody = new ByteArrayOutputStream();

        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        when(exchange.getResponseBody()).thenReturn(responseBody);
    }

    @Test
    void testHandleSuccess() throws IOException, SQLException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/"));
        
        Product p1 = new Product(1, 101, "Product A");
        Product p2 = new Product(2, 102, "Product B");
        when(repository.findAll()).thenReturn(Arrays.asList(p1, p2));

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("Product A"));
        assertTrue(response.contains("Product B"));
        assertTrue(response.contains("101"));
        assertTrue(response.contains("102"));
    }

    @Test
    void testHandleEmpty() throws IOException, SQLException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/"));
        when(repository.findAll()).thenReturn(Collections.emptyList());

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        assertTrue(responseBody.toString().contains("Warehouse Products"));
    }

    @Test
    void testHandleError() throws IOException, SQLException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/"));
        when(repository.findAll()).thenThrow(new SQLException("DB Error"));

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(500), anyLong());
        assertTrue(responseBody.toString().contains("Internal Server Error"));
    }
}
