package com.example.webshop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KeycloakTokenServiceTest {

    private KeycloakTokenService keycloakTokenService;
    private HttpClient mockHttpClient;
    private String tokenUrl = "http://keycloak/token";
    private String clientId = "client-id";
    private String clientSecret = "client-secret";

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        keycloakTokenService = new KeycloakTokenService(tokenUrl, clientId, clientSecret, mockHttpClient);
    }

    @Test
    void getAccessToken_success() throws ExecutionException, InterruptedException {
        // Arrange
        String expectedToken = "access-token-123";
        String jsonResponse = "{\"access_token\":\"" + expectedToken + "\",\"expires_in\":300}";
        
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // Act
        String token = keycloakTokenService.getAccessToken().get();

        // Assert
        assertEquals(expectedToken, token);
        
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).sendAsync(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        
        HttpRequest request = requestCaptor.getValue();
        assertEquals(tokenUrl, request.uri().toString());
        assertEquals("application/x-www-form-urlencoded", request.headers().firstValue("Content-Type").orElse(null));
        assertEquals("POST", request.method());
    }

    @Test
    void getAccessToken_cached() throws ExecutionException, InterruptedException {
        // Arrange
        String expectedToken = "access-token-123";
        String jsonResponse = "{\"access_token\":\"" + expectedToken + "\",\"expires_in\":300}";
        
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // Act
        String token1 = keycloakTokenService.getAccessToken().get();
        String token2 = keycloakTokenService.getAccessToken().get();

        // Assert
        assertEquals(expectedToken, token1);
        assertEquals(expectedToken, token2);
        
        // Should be called only once
        verify(mockHttpClient, times(1)).sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void getAccessToken_failure() {
        // Arrange
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(401);
        when(mockResponse.body()).thenReturn("Unauthorized");
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // Act & Assert
        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            keycloakTokenService.getAccessToken().get();
        });
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    @Test
    void getAccessToken_invalidResponse() {
        // Arrange
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{}"); // Missing access_token
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // Act & Assert
        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            keycloakTokenService.getAccessToken().get();
        });
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("Invalid response", exception.getCause().getMessage());
    }
}
