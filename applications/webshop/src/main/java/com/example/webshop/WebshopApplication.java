package com.example.webshop;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import java.net.URLDecoder;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.URLEncoder;

public class WebshopApplication {

    private static final Logger logger = LoggerFactory.getLogger(WebshopApplication.class);

    private static final String CSS = 
        "body { font-family: Arial, Helvetica, sans-serif; background-color: #F8F9FA; color: #343A40; margin: 0; padding: 20px; }" +
        ".container { max-width: 1200px; margin: 0 auto; background-color: #FFFFFF; padding: 20px; border-radius: 4px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
        "h1 { color: #343A40; }" +
        ".btn { display: inline-block; padding: 10px 20px; border-radius: 4px; text-decoration: none; color: #FFFFFF; font-weight: bold; border: none; cursor: pointer; margin-right: 10px; }" +
        ".btn-primary { background-color: #007BFF; }" +
        ".btn-secondary { background-color: #6C757D; }" +
        ".btn-success { background-color: #28A745; }" +
        "input[type='text'], input[type='password'] { padding: 8px; border: 1px solid #CED4DA; border-radius: 4px; width: 100%; box-sizing: border-box; margin-bottom: 10px; }" +
        "label { display: block; font-weight: bold; margin-bottom: 5px; }";

    public static void main(String[] args) throws IOException, SQLException {
        // --- Database Setup ---
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/postgres");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");

        // Security Setup
        String jwksUrl = System.getenv().getOrDefault("JWKS_URL", "http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/certs");
        String issuer = System.getenv().getOrDefault("ISSUER_URL", "https://localhost:8446/realms/webshop-realm");
        // KEYCLOAK_URL is now derived dynamically if possible, or falls back to env
        String defaultKeycloakUrl = System.getenv().getOrDefault("KEYCLOAK_URL", "https://localhost:8446");
        String tokenUrl = System.getenv().getOrDefault("TOKEN_URL", "http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/token");

        logger.info("Connecting to database at: {}", dbUrl);
        Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        logger.info("Database connection successful.");

