package com.example.order;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        System.out.println("KeycloakTokenService: Requesting new token for client " + clientId + " from " + keycloakTokenUrl);
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
                        
                        String token = null;
                        long expiresIn = 300; // Default

                        Pattern pToken = Pattern.compile("\"access_token\":\"([^\"]+)\"");
                        Matcher mToken = pToken.matcher(json);
                        if (mToken.find()) {
                            token = mToken.group(1);
                        }

                        Pattern pExpires = Pattern.compile("\"expires_in\":(\\d+)");
                        Matcher mExpires = pExpires.matcher(json);
                        if (mExpires.find()) {
                            expiresIn = Long.parseLong(mExpires.group(1));
                        }

                        if (token != null) {
                            this.accessToken = token;
                            this.tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000);
                            System.out.println("KeycloakTokenService: Token obtained successfully. Length: " + token.length());
                            return token;
                        } else {
                             throw new RuntimeException("Failed to parse access token");
                        }
                    } else {
                        System.err.println("KeycloakTokenService: Failed to get token. Status: " + response.statusCode() + ", Body: " + response.body());
                        throw new RuntimeException("Failed to get access token");
                    }
                });
    }
}
