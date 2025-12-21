package com.example.warehouse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class StockService {

    private final HttpClient httpClient;
    private final String webshopStockApiUrl;
    private final String keycloakTokenUrl;
    private final String clientId;
    private final String clientSecret;

    private String accessToken;
    private long tokenExpiresAt;

    public StockService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.webshopStockApiUrl = System.getenv().getOrDefault("WEBSHOP_STOCK_API_URL", "http://localhost:8000/api/stock/sync");
        this.keycloakTokenUrl = System.getenv().getOrDefault("TOKEN_URL", "http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/token");
        this.clientId = System.getenv().getOrDefault("CLIENT_ID", "warehouse-client");
        this.clientSecret = System.getenv().getOrDefault("CLIENT_SECRET", "warehouse-secret");
        
        System.out.println("StockService initialized with Webshop Stock API URL: " + this.webshopStockApiUrl);
        System.out.println("StockService initialized with Keycloak Token URL: " + this.keycloakTokenUrl);
    }

    private CompletableFuture<String> getAccessToken() {
        if (accessToken != null && tokenExpiresAt > System.currentTimeMillis() + 5000) {
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

    public void syncStock(int pmId, int stock) {
        getAccessToken().thenAccept(token -> {
            try {
                System.out.println("Syncing stock for product " + pmId + " (Stock: " + stock + ") to " + webshopStockApiUrl);
                String json = String.format(Locale.US, "{\"pmId\":%d,\"stock\":%d}", pmId, stock);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(webshopStockApiUrl))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + token)
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            if (response.statusCode() != 200) {
                                System.err.println("Failed to sync stock to Webshop. Status: " + response.statusCode() + " Body: " + response.body());
                            } else {
                                System.out.println("Synced stock for product " + pmId + " to Webshop.");
                            }
                        })
                        .exceptionally(e -> {
                            System.err.println("Exception during stock sync: " + e.getMessage());
                            throw new RuntimeException("Exception during stock sync", e);
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
