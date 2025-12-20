package com.example.productmanagement;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Locale;

public class ProductService {

    private final ProductRepository repository;
    private final HttpClient httpClient;
    private final String webshopApiUrl;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.webshopApiUrl = System.getenv().getOrDefault("WEBSHOP_API_URL", "http://localhost:8000/api/products/sync");
        System.out.println("ProductService initialized with Webshop API URL: " + this.webshopApiUrl);
    }

    public void createProduct(Product product) throws SQLException {
        // 1. Save to local DB
        int newId = repository.create(product);
        product.setId(newId);

        // 2. Sync to Webshop
        syncToWebshop(product);
    }

    public void updateProduct(Product product) throws SQLException {
        // 1. Update local DB
        repository.update(product);

        // 2. Sync to Webshop
        syncToWebshop(product);
    }

    public void deleteProduct(int id) throws SQLException {
        repository.delete(id);
        syncDeleteToWebshop(id);
    }

    public void syncProduct(int id) throws SQLException {
        Product p = repository.findById(id);
        if (p != null) {
            syncToWebshop(p);
        }
    }

    private void syncToWebshop(Product p) {
        try {
            System.out.println("Syncing product " + p.getId() + " to " + webshopApiUrl);
            // Use Locale.US to ensure dot separator for price
            String json = String.format(Locale.US,
                "{\"id\":%d,\"type\":\"%s\",\"name\":\"%s\",\"description\":\"%s\",\"price\":%.2f,\"unit\":\"%s\"}",
                p.getId(), p.getType(), p.getName(), p.getDescription(), p.getPrice(), p.getUnit()
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webshopApiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != 200) {
                            System.err.println("Failed to sync product to Webshop. Status: " + response.statusCode() + " Body: " + response.body());
                        } else {
                            System.out.println("Synced product " + p.getId() + " to Webshop. Response: " + response.body());
                        }
                    })
                    .exceptionally(e -> {
                        System.err.println("Exception during sync: " + e.getMessage());
                        e.printStackTrace();
                        return null;
                    });
        } catch (Exception e) {
            System.err.println("Error preparing sync request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void syncDeleteToWebshop(int id) {
        try {
            System.out.println("Syncing delete product " + id + " to " + webshopApiUrl);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webshopApiUrl + "?id=" + id))
                    .DELETE()
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != 200) {
                            System.err.println("Failed to sync delete to Webshop. Status: " + response.statusCode() + " Body: " + response.body());
                        } else {
                            System.out.println("Synced delete product " + id + " to Webshop. Response: " + response.body());
                        }
                    })
                    .exceptionally(e -> {
                        System.err.println("Exception during delete sync: " + e.getMessage());
                        e.printStackTrace();
                        return null;
                    });
        } catch (Exception e) {
            System.err.println("Error preparing delete sync request: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
