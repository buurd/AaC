package com.example.warehouse;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderFulfillmentControllerTest {

    private OrderFulfillmentController controller;
    private FulfillmentOrderRepository mockRepository;
    private HttpExchange mockExchange;
    private ByteArrayOutputStream responseStream;

    @BeforeEach
    void setUp() {
        mockRepository = mock(FulfillmentOrderRepository.class);
        controller = new OrderFulfillmentController(mockRepository);
        mockExchange = mock(HttpExchange.class);
        responseStream = new ByteArrayOutputStream();
        
        when(mockExchange.getResponseHeaders()).thenReturn(new Headers());
        when(mockExchange.getResponseBody()).thenReturn(responseStream);
    }

    @Test
    void handle_post_createsFulfillmentOrder() throws IOException, SQLException {
        // Arrange
        String json = "{\"orderId\":123}";
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        // Act
        controller.handle(mockExchange);

        // Assert
        verify(mockRepository).createFulfillmentOrder(123);
        verify(mockExchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseStream.toString(StandardCharsets.UTF_8);
        assertTrue(response.contains("fulfillment_started"));
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
        verify(mockExchange).sendResponseHeaders(eq(500), eq(-1L));
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
