package com.example.webshop;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class WebshopApplication {

    public static void main(String[] args) throws IOException, SQLException {
        // --- Database Setup ---
        // The validation script will provide the DB_URL as an environment variable
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/postgres");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");

        System.out.println("Connecting to database at: " + dbUrl);
        Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        System.out.println("Database connection successful.");

        // Initialize schema
        try (InputStream is = WebshopApplication.class.getResourceAsStream("/schema.sql")) {
            if (is == null) {
                throw new IOException("Cannot find schema.sql in resources.");
            }
            String schemaSql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(schemaSql);
                System.out.println("Database schema initialized.");
            }
        }

        // Create repository
        ProductRepository productRepository = new ProductRepository(connection);

        // --- HTTP Server Setup ---
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/", (exchange) -> {
            String response = "Welcome to the Webshop!";
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });
        
        server.createContext("/products", new ProductController(productRepository));
        
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port " + port);
    }
}
