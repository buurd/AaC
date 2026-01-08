package com.example.loyalty;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.auth0.jwk.JwkProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityFilterTest {

    @Mock
    private HttpExchange exchange;

    @Mock
    private Filter.Chain chain;

    @Mock
    private DecodedJWT mockJwt;

    @Mock
    private Claim mockClaim;

    private Headers requestHeaders;
    private Headers responseHeaders;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        requestHeaders = new Headers();
        responseHeaders = new Headers();
        responseBody = new ByteArrayOutputStream();

        when(exchange.getRequestHeaders()).thenReturn(requestHeaders);
        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        when(exchange.getResponseBody()).thenReturn(responseBody);
    }

    private SecurityFilter createFilter(DecodedJWT jwtToReturn, Exception toThrow) {
        return new SecurityFilter((JwkProvider) null, "http://issuer", "admin") {
            @Override
            protected DecodedJWT verifyToken(String token) throws Exception {
                if (toThrow != null) {
                    throw toThrow;
                }
                return jwtToReturn;
            }
        };
    }

    @Test
    void testNoToken_ApiRequest() throws IOException {
        SecurityFilter securityFilter = createFilter(null, null);
        securityFilter.doFilter(exchange, chain);

        verify(exchange).sendResponseHeaders(eq(401), anyLong());
        assertTrue(responseBody.toString().contains("Unauthorized: No token provided"));
        verify(chain, never()).doFilter(any());
    }

    @Test
    void testNoToken_BrowserRequest() throws IOException {
        SecurityFilter securityFilter = createFilter(null, null);
        requestHeaders.add("Accept", "text/html");
        
        securityFilter.doFilter(exchange, chain);

        verify(exchange).sendResponseHeaders(eq(302), eq(-1L));
        assertEquals("/login", responseHeaders.getFirst("Location"));
    }

    @Test
    void testInvalidToken_ApiRequest() throws IOException {
        SecurityFilter securityFilter = createFilter(null, new Exception("Invalid signature"));
        requestHeaders.add("Authorization", "Bearer invalid.token");
        
        securityFilter.doFilter(exchange, chain);

        verify(exchange).sendResponseHeaders(eq(401), anyLong());
        assertTrue(responseBody.toString().contains("Unauthorized: Invalid token"));
    }

    @Test
    void testInvalidToken_BrowserRequest() throws IOException {
        SecurityFilter securityFilter = createFilter(null, new Exception("Invalid signature"));
        requestHeaders.add("Authorization", "Bearer invalid.token");
        requestHeaders.add("Accept", "text/html");

        securityFilter.doFilter(exchange, chain);

        verify(exchange).sendResponseHeaders(eq(302), eq(-1L));
        assertEquals("/login", responseHeaders.getFirst("Location"));
    }

    @Test
    void testValidToken_CorrectRole() throws IOException {
        when(mockJwt.getClaim("realm_access")).thenReturn(mockClaim);
        when(mockClaim.asMap()).thenReturn(Map.of("roles", List.of("admin", "user")));
        
        SecurityFilter securityFilter = createFilter(mockJwt, null);
        requestHeaders.add("Authorization", "Bearer valid.token");

        securityFilter.doFilter(exchange, chain);

        verify(chain).doFilter(exchange);
        verify(exchange, never()).sendResponseHeaders(anyInt(), anyLong());
    }

    @Test
    void testValidToken_IncorrectRole() throws IOException {
        when(mockJwt.getClaim("realm_access")).thenReturn(mockClaim);
        when(mockClaim.asMap()).thenReturn(Map.of("roles", List.of("user")));

        SecurityFilter securityFilter = createFilter(mockJwt, null);
        requestHeaders.add("Authorization", "Bearer valid.token");

        securityFilter.doFilter(exchange, chain);

        verify(exchange).sendResponseHeaders(eq(403), anyLong());
        assertTrue(responseBody.toString().contains("Insufficient role"));
    }

    @Test
    void testValidToken_NoRolesClaim() throws IOException {
        when(mockJwt.getClaim("realm_access")).thenReturn(mockClaim);
        when(mockClaim.asMap()).thenReturn(Collections.emptyMap()); // No "roles" key

        SecurityFilter securityFilter = createFilter(mockJwt, null);
        requestHeaders.add("Authorization", "Bearer valid.token");

        securityFilter.doFilter(exchange, chain);

        verify(exchange).sendResponseHeaders(eq(403), anyLong());
        assertTrue(responseBody.toString().contains("Insufficient role"));
    }
    
    @Test
    void testValidToken_NoRealmAccessClaim() throws IOException {
        when(mockJwt.getClaim("realm_access")).thenReturn(mockClaim);
        when(mockClaim.isNull()).thenReturn(true);

        SecurityFilter securityFilter = createFilter(mockJwt, null);
        requestHeaders.add("Authorization", "Bearer valid.token");

        securityFilter.doFilter(exchange, chain);

        verify(exchange).sendResponseHeaders(eq(403), anyLong());
        assertTrue(responseBody.toString().contains("No realm_access claim"));
    }

    @Test
    void testTokenFromCookie() throws IOException {
        SecurityFilter securityFilter = createFilter(null, new Exception("Invalid signature"));
        requestHeaders.add("Cookie", "other=foo; auth_token=invalid.token; more=bar");
        
        securityFilter.doFilter(exchange, chain);

        verify(exchange).sendResponseHeaders(eq(401), anyLong());
    }
    
    @Test
    void testDescription() {
        SecurityFilter securityFilter = createFilter(null, null);
        assertEquals("JWT Security Filter", securityFilter.description());
    }
}
