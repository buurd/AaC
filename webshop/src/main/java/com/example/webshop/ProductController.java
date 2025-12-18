package com.example.webshop;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.stream.Collectors;

public class ProductController implements HttpHandler {

    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String response = repository.findAll().stream()
                                      .map(Product::toString)
                                      .collect(Collectors.joining("\n"));
            
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        } catch (SQLException e) {
            // Basic error handling
            e.printStackTrace();
            String errorResponse = "Internal Server Error";
            exchange.sendResponseHeaders(500, errorResponse.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorResponse.getBytes());
            }
        }
    }
}
