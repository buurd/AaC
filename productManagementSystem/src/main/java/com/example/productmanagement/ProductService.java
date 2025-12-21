package com.example.productmanagement;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class ProductService {

    private final ProductRepository repository;
    private final HttpClient httpClient;
    private final String webshopApiUrl;
    private final String warehouseApiUrl;
    private final String keycloakTokenUrl;
    private final String clientId;
    private final String clientSecret;

    private String accessToken;
    private long tokenExpiresAt;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.webshopApiUrl = System.getenv().getOrDefault("WEBSHOP_API_URL", "http://webshop-demo:8000/api/products/sync");
        this.warehouseApiUrl = System.getenv().getOrDefault("WAREHOUSE_API_URL", "http://warehouse-demo:8002/api/products/sync");
        this.keycloakTokenUrl = System.getenv().getOrDefault("TOKEN_URL", "http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/token");
        this.clientId = System.getenv().getOrDefault("CLIENT_ID", "pm-client");
        this.clientSecret = System.getenv().getOrDefault("CLIENT_SECRET", "pm-secret");

        System.out.println("ProductService initialized with Webshop API URL: " + this.webshopApiUrl);
        System.out.println("ProductService initialized with Warehouse API URL: " + this.warehouseApiUrl);
        System.out.println("ProductService initialized with Keycloak Token URL: " + this.keycloakTokenUrl);
    }

    public void createProduct(Product product) throws SQLException {
        // 1. Save to local DB
        int newId = repository.create(product);
        product.setId(newId);

        // 2. Sync to Webshop and Warehouse
        syncToWebshop(product);
        syncToWarehouse(product);
    }

    public void updateProduct(Product product) throws SQLException {
        // 1. Update local DB
        repository.update(product);

        // 2. Sync to Webshop and Warehouse
        syncToWebshop(product);
        syncToWarehouse(product);
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

    private CompletableFuture<String> getAccessToken() {
        if (accessToken != null && tokenExpiresAt > System.currentTimeMillis() + 5000) { // Refresh 5s before expiry
            return CompletableFuture.completedFuture(accessToken);
        }

        String requestBody = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(keycloakTokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        String json = response.body();
                        // Basic JSON parsing for access_token and expires_in
                        int tokenStart = json.indexOf("\"access_token\":\"");
                        int tokenEnd = json.indexOf("\"", tokenStart + 16);
                        String token = json.substring(tokenStart + 16, tokenEnd);

                        int expiresStart = json.indexOf("\"expires_in\":");
                        int expiresEnd = json.indexOf(",", expiresStart);
                        long expiresIn = Long.parseLong(json.substring(expiresStart + 13, expiresEnd));

                        this.accessToken = token;
                        this.tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000);
                        return token;
                    } else {
                        System.err.println("Failed to get access token. Status: " + response.statusCode() + " Body: " + response.body());
                        throw new RuntimeException("Failed to get access token");
                    }
                })
                .exceptionally(e -> {
                    System.err.println("Exception getting access token: " + e.getMessage());
                    throw new RuntimeException("Exception getting access token", e);
                });
    }

    private void syncToWebshop(Product p) {
        getAccessToken().thenAccept(token -> {
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
        getAccessToken().thenAccept(token -> {
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
        getAccessToken().thenAccept(token -> {
            sendDeleteRequest(webshopApiUrl, id, token);
        });
    }

    private void syncDeleteToWarehouse(int id) {
        getAccessToken().thenAccept(token -> {
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
