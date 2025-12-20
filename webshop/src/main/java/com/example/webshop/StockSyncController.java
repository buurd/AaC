package com.example.webshop;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class StockSyncController implements HttpHandler {

    private final ProductRepository repository;

    public StockSyncController(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            try {
                InputStream is = exchange.getRequestBody();
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("Stock Sync received: " + json);
                
                // Simple parse: {"pmId": 1, "stock": 100}
                int pmId = 0;
                int stock = 0;
                
                json = json.replace("{", "").replace("}", "").replace("\"", "");
                String[] pairs = json.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split(":");
                    if (kv.length < 2) continue;
                    String key = kv[0].trim();
                    String value = kv[1].trim();
                    
                    if ("pmId".equals(key)) pmId = Integer.parseInt(value);
                    if ("stock".equals(key)) stock = Integer.parseInt(value);
                }
                
                repository.updateStock(pmId, stock);
                
                String response = "{\"status\":\"success\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } catch (Exception e) {
                e.printStackTrace();
                String error = "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
                exchange.sendResponseHeaders(500, error.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.getBytes());
                }
            }
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }
}
