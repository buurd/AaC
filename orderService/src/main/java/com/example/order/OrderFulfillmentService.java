package com.example.order;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class OrderFulfillmentService {

    private final HttpClient httpClient;
    private final String warehouseFulfillmentUrl;
    private final TokenService tokenService;

    public OrderFulfillmentService() {
        this(
            System.getenv().getOrDefault("WAREHOUSE_FULFILLMENT_URL", "http://warehouse-demo:8002/api/fulfillment/order"),
            new KeycloakTokenService(
                System.getenv().getOrDefault("TOKEN_URL", "http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/token"),
                System.getenv().getOrDefault("CLIENT_ID", "order-client"),
                System.getenv().getOrDefault("CLIENT_SECRET", "order-secret")
            )
        );
    }

    public OrderFulfillmentService(String warehouseFulfillmentUrl, TokenService tokenService) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.warehouseFulfillmentUrl = warehouseFulfillmentUrl;
        this.tokenService = tokenService;
    }

    public CompletableFuture<Boolean> notifyOrderConfirmed(int orderId) {
        return tokenService.getAccessToken().thenCompose(token -> {
            String json = "{\"orderId\":" + orderId + "}";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(warehouseFulfillmentUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) {
                            System.err.println("Failed to notify warehouse. Status: " + response.statusCode());
                            return false;
                        }
                        return true;
                    });
        });
    }
}
