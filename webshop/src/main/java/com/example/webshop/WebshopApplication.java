package com.example.webshop;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpContext;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebshopApplication {

    private static final Logger logger = LoggerFactory.getLogger(WebshopApplication.class);

    private static final String CSS = 
        "body { font-family: Arial, Helvetica, sans-serif; background-color: #F8F9FA; color: #343A40; margin: 0; padding: 20px; }" +
        ".container { max-width: 1200px; margin: 0 auto; background-color: #FFFFFF; padding: 20px; border-radius: 4px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
        "h1 { color: #343A40; }" +
        ".btn { display: inline-block; padding: 10px 20px; border-radius: 4px; text-decoration: none; color: #FFFFFF; font-weight: bold; border: none; cursor: pointer; }" +
        ".btn-primary { background-color: #007BFF; }";

    public static void main(String[] args) throws IOException, SQLException {
        // --- Database Setup ---
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/postgres");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");

        // Security Setup
        String jwksUrl = System.getenv().getOrDefault("JWKS_URL", "http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/certs");
        String issuer = System.getenv().getOrDefault("ISSUER_URL", "https://localhost:8446/realms/webshop-realm");

        logger.info("Connecting to database at: {}", dbUrl);
        Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        logger.info("Database connection successful.");

        // Initialize schema
        try (InputStream is = WebshopApplication.class.getResourceAsStream("/schema.sql")) {
            if (is == null) {
                throw new IOException("Cannot find schema.sql in resources.");
            }
            String schemaSql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(schemaSql);
                logger.info("Database schema initialized.");
            }
        }

        // Create repository
        ProductRepository productRepository = new ProductRepository(connection);

        // Security Filters
        SecurityFilter productSyncFilter = new SecurityFilter(jwksUrl, issuer, "product-sync");
        SecurityFilter stockSyncFilter = new SecurityFilter(jwksUrl, issuer, "stock-sync");

        // --- HTTP Server Setup ---
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/", (exchange) -> {
            String path = exchange.getRequestURI().getPath();
            logger.info("Received request: {} {}", exchange.getRequestMethod(), path);
            if ("/".equals(path)) {
                String html = "<!DOCTYPE html><html><head><style>" + CSS + "</style></head><body><div class='container'>" +
                              "<h1>Welcome to the Webshop!</h1>" +
                              "<p>Browse our amazing products.</p>" +
                              "<a href='/products' class='btn btn-primary'>View Products</a>" +
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
        server.createContext("/cart", new ShoppingCartController());
        
        HttpContext productSyncContext = server.createContext("/api/products/sync", new ProductSyncController(productRepository));
        productSyncContext.getFilters().add(productSyncFilter);
        
        HttpContext stockSyncContext = server.createContext("/api/stock/sync", new StockSyncController(productRepository));
        stockSyncContext.getFilters().add(stockSyncFilter);
        
        // Use a thread pool to handle multiple requests concurrently
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        logger.info("Server started on port {}", port);
    }
}
