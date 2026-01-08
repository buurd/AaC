package com.example.loyalty;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.net.URLEncoder;

public class LoyaltyApplication {

    public static void main(String[] args) throws IOException {
        int port = 8084;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Database Configuration (Environment Variables)
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/loyalty_db");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");

        // Security Configuration
        String jwksUrl = System.getenv().getOrDefault("JWKS_URL", "http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/certs");
        String issuer = System.getenv().getOrDefault("ISSUER_URL", "https://localhost:8446/realms/webshop-realm");
        String tokenUrl = System.getenv().getOrDefault("TOKEN_URL", "http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/token");

        // Dependency Injection
        LoyaltyRepository repository = new LoyaltyRepository(dbUrl, dbUser, dbPassword);
        BonusRuleEngine ruleEngine = new BonusRuleEngine();
        PointService pointService = new PointService(repository, ruleEngine);
        LoyaltyController controller = new LoyaltyController(pointService, ruleEngine);

        // Security Filter for admin actions
        SecurityFilter adminFilter = new SecurityFilter(jwksUrl, issuer, "loyalty-manager");

        // Public routes
        server.createContext("/api/loyalty/balance/", controller);
        server.createContext("/api/loyalty/rules", controller);
        server.createContext("/api/loyalty/calculate", controller);
        server.createContext("/api/loyalty/accrue", controller);
        server.createContext("/api/loyalty/redeem", controller);
        server.createContext("/", controller);
        
        // Protected admin routes
        HttpContext adminContext = server.createContext("/api/loyalty/admin/", controller);
        adminContext.getFilters().add(adminFilter);
        
        HttpContext dashboardContext = server.createContext("/admin/dashboard", controller);
        dashboardContext.getFilters().add(adminFilter);

        server.createContext("/login", new LoginHandler(tokenUrl));

        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("Loyalty Service started on port " + port);
    }

    static class LoginHandler implements HttpHandler {
        private final String tokenUrl;
        private final HttpClient httpClient;

        public LoginHandler(String tokenUrl) {
            this.tokenUrl = tokenUrl;
            this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equalsIgnoreCase(t.getRequestMethod())) {
                InputStream is = t.getRequestBody();
                String formData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> params = parseFormData(formData);
                
                String username = params.get("username");
                String password = params.get("password");
                
                String requestBody = "client_id=webshop-client&grant_type=password&username=" + username + "&password=" + password;
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(tokenUrl))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
                
                try {
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        String json = response.body();
                        String accessToken = extractToken(json);
                        t.getResponseHeaders().add("Set-Cookie", "auth_token=" + accessToken + "; Path=/; HttpOnly");
                        redirect(t, "/admin/dashboard");
                    } else {
                        sendResponse(t, 401, "<h1>Login Failed</h1><p>Invalid credentials</p><a href='/login'>Try Again</a>");
                    }
                } catch (InterruptedException e) {
                    sendResponse(t, 500, "<h1>Error</h1><p>" + e.getMessage() + "</p>");
                }
            } else {
                String body = "<h1>Login</h1>" +
                              "<form action='/login' method='post'>" +
                              "<label>Username</label><input type='text' name='username'>" +
                              "<label>Password</label><input type='password' name='password'>" +
                              "<button type='submit'>Login</button>" +
                              "</form>";
                sendResponse(t, 200, body);
            }
        }

        private String extractToken(String json) {
            int start = json.indexOf("\"access_token\":\"");
            if (start == -1) return null;
            start += 16;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        }
    }

    private static void sendResponse(HttpExchange t, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        t.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        t.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = t.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void redirect(HttpExchange t, String location) throws IOException {
        t.getResponseHeaders().set("Location", location);
        t.sendResponseHeaders(302, -1);
    }

    private static Map<String, String> parseFormData(String formData) {
        Map<String, String> map = new HashMap<>();
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length > 0) {
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : "";
                map.put(key, value);
            }
        }
        return map;
    }
}
