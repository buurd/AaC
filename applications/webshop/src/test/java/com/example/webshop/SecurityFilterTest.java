package com.example.webshop;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SecurityFilterTest {

    private SecurityFilter securityFilter;
    
    @Mock
    private JwkProvider jwkProvider;
    
    @Mock
    private HttpExchange exchange;
    
    @Mock
    private Filter.Chain chain;
    
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private String issuer = "test-issuer";
    private String requiredRole = "test-role";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        // Generate RSA keys
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        publicKey = (RSAPublicKey) kp.getPublic();
        privateKey = (RSAPrivateKey) kp.getPrivate();
        
        Jwk jwk = mock(Jwk.class);
        when(jwk.getPublicKey()).thenReturn(publicKey);
        when(jwkProvider.get(anyString())).thenReturn(jwk);
        
        securityFilter = new SecurityFilter(jwkProvider, issuer, requiredRole);
        
        when(exchange.getRequestHeaders()).thenReturn(new Headers());
        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());
    }

    @Test
    void doFilter_validToken_callsChain() throws IOException {
        // Arrange
        String token = createToken(requiredRole);
        exchange.getRequestHeaders().add("Authorization", "Bearer " + token);

        // Act
        securityFilter.doFilter(exchange, chain);

        // Assert
        verify(chain).doFilter(exchange);
    }

    @Test
    void doFilter_validRealmRole_callsChain() throws IOException {
        // Arrange
        String token = createTokenWithRealmRole(requiredRole);
        exchange.getRequestHeaders().add("Authorization", "Bearer " + token);

        // Act
        securityFilter.doFilter(exchange, chain);

        // Assert
        verify(chain).doFilter(exchange);
    }

    @Test
    void doFilter_missingToken_returns401() throws IOException {
        // Act
        securityFilter.doFilter(exchange, chain);

        // Assert
        verify(exchange).sendResponseHeaders(eq(401), anyLong());
        verify(chain, never()).doFilter(exchange);
    }

    @Test
    void doFilter_invalidRole_returns403() throws IOException {
        // Arrange
        String token = createToken("wrong-role");
        exchange.getRequestHeaders().add("Authorization", "Bearer " + token);

        // Act
        securityFilter.doFilter(exchange, chain);

        // Assert
        verify(exchange).sendResponseHeaders(eq(403), anyLong());
        verify(chain, never()).doFilter(exchange);
    }

    @Test
    void doFilter_invalidToken_returns401() throws IOException {
        // Arrange
        exchange.getRequestHeaders().add("Authorization", "Bearer invalid-token");

        // Act
        securityFilter.doFilter(exchange, chain);

        // Assert
        verify(exchange).sendResponseHeaders(eq(401), anyLong());
        verify(chain, never()).doFilter(exchange);
    }

    private String createToken(String role) {
        Map<String, Object> clientAccess = new HashMap<>();
        clientAccess.put("roles", Collections.singletonList(role));
        
        Map<String, Object> resourceAccess = new HashMap<>();
        resourceAccess.put("webshop-client", clientAccess);
        
        return JWT.create()
                .withIssuer(issuer)
                .withKeyId("key-id")
                .withClaim("resource_access", resourceAccess)
                .sign(Algorithm.RSA256(null, privateKey));
    }

    private String createTokenWithRealmRole(String role) {
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", Collections.singletonList(role));
        
        return JWT.create()
                .withIssuer(issuer)
                .withKeyId("key-id")
                .withClaim("realm_access", realmAccess)
                .sign(Algorithm.RSA256(null, privateKey));
    }
}
