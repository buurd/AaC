package com.example.order;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoyaltyIntegrationService {

    private final HttpClient httpClient;
    private final String loyaltyAccrueUrl;
    private final String loyaltyRedeemUrl;
    private final TokenService tokenService;

    public LoyaltyIntegrationService() {
        this(
            System.getenv().getOrDefault("LOYALTY_ACCRUE_URL", "http://loyalty-service:8084/api/loyalty/accrue"),
            System.getenv().getOrDefault("LOYALTY_REDEEM_URL", "http://loyalty-service:8084/api/loyalty/redeem"),
            new KeycloakTokenService(
                System.getenv().getOrDefault("TOKEN_URL", "http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/token"),
                System.getenv().getOrDefault("CLIENT_ID", "order-client"),
                System.getenv().getOrDefault("CLIENT_SECRET", "order-secret")
            )
        );
    }

    public LoyaltyIntegrationService(String loyaltyAccrueUrl, String loyaltyRedeemUrl, TokenService tokenService) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.loyaltyAccrueUrl = loyaltyAccrueUrl;
        this.loyaltyRedeemUrl = loyaltyRedeemUrl;
        this.tokenService = tokenService;
    }

    public CompletableFuture<Integer> accruePoints(String customerId, int orderId, double totalAmount, String itemsJson) {
        System.out.println("LoyaltyIntegrationService: Accruing points for order " + orderId);
        return tokenService.getAccessToken().thenCompose(token -> {
            String json = String.format("{\"customerId\":\"%s\",\"orderId\":\"%d\",\"totalAmount\":%.2f,\"currency\":\"EUR\",\"items\":%s}",
                customerId, orderId, totalAmount, itemsJson);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(loyaltyAccrueUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            String body = response.body();
                            Pattern p = Pattern.compile("\"pointsAccrued\":(\\d+)");
                            Matcher m = p.matcher(body);
                            if (m.find()) {
                                return Integer.parseInt(m.group(1));
                            }
                        } else {
                            System.err.println("Failed to accrue points for order " + orderId + ". Status: " + response.statusCode() + ", Body: " + response.body());
                        }
                        return 0;
                    });
        });
    }

    public CompletableFuture<Boolean> redeemPoints(String customerId, int orderId, int pointsToRedeem) {
        System.out.println("LoyaltyIntegrationService: Redeeming " + pointsToRedeem + " points for order " + orderId);
        return tokenService.getAccessToken().thenCompose(token -> {
            String json = String.format("{\"customerId\":\"%s\",\"orderId\":\"%d\",\"pointsToRedeem\":%d}",
                customerId, orderId, pointsToRedeem);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(loyaltyRedeemUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            return true;
                        } else {
                            System.err.println("Failed to redeem points for order " + orderId + ". Status: " + response.statusCode() + ", Body: " + response.body());
                            return false;
                        }
                    });
        });
    }
}
