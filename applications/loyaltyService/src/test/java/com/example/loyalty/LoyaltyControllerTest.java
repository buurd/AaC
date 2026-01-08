package com.example.loyalty;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoyaltyControllerTest {

    @Mock
    private PointService pointService;

    @Mock
    private BonusRuleEngine ruleEngine;

    @Mock
    private HttpExchange exchange;

    private LoyaltyController controller;
    private Headers responseHeaders;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new LoyaltyController(pointService, ruleEngine);
        responseHeaders = new Headers();
        responseBody = new ByteArrayOutputStream();

        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        when(exchange.getResponseBody()).thenReturn(responseBody);
    }

    @Test
    void testHandleHome() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/"));

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        assertTrue(responseBody.toString().contains("Loyalty Admin Dashboard"));
    }

    @Test
    void testHandleGetRules() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/loyalty/rules"));
        when(ruleEngine.getRuleDescriptions()).thenReturn(Arrays.asList("Rule 1", "Rule 2"));

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("Rule 1"));
        assertTrue(response.contains("Rule 2"));
    }

    @Test
    void testHandleCalculate() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/loyalty/calculate"));
        
        String jsonRequest = "{\"totalAmount\": 100.0, \"items\": []}";
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(jsonRequest.getBytes(StandardCharsets.UTF_8)));
        when(ruleEngine.evaluate(eq(100.0), anyList())).thenReturn(150);

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        assertTrue(responseBody.toString().contains("150"));
    }

    @Test
    void testHandleDashboard() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/admin/dashboard"));
        when(pointService.getTotalPointsIssued()).thenReturn(5000L);
        when(ruleEngine.isForceJanuaryBonus()).thenReturn(true);
        when(ruleEngine.getRuleDescriptions()).thenReturn(Arrays.asList("Rule A"));

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("5000"));
        assertTrue(response.contains("true"));
        assertTrue(response.contains("Rule A"));
    }

    @Test
    void testHandleToggleBonus() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/loyalty/admin/toggle-bonus"));
        String body = "active=true";
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));

        controller.handle(exchange);

        verify(ruleEngine).setForceJanuaryBonus(true);
        verify(exchange).sendResponseHeaders(eq(303), eq(-1L));
        assertEquals("/admin/dashboard", responseHeaders.getFirst("Location"));
    }

    @Test
    void testHandleAccrueSuccess() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/loyalty/accrue"));
        String jsonRequest = "{\"customerId\": \"cust1\", \"totalAmount\": 50.0}";
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(jsonRequest.getBytes(StandardCharsets.UTF_8)));
        when(pointService.accruePoints(eq("cust1"), eq(50.0), any())).thenReturn(50);

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        assertTrue(responseBody.toString().contains("success"));
        assertTrue(responseBody.toString().contains("50"));
    }

    @Test
    void testHandleAccrueMissingFields() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/loyalty/accrue"));
        String jsonRequest = "{}"; // Missing fields
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(jsonRequest.getBytes(StandardCharsets.UTF_8)));

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(400), anyLong());
    }

    @Test
    void testHandleRedeemSuccess() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/loyalty/redeem"));
        String jsonRequest = "{\"customerId\": \"cust1\", \"pointsToRedeem\": 20}";
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(jsonRequest.getBytes(StandardCharsets.UTF_8)));
        when(pointService.redeemPoints("cust1", 20)).thenReturn(true);
        when(pointService.getBalance("cust1")).thenReturn(80);

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        assertTrue(responseBody.toString().contains("SUCCESS"));
        assertTrue(responseBody.toString().contains("80"));
    }

    @Test
    void testHandleRedeemInsufficientFunds() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/loyalty/redeem"));
        String jsonRequest = "{\"customerId\": \"cust1\", \"pointsToRedeem\": 1000}";
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(jsonRequest.getBytes(StandardCharsets.UTF_8)));
        when(pointService.redeemPoints("cust1", 1000)).thenReturn(false);

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(409), anyLong());
        assertTrue(responseBody.toString().contains("INSUFFICIENT_FUNDS"));
    }

    @Test
    void testHandleGetBalance() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/loyalty/balance/John%20Doe"));
        when(pointService.getBalance("John Doe")).thenReturn(120);

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("John Doe"));
        assertTrue(response.contains("120"));
        assertTrue(response.contains("12.00")); // 120 / 10
    }

    @Test
    void testNotFound() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/unknown"));

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(404), anyLong());
    }
}
