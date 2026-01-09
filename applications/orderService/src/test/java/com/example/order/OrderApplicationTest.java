package com.example.order;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class OrderApplicationTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpExchange exchange;

    private OrderApplication.LoginHandler loginHandler;
    private Headers responseHeaders;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.openMocks(this);
        loginHandler = new OrderApplication.LoginHandler("http://token-url");
        
        // Inject mock HttpClient
        Field httpClientField = OrderApplication.LoginHandler.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(loginHandler, httpClient);

        responseHeaders = new Headers();
        responseBody = new ByteArrayOutputStream();

        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        when(exchange.getResponseBody()).thenReturn(responseBody);
    }

    @Test
    void testHandle_Get_ReturnsLoginForm() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(java.net.URI.create("/login"));

        loginHandler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("<form action='/login' method='post'>"));
    }

    @Test
    void testHandle_Post_Success() throws IOException, InterruptedException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(java.net.URI.create("/login"));
        String formData = "username=testuser&password=password";
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(formData.getBytes()));

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"access_token\":\"mock-token\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        loginHandler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(302), anyLong());
        assertTrue(responseHeaders.getFirst("Location").endsWith("/orders"));
        assertTrue(responseHeaders.getFirst("Set-Cookie").contains("auth_token=mock-token"));
    }

    @Test
    void testHandle_Post_Failure() throws IOException, InterruptedException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(java.net.URI.create("/login"));
        String formData = "username=testuser&password=wrong";
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(formData.getBytes()));

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(401);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        loginHandler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(401), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("Login Failed"));
    }

    @Test
    void testHandle_Post_InvalidForm_Returns400() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(java.net.URI.create("/login"));
        String formData = "invalid-form-data";
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(formData.getBytes()));

        loginHandler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(400), anyLong());
    }

    @Test
    void testMain_Instantiation() {
        // This test is just to cover the class instantiation if possible, 
        // though main method is hard to test fully without side effects.
        // We can at least instantiate the class to cover the default constructor if it exists implicitly.
        OrderApplication app = new OrderApplication();
        assertTrue(app != null);
    }
}
