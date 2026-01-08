package com.example.warehouse;

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
        if (accessToken != null && tokenExpiresAt > System.currentTimeMillis() + 5000) { // Refresh 5s before expiry
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
                        // Basic JSON parsing for access_token and expires_in
                        int tokenStart = json.indexOf("\"access_token\":\"");
                        if (tokenStart == -1) {
                             throw new RuntimeException("Invalid response: access_token not found");
                        }
                        int tokenEnd = json.indexOf("\"", tokenStart + 16);
                        String token = json.substring(tokenStart + 16, tokenEnd);

                        int expiresStart = json.indexOf("\"expires_in\":");
                        if (expiresStart == -1) {
                             // Default expiry if not found or handle error
                             // For now let's assume it's there or default to short time
                             expiresStart = -1; 
                        }
                        
                        long expiresIn = 300; // Default 5 minutes
                        if (expiresStart != -1) {
                            int expiresEnd = json.indexOf(",", expiresStart);
                            if (expiresEnd == -1) expiresEnd = json.indexOf("}", expiresStart); // Could be last element
                            expiresIn = Long.parseLong(json.substring(expiresStart + 13, expiresEnd).trim());
                        }

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
}
