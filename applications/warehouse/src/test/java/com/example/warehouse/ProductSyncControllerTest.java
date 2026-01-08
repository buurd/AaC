package com.example.warehouse;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductSyncControllerTest {

    private ProductSyncController controller;
    private ProductRepository mockRepository;
    private HttpExchange mockExchange;
    private ByteArrayOutputStream responseStream;

    @BeforeEach
    void setUp() {
        mockRepository = mock(ProductRepository.class);
        controller = new ProductSyncController(mockRepository);
        mockExchange = mock(HttpExchange.class);
        responseStream = new ByteArrayOutputStream();
        
        when(mockExchange.getResponseHeaders()).thenReturn(new Headers());
        when(mockExchange.getResponseBody()).thenReturn(responseStream);
    }

    @Test
    void handle_post_upsertsProduct() throws IOException, SQLException {
        // Arrange
        String json = "{\"id\":101,\"name\":\"Test Product\"}";
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        // Act
        controller.handle(mockExchange);

        // Assert
        verify(mockRepository).upsert(any(Product.class));
        verify(mockExchange).sendResponseHeaders(eq(200), anyLong());
        
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(mockRepository).upsert(productCaptor.capture());
        assertEquals(101, productCaptor.getValue().getPmId());
        assertEquals("Test Product", productCaptor.getValue().getName());
    }

    @Test
    void handle_post_invalidJson_returns500() throws IOException {
        // Arrange
        String json = "invalid-json";
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        // Act
        controller.handle(mockExchange);

        // Assert
        verify(mockExchange).sendResponseHeaders(eq(500), anyLong());
    }

    @Test
    void handle_delete_deletesProduct() throws IOException, SQLException {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("DELETE");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/api/products/sync?id=101"));

        // Act
        controller.handle(mockExchange);

        // Assert
        verify(mockRepository).deleteByPmId(101);
        verify(mockExchange).sendResponseHeaders(eq(200), anyLong());
    }

    @Test
    void handle_delete_missingId_returns500() throws IOException {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("DELETE");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/api/products/sync"));

        // Act
        controller.handle(mockExchange);

        // Assert
        verify(mockExchange).sendResponseHeaders(eq(500), anyLong());
    }

    @Test
    void handle_get_returns405() throws IOException {
        // Arrange
        when(mockExchange.getRequestMethod()).thenReturn("GET");

        // Act
        controller.handle(mockExchange);

        // Assert
        verify(mockExchange).sendResponseHeaders(eq(405), eq(-1L));
    }
}
