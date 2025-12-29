package com.example.warehouse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class OrderUpdateService {

    private final HttpClient httpClient;
    private final String orderServiceUrl;
    private final TokenService tokenService;

    public OrderUpdateService() {
        this(
            System.getenv().getOrDefault("ORDER_SERVICE_URL", "http://order-service:8003/api/orders/status"),
            new KeycloakTokenService(
                System.getenv().getOrDefault("TOKEN_URL", "http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/token"),
                System.getenv().getOrDefault("CLIENT_ID", "warehouse-client"),
                System.getenv().getOrDefault("CLIENT_SECRET", "warehouse-secret")
            )
        );
    }

    public OrderUpdateService(String orderServiceUrl, TokenService tokenService) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.orderServiceUrl = orderServiceUrl;
        this.tokenService = tokenService;
    }

    public void updateStatus(int orderId, String status) {
        tokenService.getAccessToken().thenAccept(token -> {
            try {
                System.out.println("Updating status for order " + orderId + " to " + status);
                String json = String.format(Locale.US, "{\"orderId\":%d,\"status\":\"%s\"}", orderId, status);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(orderServiceUrl))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + token)
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            if (response.statusCode() != 200) {
                                System.err.println("Failed to update order status. Status: " + response.statusCode() + " Body: " + response.body());
                            } else {
                                System.out.println("Updated order status for " + orderId + " to " + status);
                            }
                        })
                        .exceptionally(e -> {
                            System.err.println("Exception during order status update: " + e.getMessage());
                            throw new RuntimeException("Exception during order status update", e);
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
