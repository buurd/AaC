package com.example.webshop;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class OrderService {

    private final HttpClient httpClient;
    private final String orderServiceUrl;
    private final TokenService tokenService;

    public OrderService() {
        this(
            System.getenv().getOrDefault("ORDER_SERVICE_URL", "http://order-service:8003/orders"),
            new KeycloakTokenService(
                System.getenv().getOrDefault("TOKEN_URL", "http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/token"),
                System.getenv().getOrDefault("CLIENT_ID", "webshop-client"),
                System.getenv().getOrDefault("CLIENT_SECRET", "webshop-secret")
            )
        );
    }

    public OrderService(String orderServiceUrl, TokenService tokenService) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.orderServiceUrl = orderServiceUrl;
        this.tokenService = tokenService;
    }

    public CompletableFuture<String> getOrdersForCustomer(String customerName) {
        return tokenService.getAccessToken().thenCompose(token -> {
            String url = orderServiceUrl + "?customer=" + customerName.replace(" ", "%20");
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body);
        });
    }
}
