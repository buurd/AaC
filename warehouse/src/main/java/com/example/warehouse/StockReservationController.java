package com.example.warehouse;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class StockReservationController implements HttpHandler {

    private final DeliveryRepository repository;
    private final ProductRepository productRepository;
    private final StockService stockService;

    public StockReservationController(DeliveryRepository repository, ProductRepository productRepository, StockService stockService) {
        this.repository = repository;
        this.productRepository = productRepository;
        this.stockService = stockService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            try {
                InputStream is = exchange.getRequestBody();
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("Stock Reservation received: " + json);
                
                // Simple parse: {"productId": 1, "quantity": 1}
                int productId = 0;
                int quantity = 0;
                
                json = json.replace("{", "").replace("}", "").replace("\"", "");
                String[] pairs = json.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split(":");
                    if (kv.length < 2) continue;
                    String key = kv[0].trim();
                    String value = kv[1].trim();
                    
                    if ("productId".equals(key)) productId = Integer.parseInt(value);
                    if ("quantity".equals(key)) quantity = Integer.parseInt(value);
                }
                
                boolean success = repository.reserveStock(productId, quantity);
                
                if (success) {
                    // Trigger sync to Webshop
                    int newStock = repository.countStock(productId);
                    Product p = productRepository.findById(productId);
                    if (p != null && p.getPmId() != null) {
                        stockService.syncStock(p.getPmId(), newStock);
                    }

                    String response = "{\"status\":\"reserved\"}";
                    exchange.sendResponseHeaders(200, response.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                } else {
                    String response = "{\"status\":\"insufficient_stock\"}";
                    exchange.sendResponseHeaders(409, response.length()); // Conflict
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            }
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }
}
