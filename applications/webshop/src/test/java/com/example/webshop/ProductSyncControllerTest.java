package com.example.webshop;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ProductSyncControllerTest {

    @Test
    void testHandlePost_ParsesProductCorrectly() throws IOException, SQLException {
        // Arrange
        ProductRepository mockRepo = mock(ProductRepository.class);
        ProductSyncController controller = new ProductSyncController(mockRepo);
        HttpExchange mockExchange = mock(HttpExchange.class);

        String json = "{\"id\":1,\"type\":\"T-Shirt\",\"name\":\"Classic T-Shirt - Red S\",\"description\":\"100% Cotton\",\"price\":20.00,\"unit\":\"pcs\"}";
        InputStream requestBody = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestBody()).thenReturn(requestBody);
        when(mockExchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());
        when(mockExchange.getResponseHeaders()).thenReturn(new Headers());

        // Act
        controller.handle(mockExchange);

        // Assert
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(mockRepo).upsert(productCaptor.capture());

        Product capturedProduct = productCaptor.getValue();
        assertEquals(1, capturedProduct.getPmId());
        assertEquals("T-Shirt", capturedProduct.getType());
        assertEquals("Classic T-Shirt - Red S", capturedProduct.getName());
        assertEquals("100% Cotton", capturedProduct.getDescription());
        assertEquals(20.00, capturedProduct.getPrice());
        assertEquals("pcs", capturedProduct.getUnit());
        
        verify(mockExchange).sendResponseHeaders(eq(200), anyLong());
    }

    @Test
    void testHandleDelete_DeletesProductCorrectly() throws IOException, SQLException {
        // Arrange
        ProductRepository mockRepo = mock(ProductRepository.class);
        ProductSyncController controller = new ProductSyncController(mockRepo);
        HttpExchange mockExchange = mock(HttpExchange.class);

        when(mockExchange.getRequestMethod()).thenReturn("DELETE");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/api/products/sync?id=101"));
        when(mockExchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());
        when(mockExchange.getResponseHeaders()).thenReturn(new Headers());

        // Act
        controller.handle(mockExchange);

        // Assert
        verify(mockRepo).deleteByPmId(101);
        verify(mockExchange).sendResponseHeaders(eq(200), anyLong());
    }
}
