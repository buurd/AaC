package com.example.webshop;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderHistoryController implements HttpHandler {

    private final OrderService orderService;

    public OrderHistoryController(OrderService orderService) {
        this.orderService = orderService;
    }

    private static final String CSS = 
        "body { font-family: Arial, Helvetica, sans-serif; background-color: #F8F9FA; color: #343A40; margin: 0; padding: 20px; }" +
        ".container { max-width: 1200px; margin: 0 auto; background-color: #FFFFFF; padding: 20px; border-radius: 4px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
        "h1 { color: #343A40; }" +
        "table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }" +
        "th { background-color: #007BFF; color: #FFFFFF; padding: 12px; text-align: left; }" +
        "td { padding: 12px; border-bottom: 1px solid #DEE2E6; }" +
        "tr:nth-child(even) { background-color: #F2F2F2; }" +
        ".btn { display: inline-block; padding: 10px 20px; border-radius: 4px; text-decoration: none; color: #FFFFFF; font-weight: bold; border: none; cursor: pointer; }" +
        ".btn-secondary { background-color: #6C757D; }";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // In a real app, we'd get the customer name from the session/token.
        // For this demo, we'll hardcode or take from query param for testing.
        // Let's assume "John Doe" for simplicity or parse from a cookie if we had one.
        String customerName = "John Doe"; 

        try {
            String json = orderService.getOrdersForCustomer(customerName).get();
            
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><style>").append(CSS).append("</style></head><body>");
            html.append("<div class='container'>");
            html.append("<div style='display:flex; justify-content:space-between; align-items:center;'>");
            html.append("<h1>My Orders</h1>");
            html.append("<a href='/products' class='btn btn-secondary'>Back to Shop</a>");
            html.append("</div>");
            
            html.append("<table><thead><tr><th>Order ID</th><th>Status</th></tr></thead><tbody>");
            
            // Simple JSON parsing
            Pattern p = Pattern.compile("\\{\"id\":(\\d+),\"customerName\":\"[^\"]+\",\"status\":\"([^\"]+)\"\\}");
            Matcher m = p.matcher(json);
            
            while (m.find()) {
                html.append("<tr>");
                html.append("<td>").append(m.group(1)).append("</td>");
                html.append("<td>").append(m.group(2)).append("</td>");
                html.append("</tr>");
            }
            
            html.append("</tbody></table>");
            html.append("</div></body></html>");
            
            String response = html.toString();
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
            
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            String error = "Failed to load orders";
            exchange.sendResponseHeaders(500, error.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(error.getBytes());
            }
        }
    }
}
