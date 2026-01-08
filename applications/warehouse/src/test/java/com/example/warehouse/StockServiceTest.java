package com.example.warehouse;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StockServiceTest {

    @Test
    void testSyncStock_SendsCorrectRequest() {
        // Arrange
        HttpClient mockHttpClient = mock(HttpClient.class);
        TokenService mockTokenService = mock(TokenService.class);
        String webshopUrl = "http://webshop/api/stock/sync";
        
        when(mockTokenService.getAccessToken()).thenReturn(CompletableFuture.completedFuture("mock-token"));
        
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        
        // Fix: Use wildcard for HttpResponse type to match sendAsync signature
        CompletableFuture<HttpResponse<Object>> futureResponse = CompletableFuture.completedFuture((HttpResponse<Object>)(HttpResponse<?>)mockResponse);
        when(mockHttpClient.sendAsync(any(), any())).thenReturn(futureResponse);

        StockService stockService = new StockService(mockHttpClient, webshopUrl, mockTokenService);

        // Act
        stockService.syncStock(123, 50);

        // Assert
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient, timeout(1000)).sendAsync(requestCaptor.capture(), any());

        HttpRequest request = requestCaptor.getValue();
        assertEquals(webshopUrl, request.uri().toString());
        assertEquals("POST", request.method());
        assertEquals("application/json", request.headers().firstValue("Content-Type").orElse(""));
        assertEquals("Bearer mock-token", request.headers().firstValue("Authorization").orElse(""));
    }

    @Test
    void testSyncStock_Failure() {
        // Arrange
        HttpClient mockHttpClient = mock(HttpClient.class);
        TokenService mockTokenService = mock(TokenService.class);
        String webshopUrl = "http://webshop/api/stock/sync";
        
        when(mockTokenService.getAccessToken()).thenReturn(CompletableFuture.completedFuture("mock-token"));
        
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockResponse.body()).thenReturn("Error");
        
        // Fix: Use wildcard for HttpResponse type
        CompletableFuture<HttpResponse<Object>> futureResponse = CompletableFuture.completedFuture((HttpResponse<Object>)(HttpResponse<?>)mockResponse);
        when(mockHttpClient.sendAsync(any(), any())).thenReturn(futureResponse);

        StockService stockService = new StockService(mockHttpClient, webshopUrl, mockTokenService);

        // Act
        stockService.syncStock(123, 50);

        // Assert
        verify(mockHttpClient, timeout(1000)).sendAsync(any(), any());
    }
    
    @Test
    void testSyncStock_Exception() {
        // Arrange
        HttpClient mockHttpClient = mock(HttpClient.class);
        TokenService mockTokenService = mock(TokenService.class);
        String webshopUrl = "http://webshop/api/stock/sync";
        
        when(mockTokenService.getAccessToken()).thenReturn(CompletableFuture.completedFuture("mock-token"));
        
        when(mockHttpClient.sendAsync(any(), any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("Network error")));

        StockService stockService = new StockService(mockHttpClient, webshopUrl, mockTokenService);

        // Act
        stockService.syncStock(123, 50);

        // Assert
        verify(mockHttpClient, timeout(1000)).sendAsync(any(), any());
    }
    
    @Test
    void testConstructors() {
        // Just to cover constructors
        StockService s1 = new StockService();
        StockService s2 = new StockService("http://url", mock(TokenService.class));
    }
}
