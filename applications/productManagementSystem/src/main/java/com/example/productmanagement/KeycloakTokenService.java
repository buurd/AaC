package com.example.productmanagement;

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
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.keycloakTokenUrl = keycloakTokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
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
                             throw new RuntimeException("Invalid JSON response: missing access_token");
                        }
                        int tokenEnd = json.indexOf("\"", tokenStart + 16);
                        String token = json.substring(tokenStart + 16, tokenEnd);

                        int expiresStart = json.indexOf("\"expires_in\":");
                        if (expiresStart == -1) {
                             // Default expiry if not found
                             this.accessToken = token;
                             this.tokenExpiresAt = System.currentTimeMillis() + (300 * 1000);
                             return token;
                        }
                        int expiresEnd = json.indexOf(",", expiresStart);
                        if (expiresEnd == -1) {
                            // Try finding closing brace if it's the last element
                            expiresEnd = json.indexOf("}", expiresStart);
                        }
                        
                        long expiresIn = 300; // Default
                        try {
                            expiresIn = Long.parseLong(json.substring(expiresStart + 13, expiresEnd).trim());
                        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                            System.err.println("Error parsing expires_in: " + e.getMessage());
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
