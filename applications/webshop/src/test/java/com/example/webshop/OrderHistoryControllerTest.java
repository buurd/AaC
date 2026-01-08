package com.example.webshop;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrderHistoryControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private HttpExchange exchange;

    private OrderHistoryController controller;
    private Headers requestHeaders;
    private Headers responseHeaders;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new OrderHistoryController(orderService);
        requestHeaders = new Headers();
        responseHeaders = new Headers();
        responseBody = new ByteArrayOutputStream();

        when(exchange.getRequestHeaders()).thenReturn(requestHeaders);
        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        when(exchange.getResponseBody()).thenReturn(responseBody);
    }

    @Test
    void testHandle_NoCookie_RedirectsToLogin() throws IOException {
        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(302, -1);
        assertTrue(responseHeaders.getFirst("Location").endsWith("/login"));
    }

    @Test
    void testHandle_WithCookie_ReturnsOrders() throws IOException, ExecutionException, InterruptedException {
        requestHeaders.add("Cookie", "webshop_username=testuser");
        String jsonResponse = "[{\"id\":123,\"customerName\":\"testuser\",\"status\":\"CONFIRMED\",\"pointsEarned\":10,\"pointsRedeemed\":0}]";
        when(orderService.getOrdersForCustomer("testuser")).thenReturn(CompletableFuture.completedFuture(jsonResponse));

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("My Orders"));
        assertTrue(response.contains("123"));
        assertTrue(response.contains("CONFIRMED"));
    }

    @Test
    void testHandle_ServiceFailure_Returns500() throws IOException, ExecutionException, InterruptedException {
        requestHeaders.add("Cookie", "webshop_username=testuser");
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Service down"));
        when(orderService.getOrdersForCustomer("testuser")).thenReturn(future);

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(500), anyLong());
    }
}