        // Initialize schema
        try (InputStream is = WebshopApplication.class.getResourceAsStream("/schema.sql")) {
            if (is == null) {
                throw new IOException("Cannot find schema.sql in resources.");
            }
            String schemaSql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(schemaSql);
                logger.info("Database schema initialized.");
            }
        }

        // Create repository
        ProductRepository productRepository = new ProductRepository(connection);
        
        // Create services
        OrderService orderService = new OrderService();

        // Security Filters
        SecurityFilter productSyncFilter = new SecurityFilter(jwksUrl, issuer, "product-sync");
        SecurityFilter stockSyncFilter = new SecurityFilter(jwksUrl, issuer, "stock-sync");

        // --- HTTP Server Setup ---
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/", (exchange) -> {
            String path = exchange.getRequestURI().getPath();
            logger.info("Received request: {} {}", exchange.getRequestMethod(), path);
            if ("/".equals(path)) {
                String host = exchange.getRequestHeaders().getFirst("Host");
                if (host == null) host = "localhost:8443"; // Fallback
                // Assuming HTTPS via proxy
                String baseUrl = "https://" + host;
                if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                
                // Derive Keycloak URL from Host if possible (replace 8443 with 8446)
                String currentKeycloakUrl = defaultKeycloakUrl;
                if (host.contains(":8443")) {
                    currentKeycloakUrl = "https://" + host.replace(":8443", ":8446");
                }

                String logoutUrl = "/logout";
                String encodedRedirectUri = URLEncoder.encode(baseUrl + "/", StandardCharsets.UTF_8);
                String registerUrl = currentKeycloakUrl + "/realms/webshop-realm/protocol/openid-connect/registrations?client_id=webshop-client&response_type=code&scope=openid&redirect_uri=" + encodedRedirectUri;
                
                String header = "<div style='display:flex; justify-content:space-between; align-items:center; margin-bottom: 20px;'>" +
                                "<div>" +
                                "<button onclick=\"window.location.href='/'\" class='btn btn-primary'>Webshop Home</button>" +
                                "<button onclick=\"window.location.href='/products'\" class='btn btn-secondary'>Products</button>" +
                                "<button onclick=\"window.location.href='/cart'\" class='btn btn-secondary'>Cart</button>" +
                                "<button onclick=\"window.location.href='/my-orders'\" class='btn btn-secondary'>My Orders</button>" +
                                "</div>" +
                                "<div>" +
                                "<button onclick=\"window.location.href='/login'\" class='btn btn-primary'>Login</button>" +
                                "<button onclick=\"window.location.href='" + registerUrl + "'\" class='btn btn-success'>Register</button>" +
                                "<button onclick=\"window.location.href='" + logoutUrl + "'\" class='btn btn-secondary'>Logout</button>" +
                                "</div>" +
                                "</div>";

                String html = "<!DOCTYPE html><html><head><style>" + CSS + "</style></head><body><div class='container'>" +
                              header +
                              "<h1>Welcome to the Webshop!</h1>" +
                              "<p>Browse our amazing products.</p>" +
                              "<button onclick=\"window.location.href='/products'\" class='btn btn-primary'>View Products</button>" +
                              "</div></body></html>";
                byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                String response = "404 Not Found";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
            exchange.close();
        });
        
        server.createContext("/login", new LoginHandler(tokenUrl));
        
        server.createContext("/logout", (exchange) -> {
            String path = exchange.getRequestURI().getPath();
            logger.info("Received request: {} {}", exchange.getRequestMethod(), path);
            
            String host = exchange.getRequestHeaders().getFirst("Host");
            if (host == null) host = "localhost:8443";
            String baseUrl = "https://" + host;
            if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

            // Derive Keycloak URL from Host if possible (replace 8443 with 8446)
            String currentKeycloakUrl = defaultKeycloakUrl;
            if (host.contains(":8443")) {
                currentKeycloakUrl = "https://" + host.replace(":8443", ":8446");
            }

            // Ensure redirect_uri is URL encoded
            String encodedRedirectUri = URLEncoder.encode(baseUrl + "/", StandardCharsets.UTF_8);
            // Use post_logout_redirect_uri instead of redirect_uri for OIDC logout
            String keycloakLogoutUrl = currentKeycloakUrl + "/realms/webshop-realm/protocol/openid-connect/logout?post_logout_redirect_uri=" + encodedRedirectUri + "&client_id=webshop-client";

            // Clear cookie
            exchange.getResponseHeaders().add("Set-Cookie", "webshop_auth_token=; Path=/; Max-Age=0");
            exchange.getResponseHeaders().add("Set-Cookie", "webshop_username=; Path=/; Max-Age=0");
            
            // Redirect
            exchange.getResponseHeaders().set("Location", keycloakLogoutUrl);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        
        server.createContext("/products", new ProductController(productRepository));
        server.createContext("/cart", new ShoppingCartController());
        server.createContext("/my-orders", new OrderHistoryController(orderService));
        server.createContext("/api/orders", new OrderController(orderService));
        
        HttpContext productSyncContext = server.createContext("/api/products/sync", new ProductSyncController(productRepository));
        productSyncContext.getFilters().add(productSyncFilter);
        
        HttpContext stockSyncContext = server.createContext("/api/stock/sync", new StockSyncController(productRepository));
        stockSyncContext.getFilters().add(stockSyncFilter);
        
        // Use a thread pool to handle multiple requests concurrently
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        logger.info("Server started on port {}", port);
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
            logger.info("Received request: {} {}", t.getRequestMethod(), t.getRequestURI().getPath());
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
                        // Removed HttpOnly to allow JS access
                        t.getResponseHeaders().add("Set-Cookie", "webshop_auth_token=" + accessToken + "; Path=/");
                        t.getResponseHeaders().add("Set-Cookie", "webshop_username=" + username + "; Path=/");
                        redirect(t, "/products");
                    } else {
                        logger.warn("Login failed for user: {}", username);
                        sendResponse(t, 401, "<h1>Login Failed</h1><p>Invalid credentials</p><a href='/login'>Try Again</a>");
                    }
                } catch (InterruptedException e) {
                    logger.error("Error during login", e);
                    sendResponse(t, 500, "<h1>Error</h1><p>" + e.getMessage() + "</p>");
                }
            } else {
                String body = "<h1>Login</h1>" +
                              "<form action='/login' method='post'>" +
                              "<label>Username</label><input type='text' name='username'>" +
                              "<label>Password</label><input type='password' name='password'>" +
                              "<button type='submit' class='btn btn-primary'>Login</button>" +
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
