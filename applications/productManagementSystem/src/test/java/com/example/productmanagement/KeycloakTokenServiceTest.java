package com.example.productmanagement;

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

    @Mock
    private HttpResponse<String> httpResponse;

    private KeycloakTokenService tokenService;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.openMocks(this);
        tokenService = new KeycloakTokenService("http://keycloak/token", "client", "secret");

        // Inject mock HttpClient
        Field httpClientField = KeycloakTokenService.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(tokenService, httpClient);
    }

    @Test
    void testGetAccessToken_Success() throws ExecutionException, InterruptedException {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"access_token\":\"mock-token\",\"expires_in\":300}");
        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(httpResponse));

        String token = tokenService.getAccessToken().get();
        assertEquals("mock-token", token);
    }

    @Test
    void testGetAccessToken_Cached() throws ExecutionException, InterruptedException {
        // First call to populate cache
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"access_token\":\"mock-token\",\"expires_in\":300}");
        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(httpResponse));

        tokenService.getAccessToken().get();

        // Second call should use cache and not call HttpClient
        String token = tokenService.getAccessToken().get();
        assertEquals("mock-token", token);
        
        verify(httpClient, times(1)).sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testGetAccessToken_Failure() {
        when(httpResponse.statusCode()).thenReturn(401);
        when(httpResponse.body()).thenReturn("Unauthorized");
        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(httpResponse));

        assertThrows(ExecutionException.class, () -> tokenService.getAccessToken().get());
    }

    @Test
    void testGetAccessToken_Exception() {
        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Network error")));

        assertThrows(ExecutionException.class, () -> tokenService.getAccessToken().get());
    }
}
