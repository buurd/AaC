package com.example.warehouse;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.net.URLDecoder;

public class FulfillmentController implements HttpHandler {

    private final FulfillmentOrderRepository repository;
    private final OrderUpdateService orderUpdateService;

    public FulfillmentController(FulfillmentOrderRepository repository, OrderUpdateService orderUpdateService) {
        this.repository = repository;
        this.orderUpdateService = orderUpdateService;
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
        ".btn-success { background-color: #28A745; }" +
        ".btn-secondary { background-color: #6C757D; }";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        
        if ("GET".equalsIgnoreCase(method)) {
            handleList(exchange);
        } else if ("POST".equalsIgnoreCase(method)) {
            handleUpdate(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleList(HttpExchange exchange) throws IOException {
        try {
            List<FulfillmentOrder> orders = repository.findAllFulfillmentOrders();
            
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><style>").append(CSS).append("</style></head><body>");
            html.append("<div class='container'>");
            html.append("<div style='display:flex; justify-content:space-between; align-items:center;'>");
            html.append("<h1>Order Fulfillment</h1>");
            html.append("<a href='/' class='btn btn-secondary'>Back to Dashboard</a>");
            html.append("</div>");
            
            html.append("<table><thead><tr><th>Order ID</th><th>Status</th><th>Action</th></tr></thead><tbody>");
            
            for (FulfillmentOrder o : orders) {
                html.append("<tr>");
                html.append("<td>").append(o.getOrderId()).append("</td>");
                html.append("<td>").append(o.getStatus()).append("</td>");
                html.append("<td>");
                if ("PENDING".equals(o.getStatus())) {
                    html.append("<form method='post' style='display:inline;'>");
                    html.append("<input type='hidden' name='orderId' value='").append(o.getOrderId()).append("'>");
                    html.append("<input type='hidden' name='status' value='SHIPPED'>");
                    html.append("<button type='submit' class='btn btn-success'>Mark Shipped</button>");
                    html.append("</form>");
                }
                html.append("</td>");
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
        } catch (SQLException e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleUpdate(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        String formData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = parseFormData(formData);
        
        try {
            int orderId = Integer.parseInt(params.get("orderId"));
            String status = params.get("status");
            
            repository.updateFulfillmentStatus(orderId, status);

            // Notify Order Service
            if (orderUpdateService != null) {
                orderUpdateService.updateStatus(orderId, status);
            }

            // Redirect back to list
            exchange.getResponseHeaders().set("Location", "/fulfillment");
            exchange.sendResponseHeaders(302, -1);
        } catch (Exception e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private Map<String, String> parseFormData(String formData) {
        Map<String, String> map = new HashMap<>();
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length > 0) {
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : "";
                map.put(key, value);
            }
        }
        return map;
    }
}
