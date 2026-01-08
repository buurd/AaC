package com.example.webshop;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class KeycloakTokenService implements TokenService {

    private final HttpClient httpClient;
    private final String keycloakTokenUrl;
    private final String clientId;
    private final String clientSecret;

    private String accessToken;
    private long tokenExpiresAt;

    public KeycloakTokenService(String keycloakTokenUrl, String clientId, String clientSecret) {
        this(keycloakTokenUrl, clientId, clientSecret, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    // Added constructor for testing
    public KeycloakTokenService(String keycloakTokenUrl, String clientId, String clientSecret, HttpClient httpClient) {
        this.keycloakTokenUrl = keycloakTokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.httpClient = httpClient;
    }

    @Override
    public CompletableFuture<String> getAccessToken() {
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
                        if (tokenStart == -1) throw new RuntimeException("Invalid response");
                        int tokenEnd = json.indexOf("\"", tokenStart + 16);
                        String token = json.substring(tokenStart + 16, tokenEnd);

                        int expiresStart = json.indexOf("\"expires_in\":");
                        long expiresIn = 300;
                        if (expiresStart != -1) {
                            int expiresEnd = json.indexOf(",", expiresStart);
                            if (expiresEnd == -1) expiresEnd = json.indexOf("}", expiresStart);
                            expiresIn = Long.parseLong(json.substring(expiresStart + 13, expiresEnd).trim());
                        }

                        this.accessToken = token;
                        this.tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000);
                        return token;
                    } else {
                        throw new RuntimeException("Failed to get access token");
                    }
                });
    }
}
