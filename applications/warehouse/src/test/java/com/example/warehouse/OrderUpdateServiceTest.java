package com.example.warehouse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderUpdateServiceTest {

    private OrderUpdateService orderUpdateService;
    private HttpClient mockHttpClient;
    private TokenService mockTokenService;
    private String orderServiceUrl = "http://test-order-service/api/orders/status";

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        mockTokenService = mock(TokenService.class);
        orderUpdateService = new OrderUpdateService(orderServiceUrl, mockTokenService, mockHttpClient);
    }

    @Test
    void updateStatus_success() {
        // Arrange
        int orderId = 123;
        String status = "SHIPPED";
        String token = "test-token";

        when(mockTokenService.getAccessToken()).thenReturn(CompletableFuture.completedFuture(token));

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // Act
        orderUpdateService.updateStatus(orderId, status);

        // Assert
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient, timeout(1000)).sendAsync(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));

        HttpRequest capturedRequest = requestCaptor.getValue();
        assertEquals(orderServiceUrl, capturedRequest.uri().toString());
        assertEquals("Bearer " + token, capturedRequest.headers().firstValue("Authorization").orElse(null));
        assertEquals("application/json", capturedRequest.headers().firstValue("Content-Type").orElse(null));
        assertEquals("POST", capturedRequest.method());
    }

    @Test
    void updateStatus_failure() {
        // Arrange
        int orderId = 123;
        String status = "SHIPPED";
        String token = "test-token";

        when(mockTokenService.getAccessToken()).thenReturn(CompletableFuture.completedFuture(token));

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockResponse.body()).thenReturn("Internal Server Error");
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // Act
        orderUpdateService.updateStatus(orderId, status);

        // Assert
        verify(mockHttpClient, timeout(1000)).sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        // We can't easily assert the System.err output without redirecting it, but we verify the interaction happened.
    }
    
    @Test
    void updateStatus_exception() {
        // Arrange
        int orderId = 123;
        String status = "SHIPPED";
        String token = "test-token";

        when(mockTokenService.getAccessToken()).thenReturn(CompletableFuture.completedFuture(token));

        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Network error")));

        // Act
        orderUpdateService.updateStatus(orderId, status);

        // Assert
        verify(mockHttpClient, timeout(1000)).sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
}
