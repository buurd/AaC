package com.example.warehouse;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class ProductSyncController implements HttpHandler {

    private final ProductRepository repository;

    public ProductSyncController(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        
        if ("POST".equalsIgnoreCase(method)) {
            try {
                InputStream is = exchange.getRequestBody();
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("Warehouse Sync received: " + json);
                
                Product p = parseJson(json);
                if (p.getPmId() == null || p.getPmId() == 0) {
                    throw new IllegalArgumentException("Invalid JSON: Missing or invalid 'id'");
                }
                repository.upsert(p);
                
                sendResponse(exchange, 200, "{\"status\":\"success\"}");
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
            }
        } else if ("DELETE".equalsIgnoreCase(method)) {
            try {
                String query = exchange.getRequestURI().getQuery();
                System.out.println("Warehouse Sync received DELETE query: " + query);
                int pmId = parseIdFromQuery(query);
                
                repository.deleteByPmId(pmId);
                
                sendResponse(exchange, 200, "{\"status\":\"deleted\"}");
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
            }
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void sendResponse(HttpExchange exchange, int status, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private int parseIdFromQuery(String query) {
        if (query == null) throw new IllegalArgumentException("No query parameters");
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && "id".equals(pair[0])) {
                return Integer.parseInt(pair[1]);
            }
        }
        throw new IllegalArgumentException("No id parameter found");
    }

    private Product parseJson(String json) {
        Product p = new Product();
        json = json.replace("{", "").replace("}", "").replace("\"", "");
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            if (kv.length < 2) continue;
            String key = kv[0].trim();
            String value = kv[1].trim();
            
            switch (key) {
                case "id": p.setPmId(Integer.parseInt(value)); break;
                case "name": p.setName(value); break;
            }
        }
        return p;
    }
}
