package com.example.warehouse;

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
        String token = getToken(exchange);
        
        if (token == null) {
            System.out.println("SecurityFilter: No token found for " + exchange.getRequestMethod() + " " + exchange.getRequestURI());
            System.out.println("Headers: ");
            exchange.getRequestHeaders().forEach((k, v) -> System.out.println(k + ": " + v));
            
            if (exchange.getRequestHeaders().containsKey("Cookie")) {
                System.out.println("Cookies: " + exchange.getRequestHeaders().getFirst("Cookie"));
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Location", "/login");
                exchange.sendResponseHeaders(302, -1);
                return;
            }
            sendError(exchange, 401, "Missing or invalid Authorization header/cookie");
            return;
        }

        try {
            DecodedJWT jwt = JWT.decode(token);
            RSAPublicKey publicKey = (RSAPublicKey) jwkProvider.get(jwt.getKeyId()).getPublicKey();
            Algorithm algorithm = Algorithm.RSA256(publicKey, null);
            
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build();
            
            verifier.verify(token);

            // Check Role
            Map<String, Object> realmAccess = jwt.getClaim("realm_access").asMap();
            if (realmAccess != null) {
                List<String> roles = (List<String>) realmAccess.get("roles");
                if (roles != null && roles.contains(requiredRole)) {
                    chain.doFilter(exchange);
                    return;
                }
            }
            
            System.out.println("SecurityFilter: Insufficient permissions. Roles: " + (realmAccess != null ? realmAccess.get("roles") : "null"));
            sendError(exchange, 403, "Insufficient permissions");
        } catch (Exception e) {
            e.printStackTrace();
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Location", "/login");
                exchange.sendResponseHeaders(302, -1);
                return;
            }
            sendError(exchange, 401, "Invalid token: " + e.getMessage());
        }
    }

    private String getToken(HttpExchange exchange) {
        // Check Authorization header (case-insensitive)
        String authHeader = null;
        for (Map.Entry<String, List<String>> entry : exchange.getRequestHeaders().entrySet()) {
            if ("Authorization".equalsIgnoreCase(entry.getKey()) && !entry.getValue().isEmpty()) {
                authHeader = entry.getValue().get(0);
                break;
            }
        }

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                String[] parts = cookie.trim().split("=");
                if (parts.length == 2 && "warehouse_auth_token".equals(parts[0])) {
                    return parts[1];
                }
            }
        }
        return null;
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
