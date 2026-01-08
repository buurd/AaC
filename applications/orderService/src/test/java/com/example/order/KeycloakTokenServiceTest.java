package com.example.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KeycloakTokenServiceTest {

    @Mock
    private HttpClient httpClient;

    private KeycloakTokenService tokenService;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.openMocks(this);
        tokenService = new KeycloakTokenService("http://keycloak/token", "client", "secret");

        // Inject mock HttpClient using reflection
        Field httpClientField = KeycloakTokenService.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(tokenService, httpClient);
    }

    @Test
    void testGetAccessToken_Success() throws ExecutionException, InterruptedException {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"access_token\":\"mock-token\",\"expires_in\":300}");
        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        String token = tokenService.getAccessToken().get();
        assertEquals("mock-token", token);
    }

    @Test
    void testGetAccessToken_Failure() {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(401);
        when(mockResponse.body()).thenReturn("Unauthorized");
        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            tokenService.getAccessToken().get();
        });
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("Failed to get access token", exception.getCause().getMessage());
    }
    
    @Test
    void testGetAccessToken_Cached() throws ExecutionException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        // Set cached token
        Field accessTokenField = KeycloakTokenService.class.getDeclaredField("accessToken");
        accessTokenField.setAccessible(true);
        accessTokenField.set(tokenService, "cached-token");
        
        Field tokenExpiresAtField = KeycloakTokenService.class.getDeclaredField("tokenExpiresAt");
        tokenExpiresAtField.setAccessible(true);
        tokenExpiresAtField.setLong(tokenService, System.currentTimeMillis() + 10000); // Valid for 10s

        String token = tokenService.getAccessToken().get();
        assertEquals("cached-token", token);
        verify(httpClient, never()).sendAsync(any(), any());
    }
}
