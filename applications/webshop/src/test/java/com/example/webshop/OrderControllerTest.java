package com.example.webshop;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private HttpExchange exchange;

    private OrderController controller;
    private Headers responseHeaders;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new OrderController(orderService);
        responseHeaders = new Headers();
        responseBody = new ByteArrayOutputStream();

        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        when(exchange.getResponseBody()).thenReturn(responseBody);
    }

    @Test
    void testHandle_PostOrder_Success() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        String json = "{\"customerName\":\"John Doe\"}";
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(json.getBytes()));

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(201);
        when(mockResponse.body()).thenReturn("{\"status\":\"created\"}");
        when(orderService.createOrder(anyString())).thenReturn(CompletableFuture.completedFuture(mockResponse));

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(201), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("created"));
    }

    @Test
    void testHandle_PostOrder_Failure() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        String json = "{\"customerName\":\"John Doe\"}";
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(json.getBytes()));

        CompletableFuture<HttpResponse<String>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Service down"));
        when(orderService.createOrder(anyString())).thenReturn(future);

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(500), anyLong());
    }

    @Test
    void testHandle_MethodNotAllowed() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(405), eq(-1L));
    }
}
