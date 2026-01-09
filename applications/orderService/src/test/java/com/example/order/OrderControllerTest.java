package com.example.order;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrderControllerTest {

    @Mock
    private OrderRepository mockRepo;
    @Mock
    private StockReservationService mockStockService;
    @Mock
    private OrderFulfillmentService mockFulfillmentService;
    @Mock
    private CreditService mockCreditService;
    @Mock
    private InvoiceRepository mockInvoiceRepository;
    @Mock
    private LoyaltyIntegrationService mockLoyaltyService;
    @Mock
    private HttpExchange mockExchange;

    private OrderController controller;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new OrderController(mockRepo, mockStockService, mockFulfillmentService, mockCreditService, mockInvoiceRepository, mockLoyaltyService);
        responseBody = new ByteArrayOutputStream();
        when(mockExchange.getResponseBody()).thenReturn(responseBody);
        when(mockExchange.getResponseHeaders()).thenReturn(new Headers());
    }

    @Test
    void testHandleCreateOrder_ParsesOrderCorrectly() throws IOException, SQLException {
        String json = "{\"customerName\":\"John Doe\",\"items\":[{\"productId\":1,\"quantity\":2},{\"productId\":5,\"quantity\":1}]}";
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/api/orders"));
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        
        when(mockRepo.createOrder(any(Order.class))).thenReturn(101);
        when(mockStockService.reserveStock(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(true));
        when(mockLoyaltyService.redeemPoints(anyString(), anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(true));

        controller.handle(mockExchange);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(mockRepo).createOrder(orderCaptor.capture());

        Order capturedOrder = orderCaptor.getValue();
        assertEquals("John Doe", capturedOrder.getCustomerName());
        assertEquals(2, capturedOrder.getItems().size());
        
        verify(mockExchange).sendResponseHeaders(eq(200), anyLong());
    }

    @Test
    void testHandleCreateOrder_RedemptionFailed() throws IOException, SQLException {
        String json = "{\"customerName\":\"John Doe\",\"pointsToRedeem\":100,\"items\":[{\"productId\":1,\"quantity\":1}]}";
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/api/orders"));
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        
        when(mockRepo.createOrder(any(Order.class))).thenReturn(101);
        when(mockLoyaltyService.redeemPoints(anyString(), anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(false));

        controller.handle(mockExchange);

        verify(mockRepo).updateStatus(101, "REDEMPTION_FAILED");
        verify(mockExchange).sendResponseHeaders(eq(409), anyLong());
    }

    @Test
    void testHandleCreateOrder_StockReservationFailed() throws IOException, SQLException {
        String json = "{\"customerName\":\"John Doe\",\"items\":[{\"productId\":1,\"quantity\":1}]}";
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/api/orders"));
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        
        when(mockRepo.createOrder(any(Order.class))).thenReturn(101);
        when(mockLoyaltyService.redeemPoints(anyString(), anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(true));
        when(mockStockService.reserveStock(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(false));

        controller.handle(mockExchange);

        verify(mockRepo).updateStatus(101, "REJECTED");
        verify(mockExchange).sendResponseHeaders(eq(409), anyLong());
    }

    @Test
    void testHandleListOrders_ReturnsHtml() throws IOException, SQLException {
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/orders"));

        Order o1 = new Order(1, "John Doe", "PENDING");
        when(mockRepo.findAll()).thenReturn(Collections.singletonList(o1));

        controller.handle(mockExchange);

        verify(mockExchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("Order Management"));
        assertTrue(response.contains("John Doe"));
    }

    @Test
    void testHandleApiListOrders_ReturnsJson() throws IOException, SQLException {
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/api/orders?customer=John%20Doe"));

        Order o1 = new Order(1, "John Doe", "PENDING");
        when(mockRepo.findAll()).thenReturn(Collections.singletonList(o1));

        controller.handle(mockExchange);

        verify(mockExchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("\"customerName\":\"John Doe\""));
    }

    @Test
    void testHandleConfirmOrder_Success() throws IOException, SQLException {
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/orders/confirm"));
        String formData = "id=1";
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(formData.getBytes(StandardCharsets.UTF_8)));

        Order o1 = new Order(1, "John Doe", "PENDING_CONFIRMATION");
        o1.addItem(new OrderItem(1, 2));
        when(mockRepo.findAll()).thenReturn(Collections.singletonList(o1));
        
        when(mockLoyaltyService.accruePoints(anyString(), anyInt(), anyDouble(), anyString())).thenReturn(CompletableFuture.completedFuture(10));
        when(mockFulfillmentService.notifyOrderConfirmed(anyInt())).thenReturn(CompletableFuture.completedFuture(true));

        controller.handle(mockExchange);

        verify(mockRepo).updateStatus(1, "CONFIRMED");
        verify(mockInvoiceRepository).createInvoice(any());
        verify(mockExchange).sendResponseHeaders(eq(302), anyLong());
    }

    @Test
    void testHandleConfirmOrder_FulfillmentFailed() throws IOException, SQLException {
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/orders/confirm"));
        String formData = "id=1";
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(formData.getBytes(StandardCharsets.UTF_8)));

        Order o1 = new Order(1, "John Doe", "PENDING_CONFIRMATION");
        o1.addItem(new OrderItem(1, 2));
        when(mockRepo.findAll()).thenReturn(Collections.singletonList(o1));
        
        when(mockLoyaltyService.accruePoints(anyString(), anyInt(), anyDouble(), anyString())).thenReturn(CompletableFuture.completedFuture(10));
        when(mockFulfillmentService.notifyOrderConfirmed(anyInt())).thenReturn(CompletableFuture.completedFuture(false));

        controller.handle(mockExchange);

        verify(mockRepo).updateStatus(1, "CONFIRMATION_FAILED");
        verify(mockExchange).sendResponseHeaders(eq(500), anyLong());
    }

    @Test
    void testHandleUpdateStatus_Success() throws IOException, SQLException {
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/api/orders/status"));
        String json = "{\"orderId\":1,\"status\":\"SHIPPED\"}";
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        controller.handle(mockExchange);

        verify(mockRepo).updateStatus(1, "SHIPPED");
        verify(mockExchange).sendResponseHeaders(eq(200), anyLong());
    }

    @Test
    void testHandle_MethodNotAllowed() throws IOException {
        when(mockExchange.getRequestMethod()).thenReturn("PUT");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/orders"));

        controller.handle(mockExchange);

        verify(mockExchange).sendResponseHeaders(eq(405), anyLong());
    }

    @Test
    void testHandle_NotFound() throws IOException {
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/unknown"));

        controller.handle(mockExchange);

        verify(mockExchange).sendResponseHeaders(eq(404), anyLong());
    }
}
