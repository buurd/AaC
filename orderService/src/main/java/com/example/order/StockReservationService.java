package com.example.order;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class StockReservationService {

    private final HttpClient httpClient;
    private final String warehouseReserveUrl;
    private final String keycloakTokenUrl;
    private final String clientId;
    private final String clientSecret;

    private String accessToken;
    private long tokenExpiresAt;

    public StockReservationService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.warehouseReserveUrl = System.getenv().getOrDefault("WAREHOUSE_RESERVE_URL", "http://warehouse-demo:8002/api/stock/reserve");
        this.keycloakTokenUrl = System.getenv().getOrDefault("TOKEN_URL", "http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/token");
        this.clientId = System.getenv().getOrDefault("CLIENT_ID", "order-client");
        this.clientSecret = System.getenv().getOrDefault("CLIENT_SECRET", "order-secret");
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
                        throw new RuntimeException("Failed to get access token");
                    }
                });
    }

    public CompletableFuture<Boolean> reserveStock(int productId, int quantity) {
        return getAccessToken().thenCompose(token -> {
            String json = "{\"productId\":" + productId + ",\"quantity\":" + quantity + "}";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(warehouseReserveUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> response.statusCode() == 200);
        });
    }
}
