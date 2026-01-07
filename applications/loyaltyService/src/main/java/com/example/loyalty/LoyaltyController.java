package com.example.loyalty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LoyaltyController implements HttpHandler {

    private final PointService pointService;
    private final BonusRuleEngine ruleEngine;
    private final ObjectMapper objectMapper;

    public LoyaltyController(PointService pointService, BonusRuleEngine ruleEngine) {
        this.pointService = pointService;
        this.ruleEngine = ruleEngine;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        System.out.println("LoyaltyController received " + method + " " + path);

        if ("GET".equalsIgnoreCase(method) && path.equals("/api/loyalty/rules")) {
            handleGetRules(exchange);
        } else if ("POST".equalsIgnoreCase(method) && path.equals("/api/loyalty/calculate")) {
            handleCalculate(exchange);
        } else if ("GET".equalsIgnoreCase(method) && path.equals("/")) {
            handleHome(exchange);
        } else if ("GET".equalsIgnoreCase(method) && path.equals("/admin/dashboard")) {
            handleDashboard(exchange);
        } else if ("POST".equalsIgnoreCase(method) && path.endsWith("/accrue")) {
            handleAccrue(exchange);
        } else if ("POST".equalsIgnoreCase(method) && path.endsWith("/redeem")) {
            handleRedeem(exchange);
        } else if ("GET".equalsIgnoreCase(method) && path.contains("/balance/")) {
            handleGetBalance(exchange);
        } else if ("POST".equalsIgnoreCase(method) && path.endsWith("/admin/toggle-bonus")) {
            handleToggleBonus(exchange);
        } else {
            sendResponse(exchange, 404, "{\"status\":\"not_found\"}");
        }
    }

    private void handleHome(HttpExchange exchange) throws IOException {
        String response = "<html><body><h1>Loyalty Admin Dashboard</h1></body></html>";
        sendResponse(exchange, 200, response);
    }

    private void handleGetRules(HttpExchange exchange) throws IOException {
        List<String> rules = ruleEngine.getRuleDescriptions();
        String response = objectMapper.writeValueAsString(rules);
        sendResponse(exchange, 200, response);
    }

    private void handleCalculate(HttpExchange exchange) throws IOException {
        try {
            InputStream is = exchange.getRequestBody();
            AccrueRequest request = objectMapper.readValue(is, AccrueRequest.class);
            
            // We don't need customerId for calculation, just amount and items
            int points = ruleEngine.evaluate(request.getTotalAmount(), request.getItems());
            sendResponse(exchange, 200, "{\"points\":" + points + "}");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleDashboard(HttpExchange exchange) throws IOException {
        long totalPoints = pointService.getTotalPointsIssued();
        boolean bonusActive = ruleEngine.isForceJanuaryBonus();
        List<String> rules = ruleEngine.getRuleDescriptions();
        
        StringBuilder rulesHtml = new StringBuilder("<ul>");
        for (String rule : rules) {
            rulesHtml.append("<li>").append(rule).append("</li>");
        }
        rulesHtml.append("</ul>");

        String html = "<html>" +
                "<head><title>Loyalty Admin</title><style>body{font-family:sans-serif;padding:20px;}.card{border:1px solid #ccc;padding:20px;margin:10px;border-radius:5px;}</style></head>" +
                "<body>" +
                "<h1>Loyalty Admin Dashboard</h1>" +
                "<div class='card'>" +
                "<h2>Statistics</h2>" +
                "<p>Total Points Issued: <strong>" + totalPoints + "</strong></p>" +
                "</div>" +
                "<div class='card'>" +
                "<h2>Campaign Management</h2>" +
                "<p>January Bonus Active: <strong>" + bonusActive + "</strong></p>" +
                "<form method='POST' action='/api/loyalty/admin/toggle-bonus'>" +
                "<input type='hidden' name='active' value='" + (!bonusActive) + "'>" +
                "<button type='submit'>" + (bonusActive ? "Disable" : "Enable") + " Bonus</button>" +
                "</form>" +
                "<h3>Available Rules:</h3>" +
                rulesHtml.toString() +
                "</div>" +
                "</body>" +
                "</html>";

        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void handleToggleBonus(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        // Simple form parsing (active=true)
        boolean active = body.contains("active=true");
        ruleEngine.setForceJanuaryBonus(active);
        
        // Redirect back to dashboard
        exchange.getResponseHeaders().set("Location", "/admin/dashboard");
        exchange.sendResponseHeaders(303, -1);
    }

    private void handleAccrue(HttpExchange exchange) throws IOException {
        try {
            InputStream is = exchange.getRequestBody();
            AccrueRequest request = objectMapper.readValue(is, AccrueRequest.class);
            
            if (request.getCustomerId() != null && request.getTotalAmount() > 0) {
                int points = pointService.accruePoints(request.getCustomerId(), request.getTotalAmount(), request.getItems());
                sendResponse(exchange, 200, "{\"status\":\"success\",\"pointsAccrued\":" + points + "}");
            } else {
                sendResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"Missing fields\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleRedeem(HttpExchange exchange) throws IOException {
        try {
            InputStream is = exchange.getRequestBody();
            JsonNode node = objectMapper.readTree(is);
            
            String customerId = node.has("customerId") ? node.get("customerId").asText() : null;
            int points = node.has("pointsToRedeem") ? node.get("pointsToRedeem").asInt() : 0;

            if (customerId != null && points > 0) {
                boolean success = pointService.redeemPoints(customerId, points);
                if (success) {
                    int remaining = pointService.getBalance(customerId);
                    sendResponse(exchange, 200, "{\"status\":\"SUCCESS\",\"pointsRedeemed\":" + points + ",\"remainingBalance\":" + remaining + "}");
                } else {
                    sendResponse(exchange, 409, "{\"status\":\"INSUFFICIENT_FUNDS\"}");
                }
            } else {
                sendResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"Missing fields\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleGetBalance(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String customerId = path.substring(path.lastIndexOf("/") + 1);
        // Decode URL encoded customer ID (e.g. John%20Doe -> John Doe)
        customerId = java.net.URLDecoder.decode(customerId, StandardCharsets.UTF_8);
        
        int points = pointService.getBalance(customerId);
        double value = points / 10.0; // 10 points = 1 Euro
        
        // Add Cache-Control header to prevent caching
        exchange.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate");
        
        String response = String.format("{\"customerId\":\"%s\",\"points\":%d,\"value\":%.2f,\"currency\":\"EUR\"}", customerId, points, value);
        sendResponse(exchange, 200, response);
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void sendResponse(HttpExchange exchange, int status, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        // Set Content-Type based on response content
        if (response.trim().startsWith("{") || response.trim().startsWith("[")) {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
        } else {
            exchange.getResponseHeaders().set("Content-Type", "text/html");
        }
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
