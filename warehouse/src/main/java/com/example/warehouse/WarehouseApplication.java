package com.example.warehouse;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Executors;

public class WarehouseApplication {

    private static final String CSS = 
        "body { font-family: Arial, Helvetica, sans-serif; background-color: #F8F9FA; color: #343A40; margin: 0; padding: 20px; }" +
        ".container { max-width: 1200px; margin: 0 auto; background-color: #FFFFFF; padding: 20px; border-radius: 4px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
        "h1 { color: #343A40; }" +
        ".btn { display: inline-block; padding: 10px 20px; border-radius: 4px; text-decoration: none; color: #FFFFFF; font-weight: bold; border: none; cursor: pointer; margin-right: 10px; }" +
        ".btn-primary { background-color: #007BFF; }" +
        ".btn-secondary { background-color: #6C757D; }";

    public static void main(String[] args) throws IOException, SQLException {
        // --- Database Setup ---
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/postgres");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");

        System.out.println("Connecting to database at: " + dbUrl);
        Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        System.out.println("Database connection successful.");

        // Initialize schema
        try (InputStream is = WarehouseApplication.class.getResourceAsStream("/schema.sql")) {
            if (is == null) {
                throw new IOException("Cannot find schema.sql in resources.");
            }
            String schemaSql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement stmt = connection.createStatement()) {
                String[] statements = schemaSql.split(";");
                for (String sql : statements) {
                    if (!sql.trim().isEmpty()) {
                        stmt.execute(sql);
                    }
                }
                System.out.println("Database schema initialized.");
            }
        }

        ProductRepository productRepository = new ProductRepository(connection);
        DeliveryRepository deliveryRepository = new DeliveryRepository(connection);
        StockService stockService = new StockService();

        // --- HTTP Server Setup ---
        int port = 8002;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/", (exchange) -> {
            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path)) {
                String html = "<!DOCTYPE html><html><head><style>" + CSS + "</style></head><body><div class='container'>" +
                              "<h1>Warehouse Service</h1>" +
                              "<p>Manage inventory and deliveries.</p>" +
                              "<a href='/products' class='btn btn-secondary'>View Products</a>" +
                              "<a href='/deliveries' class='btn btn-primary'>Manage Deliveries</a>" +
                              "</div></body></html>";
                byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                String response = "404 Not Found";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
            exchange.close();
        });
        
        server.createContext("/products", new ProductController(productRepository));
        server.createContext("/api/products/sync", new ProductSyncController(productRepository));
        server.createContext("/deliveries", new DeliveryController(deliveryRepository, productRepository, stockService));
        
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Warehouse Service started on port " + port);
    }
}
