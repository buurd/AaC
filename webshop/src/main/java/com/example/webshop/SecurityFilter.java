package com.example.webshop;

import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SecurityFilter extends Filter {

    private final String requiredRole;
    private final JwkProvider jwkProvider;
    private final String issuer;

    public SecurityFilter(String jwksUrl, String issuer, String requiredRole) {
        this.requiredRole = requiredRole;
        this.issuer = issuer;
        try {
            this.jwkProvider = new JwkProviderBuilder(new URL(jwksUrl))
                    .cached(10, 24, TimeUnit.HOURS)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JWK Provider", e);
        }
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(exchange, 401, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        try {
            DecodedJWT jwt = JWT.decode(token);
            RSAPublicKey publicKey = (RSAPublicKey) jwkProvider.get(jwt.getKeyId()).getPublicKey();
            Algorithm algorithm = Algorithm.RSA256(publicKey, null);
            
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build();
            
            verifier.verify(token);

            // Check Role (Client Role in resource_access)
            // Structure: resource_access -> webshop-client -> roles -> [product-sync]
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access").asMap();
            if (resourceAccess != null) {
                Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get("webshop-client");
                if (clientAccess != null) {
                    List<String> roles = (List<String>) clientAccess.get("roles");
                    if (roles != null && roles.contains(requiredRole)) {
                        chain.doFilter(exchange);
                        return;
                    }
                }
            }
            
            // Also check Realm Roles just in case (though we mapped to client roles)
            Map<String, Object> realmAccess = jwt.getClaim("realm_access").asMap();
            if (realmAccess != null) {
                List<String> roles = (List<String>) realmAccess.get("roles");
                if (roles != null && roles.contains(requiredRole)) {
                    chain.doFilter(exchange);
                    return;
                }
            }
            
            sendError(exchange, 403, "Insufficient permissions");
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 401, "Invalid token: " + e.getMessage());
        }
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        exchange.sendResponseHeaders(code, message.length());
        exchange.getResponseBody().write(message.getBytes());
        exchange.close();
    }

    @Override
    public String description() {
        return "JWT Security Filter";
    }
}
