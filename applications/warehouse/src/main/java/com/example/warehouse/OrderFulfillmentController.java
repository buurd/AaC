package com.example.warehouse;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class OrderFulfillmentController implements HttpHandler {

    private final FulfillmentOrderRepository repository;

    public OrderFulfillmentController(FulfillmentOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            try {
                InputStream is = exchange.getRequestBody();
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("Fulfillment request received: " + json);

                int orderId = parseOrderId(json);
                
                repository.createFulfillmentOrder(orderId);

                String response = "{\"status\":\"processing\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            }
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private int parseOrderId(String json) {
        json = json.replace("{", "").replace("}", "").replace("\"", "");
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            if (kv.length < 2) continue;
            String key = kv[0].trim();
            String value = kv[1].trim();
            if ("orderId".equals(key)) {
                return Integer.parseInt(value);
            }
        }
        throw new IllegalArgumentException("No orderId found");
    }
}
