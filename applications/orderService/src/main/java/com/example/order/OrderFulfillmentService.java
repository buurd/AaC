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
        System.out.println("OrderFulfillmentService: Notifying fulfillment for order " + orderId + " at " + warehouseFulfillmentUrl);
        return tokenService.getAccessToken().thenCompose(token -> {
            System.out.println("OrderFulfillmentService: Got token (length: " + (token != null ? token.length() : 0) + ")");
            String json = "{\"orderId\":" + orderId + "}";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(warehouseFulfillmentUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        System.out.println("OrderFulfillmentService: Response status: " + response.statusCode());
                        if (response.statusCode() != 200) {
                            System.out.println("OrderFulfillmentService: Response body: " + response.body());
                            return false;
                        }
                        return true;
                    });
        });
    }
}
