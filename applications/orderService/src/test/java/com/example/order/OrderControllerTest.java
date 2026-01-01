package com.example.order;

import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderControllerTest {

    @Test
    void testHandleCreateOrder_ParsesOrderCorrectly() throws IOException, SQLException {
        // Arrange
        OrderRepository mockRepo = mock(OrderRepository.class);
        StockReservationService mockStockService = mock(StockReservationService.class);
        OrderFulfillmentService mockFulfillmentService = mock(OrderFulfillmentService.class);
        CreditService mockCreditService = mock(CreditService.class);
        InvoiceRepository mockInvoiceRepository = mock(InvoiceRepository.class);
        
        OrderController controller = new OrderController(mockRepo, mockStockService, mockFulfillmentService, mockCreditService, mockInvoiceRepository);
        HttpExchange mockExchange = mock(HttpExchange.class);

        String json = "{\"customerName\":\"John Doe\",\"items\":[{\"productId\":1,\"quantity\":2},{\"productId\":5,\"quantity\":1}]}";
        InputStream requestBody = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestURI()).thenReturn(java.net.URI.create("/api/orders"));
        when(mockExchange.getRequestBody()).thenReturn(requestBody);
        when(mockExchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());
        
        when(mockRepo.createOrder(any(Order.class))).thenReturn(101);
        when(mockStockService.reserveStock(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(true));

        // Act
        controller.handle(mockExchange);

        // Assert
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(mockRepo).createOrder(orderCaptor.capture());

        Order capturedOrder = orderCaptor.getValue();
        assertEquals("John Doe", capturedOrder.getCustomerName());
        assertEquals(2, capturedOrder.getItems().size());
        
        assertEquals(1, capturedOrder.getItems().get(0).getProductId());
        assertEquals(2, capturedOrder.getItems().get(0).getQuantity());
        
        assertEquals(5, capturedOrder.getItems().get(1).getProductId());
        assertEquals(1, capturedOrder.getItems().get(1).getQuantity());
    }
}
