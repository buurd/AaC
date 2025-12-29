package com.example.productmanagement;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class ProductService {

    private final ProductRepository repository;
    private final HttpClient httpClient;
    private final String webshopApiUrl;
    private final String warehouseApiUrl;
    private final TokenService tokenService;

    public ProductService(ProductRepository repository) {
        this(repository, 
             System.getenv().getOrDefault("WEBSHOP_API_URL", "http://webshop-demo:8000/api/products/sync"),
             System.getenv().getOrDefault("WAREHOUSE_API_URL", "http://warehouse-demo:8002/api/products/sync"),
             new KeycloakTokenService(
                 System.getenv().getOrDefault("TOKEN_URL", "http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/token"),
                 System.getenv().getOrDefault("CLIENT_ID", "pm-client"),
                 System.getenv().getOrDefault("CLIENT_SECRET", "pm-secret")
             ));
    }

    public ProductService(ProductRepository repository, String webshopApiUrl, String warehouseApiUrl, TokenService tokenService) {
        this.repository = repository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.webshopApiUrl = webshopApiUrl;
        this.warehouseApiUrl = warehouseApiUrl;
        this.tokenService = tokenService;

        System.out.println("ProductService initialized with Webshop API URL: " + this.webshopApiUrl);
        System.out.println("ProductService initialized with Warehouse API URL: " + this.warehouseApiUrl);
    }

    public void createProduct(Product product) throws SQLException {
        // 1. Save to local DB
        int newId = repository.create(product);
        product.setId(newId);

        // 2. Sync to Webshop and Warehouse
        // Automatic sync removed as per requirements change (REQ-025, REQ-033)
        // syncToWebshop(product);
        // syncToWarehouse(product);
    }

    public void updateProduct(Product product) throws SQLException {
        // 1. Update local DB
        repository.update(product);

        // 2. Sync to Webshop and Warehouse
        // Automatic sync removed as per requirements change (REQ-025, REQ-033)
        // syncToWebshop(product);
        // syncToWarehouse(product);
    }

    public void deleteProduct(int id) throws SQLException {
        repository.delete(id);
        syncDeleteToWebshop(id);
        syncDeleteToWarehouse(id);
    }

    public void syncProduct(int id) throws SQLException {
        Product p = repository.findById(id);
        if (p != null) {
            syncToWebshop(p);
            syncToWarehouse(p);
        }
    }

    private void syncToWebshop(Product p) {
        tokenService.getAccessToken().thenAccept(token -> {
            try {
                System.out.println("Syncing product " + p.getId() + " to " + webshopApiUrl);
                String json = String.format(Locale.US,
                    "{\"id\":%d,\"type\":\"%s\",\"name\":\"%s\",\"description\":\"%s\",\"price\":%.2f,\"unit\":\"%s\"}",
                    p.getId(), p.getType(), p.getName(), p.getDescription(), p.getPrice(), p.getUnit()
                );
                sendRequest(webshopApiUrl, json, token);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void syncToWarehouse(Product p) {
        tokenService.getAccessToken().thenAccept(token -> {
            try {
                System.out.println("Syncing product " + p.getId() + " to " + warehouseApiUrl);
                // Warehouse only needs ID and Name
                String json = String.format(Locale.US,
                    "{\"id\":%d,\"name\":\"%s\"}",
                    p.getId(), p.getName()
                );
                sendRequest(warehouseApiUrl, json, token);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendRequest(String url, String json, String token) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        System.err.println("Failed to sync to " + url + ". Status: " + response.statusCode() + " Body: " + response.body());
                    } else {
                        System.out.println("Synced to " + url + ". Response: " + response.body());
                    }
                })
                .exceptionally(e -> {
                    System.err.println("Exception during sync to " + url + ": " + e.getMessage());
                    throw new RuntimeException("Exception during sync to " + url, e);
                });
    }

    private void syncDeleteToWebshop(int id) {
        tokenService.getAccessToken().thenAccept(token -> {
            sendDeleteRequest(webshopApiUrl, id, token);
        });
    }

    private void syncDeleteToWarehouse(int id) {
        tokenService.getAccessToken().thenAccept(token -> {
            sendDeleteRequest(warehouseApiUrl, id, token);
        });
    }

    private void sendDeleteRequest(String url, int id, String token) {
        try {
            System.out.println("Syncing delete product " + id + " to " + url);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "?id=" + id))
                    .header("Authorization", "Bearer " + token)
                    .DELETE()
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != 200) {
                            System.err.println("Failed to sync delete to " + url + ". Status: " + response.statusCode() + " Body: " + response.body());
                        } else {
                            System.out.println("Synced delete product " + id + " to " + url + ". Response: " + response.body());
                        }
                    })
                    .exceptionally(e -> {
                        System.err.println("Exception during delete sync to " + url + ": " + e.getMessage());
                        throw new RuntimeException("Exception during delete sync to " + url, e);
                    });
        } catch (Exception e) {
            System.err.println("Error preparing delete sync request to " + url + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
