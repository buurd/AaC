package com.example.loyalty;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.interfaces.Claim;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;

public class SecurityFilter extends Filter {
    private final String jwksUrl;
    private final String issuer;
    private final String requiredRole;

    public SecurityFilter(String jwksUrl, String issuer, String requiredRole) {
        this.jwksUrl = jwksUrl;
        this.issuer = issuer;
        this.requiredRole = requiredRole;
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        String token = getToken(exchange);

        if (token == null) {
            // If it's a browser request (Accept: text/html), redirect to login
            String accept = exchange.getRequestHeaders().getFirst("Accept");
            if (accept != null && accept.contains("text/html")) {
                exchange.getResponseHeaders().set("Location", "/login");
                exchange.sendResponseHeaders(302, -1);
                return;
            }
            
            sendError(exchange, 401, "Unauthorized: No token provided");
            return;
        }

        try {
            JwkProvider provider = new JwkProviderBuilder(new URL(jwksUrl)).build();
            DecodedJWT jwt = JWT.decode(token);
            RSAPublicKey publicKey = (RSAPublicKey) provider.get(jwt.getKeyId()).getPublicKey();
            Algorithm algorithm = Algorithm.RSA256(publicKey, null);
            JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build();
            
            verifier.verify(jwt);

            // Role check
            Claim realmAccess = jwt.getClaim("realm_access");
            Map<String, Object> realmAccessMap = realmAccess.asMap();
            List<String> roles = (List<String>) realmAccessMap.get("roles");

            if (roles == null || !roles.contains(requiredRole)) {
                sendError(exchange, 403, "Forbidden: Insufficient role");
                return;
            }

            chain.doFilter(exchange);
        } catch (Exception e) {
            e.printStackTrace();
            // If token is invalid and it's a browser request, redirect to login
            String accept = exchange.getRequestHeaders().getFirst("Accept");
            if (accept != null && accept.contains("text/html")) {
                exchange.getResponseHeaders().set("Location", "/login");
                exchange.sendResponseHeaders(302, -1);
                return;
            }
            sendError(exchange, 401, "Unauthorized: Invalid token");
        }
    }

    private String getToken(HttpExchange exchange) {
        // 1. Check Authorization header
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // 2. Check Cookie
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                String[] parts = cookie.trim().split("=");
                if (parts.length == 2 && "auth_token".equals(parts[0])) {
                    return parts[1];
                }
            }
        }

        return null;
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Override
    public String description() {
        return "JWT Security Filter";
    }
}
