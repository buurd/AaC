package com.example.loyalty;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoyaltyApplicationTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpExchange exchange;

    @Mock
    private HttpResponse<String> httpResponse;

    private LoyaltyApplication.LoginHandler loginHandler;
    private Headers responseHeaders;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        loginHandler = new LoyaltyApplication.LoginHandler("http://token-url", httpClient);
        
        responseHeaders = new Headers();
        responseBody = new ByteArrayOutputStream();

        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        when(exchange.getResponseBody()).thenReturn(responseBody);
    }

    @Test
    void testHandleGetLogin() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");

        loginHandler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        assertTrue(responseBody.toString().contains("<form action='/login' method='post'>"));
    }

    @Test
    void testHandlePostLoginSuccess() throws IOException, InterruptedException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        String formData = "username=admin&password=password";
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(formData.getBytes(StandardCharsets.UTF_8)));

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"access_token\":\"mock-token\"}");

        loginHandler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(302), eq(-1L));
        assertEquals("/admin/dashboard", responseHeaders.getFirst("Location"));
        assertTrue(responseHeaders.getFirst("Set-Cookie").contains("auth_token=mock-token"));
    }

    @Test
    void testHandlePostLoginFailure() throws IOException, InterruptedException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        String formData = "username=admin&password=wrong";
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(formData.getBytes(StandardCharsets.UTF_8)));

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(401);

        loginHandler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(401), anyLong());
        assertTrue(responseBody.toString().contains("Invalid credentials"));
    }

    @Test
    void testHandlePostLoginError() throws IOException, InterruptedException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        String formData = "username=admin&password=password";
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(formData.getBytes(StandardCharsets.UTF_8)));

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new InterruptedException("Network error"));

        loginHandler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(500), anyLong());
        assertTrue(responseBody.toString().contains("Network error"));
    }
}
