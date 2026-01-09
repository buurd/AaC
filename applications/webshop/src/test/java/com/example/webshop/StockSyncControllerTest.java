package com.example.webshop;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class StockSyncControllerTest {

    @Test
    void testHandlePost_UpdatesStockCorrectly() throws IOException, SQLException {
        // Arrange
        ProductRepository mockRepo = mock(ProductRepository.class);
        StockSyncController controller = new StockSyncController(mockRepo);
        HttpExchange mockExchange = mock(HttpExchange.class);

        String json = "{\"pmId\": 1, \"stock\": 100}";
        InputStream requestBody = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(requestBody);
        when(mockExchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());
        when(mockExchange.getResponseHeaders()).thenReturn(new Headers());

        // Act
        controller.handle(mockExchange);

        // Assert
        verify(mockRepo).updateStock(1, 100);
        verify(mockExchange).sendResponseHeaders(eq(200), anyLong());
    }

    @Test
    void testHandle_MethodNotAllowed() throws IOException {
        // Arrange
        ProductRepository mockRepo = mock(ProductRepository.class);
        StockSyncController controller = new StockSyncController(mockRepo);
        HttpExchange mockExchange = mock(HttpExchange.class);

        when(mockExchange.getRequestMethod()).thenReturn("GET");

        // Act
        controller.handle(mockExchange);

        // Assert
        verify(mockExchange).sendResponseHeaders(eq(405), anyLong());
    }

    @Test
    void testHandlePost_Exception_Returns500() throws IOException {
        // Arrange
        ProductRepository mockRepo = mock(ProductRepository.class);
        StockSyncController controller = new StockSyncController(mockRepo);
        HttpExchange mockExchange = mock(HttpExchange.class);

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenThrow(new RuntimeException("Stream error"));
        when(mockExchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());
        when(mockExchange.getResponseHeaders()).thenReturn(new Headers());

        // Act
        controller.handle(mockExchange);

        // Assert
        verify(mockExchange).sendResponseHeaders(eq(500), anyLong());
    }
}
