package com.example.warehouse;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WarehouseApplicationTest {

    @Test
    void loginHandler_get_returnsLoginForm() throws IOException {
        // Arrange
        WarehouseApplication.LoginHandler handler = new WarehouseApplication.LoginHandler("http://token-url", mock(HttpClient.class));
        HttpExchange mockExchange = mock(HttpExchange.class);
        
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/login"));
        
        Headers responseHeaders = new Headers();
        when(mockExchange.getResponseHeaders()).thenReturn(responseHeaders);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(mockExchange.getResponseBody()).thenReturn(outputStream);

        // Act
        handler.handle(mockExchange);

        // Assert
        verify(mockExchange).sendResponseHeaders(eq(200), anyLong());
        String response = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(response.contains("<form action='/login' method='post'>"));
    }

    @Test
    void loginHandler_post_success() throws IOException, InterruptedException {
        // Arrange
        HttpClient mockHttpClient = mock(HttpClient.class);
        WarehouseApplication.LoginHandler handler = new WarehouseApplication.LoginHandler("http://token-url", mockHttpClient);
        HttpExchange mockExchange = mock(HttpExchange.class);
        
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/login"));
        
        String formData = "username=testuser&password=testpass";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(formData.getBytes(StandardCharsets.UTF_8));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        
        Headers responseHeaders = new Headers();
        when(mockExchange.getResponseHeaders()).thenReturn(responseHeaders);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(mockExchange.getResponseBody()).thenReturn(outputStream);
        
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"access_token\":\"test-token\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        // Act
        handler.handle(mockExchange);

        // Assert
        verify(mockExchange).sendResponseHeaders(eq(302), eq(-1L));
        assertTrue(responseHeaders.getFirst("Set-Cookie").contains("auth_token=test-token"));
        assertEquals("/", responseHeaders.getFirst("Location"));
    }
    
    @Test
    void loginHandler_post_failure() throws IOException, InterruptedException {
        // Arrange
        HttpClient mockHttpClient = mock(HttpClient.class);
        WarehouseApplication.LoginHandler handler = new WarehouseApplication.LoginHandler("http://token-url", mockHttpClient);
        HttpExchange mockExchange = mock(HttpExchange.class);
        
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/login"));
        
        String formData = "username=testuser&password=wrongpass";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(formData.getBytes(StandardCharsets.UTF_8));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        
        Headers responseHeaders = new Headers();
        when(mockExchange.getResponseHeaders()).thenReturn(responseHeaders);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(mockExchange.getResponseBody()).thenReturn(outputStream);
        
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(401);
        when(mockResponse.body()).thenReturn("Unauthorized");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        // Act
        handler.handle(mockExchange);

        // Assert
        verify(mockExchange).sendResponseHeaders(eq(401), anyLong());
        String response = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(response.contains("Login Failed"));
    }
}
