package com.example.order;

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
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SecurityFilterTest {

    @Mock
    private JwkProvider jwkProvider;

    @Mock
    private HttpExchange exchange;

    @Mock
    private Filter.Chain chain;

    private SecurityFilter filter;
    private Headers requestHeaders;
    private Headers responseHeaders;
    private ByteArrayOutputStream responseBody;

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private final String ISSUER = "http://issuer";
    private final String ROLE = "user";
    private final String KEY_ID = "key1";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Generate Keys
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        publicKey = (RSAPublicKey) kp.getPublic();
        privateKey = (RSAPrivateKey) kp.getPrivate();

        // Mock JwkProvider
        Jwk jwk = mock(Jwk.class);
        when(jwk.getPublicKey()).thenReturn(publicKey);
        when(jwkProvider.get(anyString())).thenReturn(jwk);

        filter = new SecurityFilter(jwkProvider, ISSUER, ROLE);
        requestHeaders = new Headers();
        responseHeaders = new Headers();
        responseBody = new ByteArrayOutputStream();

        when(exchange.getRequestHeaders()).thenReturn(requestHeaders);
        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        when(exchange.getResponseBody()).thenReturn(responseBody);
    }

    @Test
    void testDoFilter_NoToken_GetRequest_Redirects() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        
        filter.doFilter(exchange, chain);

        verify(exchange).sendResponseHeaders(eq(302), anyLong());
        verify(chain, never()).doFilter(exchange);
    }

    @Test
    void testDoFilter_NoToken_PostRequest_Returns401() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("POST");

        filter.doFilter(exchange, chain);

        verify(exchange).sendResponseHeaders(eq(401), anyLong());
        verify(chain, never()).doFilter(exchange);
    }

    @Test
    void testDoFilter_InvalidTokenFormat() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        requestHeaders.add("Authorization", "Bearer invalid-token");

        filter.doFilter(exchange, chain);

        verify(exchange).sendResponseHeaders(eq(401), anyLong());
        verify(chain, never()).doFilter(exchange);
    }

    @Test
    void testDoFilter_ValidToken_Authorized() throws IOException {
        String token = JWT.create()
                .withIssuer(ISSUER)
                .withKeyId(KEY_ID)
                .withClaim("realm_access", Map.of("roles", Collections.singletonList(ROLE)))
                .sign(Algorithm.RSA256(null, privateKey));

        when(exchange.getRequestMethod()).thenReturn("GET");
        requestHeaders.add("Authorization", "Bearer " + token);

        filter.doFilter(exchange, chain);

        verify(chain).doFilter(exchange);
    }

    @Test
    void testDoFilter_ValidToken_WrongRole_Returns403() throws IOException {
        String token = JWT.create()
                .withIssuer(ISSUER)
                .withKeyId(KEY_ID)
                .withClaim("realm_access", Map.of("roles", Collections.singletonList("wrong-role")))
                .sign(Algorithm.RSA256(null, privateKey));

        when(exchange.getRequestMethod()).thenReturn("GET");
        requestHeaders.add("Authorization", "Bearer " + token);

        filter.doFilter(exchange, chain);

        verify(exchange).sendResponseHeaders(eq(403), anyLong());
        verify(chain, never()).doFilter(exchange);
    }

    @Test
    void testDoFilter_TokenFromCookie_Authorized() throws IOException {
        String token = JWT.create()
                .withIssuer(ISSUER)
                .withKeyId(KEY_ID)
                .withClaim("realm_access", Map.of("roles", Collections.singletonList(ROLE)))
                .sign(Algorithm.RSA256(null, privateKey));

        when(exchange.getRequestMethod()).thenReturn("GET");
        requestHeaders.add("Cookie", "auth_token=" + token);

        filter.doFilter(exchange, chain);

        verify(chain).doFilter(exchange);
    }
}
