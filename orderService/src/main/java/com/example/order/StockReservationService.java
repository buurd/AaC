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
    private final TokenService tokenService;

    public StockReservationService() {
        this(
            System.getenv().getOrDefault("WAREHOUSE_RESERVE_URL", "http://warehouse-demo:8002/api/stock/reserve"),
            new KeycloakTokenService(
                System.getenv().getOrDefault("TOKEN_URL", "http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/token"),
                System.getenv().getOrDefault("CLIENT_ID", "order-client"),
                System.getenv().getOrDefault("CLIENT_SECRET", "order-secret")
            )
        );
    }

    public StockReservationService(String warehouseReserveUrl, TokenService tokenService) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.warehouseReserveUrl = warehouseReserveUrl;
        this.tokenService = tokenService;
    }

    public CompletableFuture<Boolean> reserveStock(int productId, int quantity) {
        return tokenService.getAccessToken().thenCompose(token -> {
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
