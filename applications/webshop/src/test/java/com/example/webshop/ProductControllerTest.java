package com.example.webshop;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProductControllerTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpExchange exchange;

    private ProductController controller;
    private Headers responseHeaders;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new ProductController(repository, httpClient);
        responseHeaders = new Headers();
        responseBody = new ByteArrayOutputStream();

        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        when(exchange.getResponseBody()).thenReturn(responseBody);
        when(exchange.getRequestURI()).thenReturn(URI.create("/products"));
        when(exchange.getRequestMethod()).thenReturn("GET");
    }

    @Test
    void testConstructor() {
        assertDoesNotThrow(() -> new ProductController(repository));
    }

    @Test
    void testHandle_ReturnsProducts() throws IOException, SQLException, InterruptedException {
        // Arrange
        Product p1 = new Product();
        p1.setId(1);
        p1.setName("T-Shirt - Red S");
        p1.setPrice(20.0);
        p1.setUnit("pcs");
        p1.setStock(10);
        p1.setDescription("Cool T-Shirt");

        Product p2 = new Product();
        p2.setId(2);
        p2.setName("T-Shirt - Blue M");
        p2.setPrice(22.0);
        p2.setUnit("pcs");
        p2.setStock(5);
        p2.setDescription("Cool T-Shirt");

        when(repository.findAll()).thenReturn(Arrays.asList(p1, p2));

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("[\"Summer Sale\"]");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        // Act
        controller.handle(exchange);

        // Assert
        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("T-Shirt"));
        assertTrue(response.contains("Red"));
        assertTrue(response.contains("Blue"));
        assertTrue(response.contains("Summer Sale"));
    }

    @Test
    void testHandle_ProductWithoutAttributes() throws IOException, SQLException, InterruptedException {
        // Arrange
        Product p1 = new Product();
        p1.setId(1);
        p1.setName("Simple Product");
        p1.setPrice(10.0);
        p1.setUnit("pcs");
        p1.setStock(100);
        p1.setDescription("Just a product");

        when(repository.findAll()).thenReturn(Collections.singletonList(p1));

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("[]");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        // Act
        controller.handle(exchange);

        // Assert
        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("Simple Product"));
    }

    @Test
    void testHandle_RepositoryError() throws IOException, SQLException {
        // Arrange
        when(repository.findAll()).thenThrow(new SQLException("DB Error"));

        // Act
        controller.handle(exchange);

        // Assert
        verify(exchange).sendResponseHeaders(eq(500), anyLong());
    }
    
    @Test
    void testHandle_CampaignServiceFailure() throws IOException, SQLException, InterruptedException {
        // Arrange
        Product p1 = new Product();
        p1.setId(1);
        p1.setName("T-Shirt - Red S");
        p1.setPrice(20.0);
        p1.setUnit("pcs");
        p1.setStock(10);
        p1.setDescription("Cool T-Shirt");

        when(repository.findAll()).thenReturn(Collections.singletonList(p1));

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new IOException("Network Error"));

        // Act
        controller.handle(exchange);

        // Assert
        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("T-Shirt"));
        // Should still render products even if campaigns fail
    }

    @Test
    void testHandle_CampaignServiceInterrupted() throws IOException, SQLException, InterruptedException {
        // Arrange
        Product p1 = new Product();
        p1.setId(1);
        p1.setName("T-Shirt");
        p1.setPrice(20.0);
        p1.setUnit("pcs");
        p1.setStock(10);
        p1.setDescription("Cool T-Shirt");

        when(repository.findAll()).thenReturn(Collections.singletonList(p1));

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new InterruptedException("Interrupted"));

        // Act
        controller.handle(exchange);

        // Assert
        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        assertTrue(Thread.interrupted()); // Check if interrupt flag was restored
    }
}
