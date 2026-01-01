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
        when(mockHttpClient.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(mock(HttpResponse.class)));

        StockService stockService = new StockService(mockHttpClient, webshopUrl, mockTokenService);

        // Act
        stockService.syncStock(123, 50);

        // Assert
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).sendAsync(requestCaptor.capture(), any());

        HttpRequest request = requestCaptor.getValue();
        assertEquals(webshopUrl, request.uri().toString());
        assertEquals("POST", request.method());
        assertEquals("application/json", request.headers().firstValue("Content-Type").orElse(""));
        assertEquals("Bearer mock-token", request.headers().firstValue("Authorization").orElse(""));
        
        // We can't easily check the body of HttpRequest as it's a BodyPublisher, 
        // but we verified the structure and headers.
    }
}
