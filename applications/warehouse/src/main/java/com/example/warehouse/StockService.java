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
    private final TokenService tokenService;

    public StockService() {
        this(
            System.getenv().getOrDefault("WEBSHOP_STOCK_API_URL", "http://localhost:8000/api/stock/sync"),
            new KeycloakTokenService(
                System.getenv().getOrDefault("TOKEN_URL", "http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/token"),
                System.getenv().getOrDefault("CLIENT_ID", "warehouse-client"),
                System.getenv().getOrDefault("CLIENT_SECRET", "warehouse-secret")
            )
        );
    }

    public StockService(String webshopStockApiUrl, TokenService tokenService) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.webshopStockApiUrl = webshopStockApiUrl;
        this.tokenService = tokenService;
        
        System.out.println("StockService initialized with Webshop Stock API URL: " + this.webshopStockApiUrl);
    }

    public void syncStock(int pmId, int stock) {
        tokenService.getAccessToken().thenAccept(token -> {
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
