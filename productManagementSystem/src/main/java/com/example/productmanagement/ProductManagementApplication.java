package com.example.productmanagement;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ProductManagementApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductManagementApplication.class);

    private static final String CSS = 
        "body { font-family: Arial, Helvetica, sans-serif; background-color: #F8F9FA; color: #343A40; margin: 0; padding: 20px; }" +
        ".container { max-width: 1200px; margin: 0 auto; background-color: #FFFFFF; padding: 20px; border-radius: 4px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
        "h1, h2 { color: #343A40; }" +
        "table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }" +
        "th { background-color: #007BFF; color: #FFFFFF; padding: 12px; text-align: left; }" +
        "td { padding: 12px; border-bottom: 1px solid #DEE2E6; }" +
        "tr:nth-child(even) { background-color: #F2F2F2; }" +
        ".btn { display: inline-block; padding: 10px 20px; border-radius: 4px; text-decoration: none; color: #FFFFFF; font-weight: bold; border: none; cursor: pointer; margin-right: 5px; }" +
        ".btn-primary { background-color: #007BFF; }" +
        ".btn-secondary { background-color: #6C757D; }" +
        ".btn-danger { background-color: #DC3545; }" +
        ".btn-success { background-color: #28A745; }" +
        ".btn-warning { background-color: #FFC107; color: #212529; }" +
        "input[type='text'], input[type='number'], input[type='password'] { padding: 8px; border: 1px solid #CED4DA; border-radius: 4px; width: 100%; box-sizing: border-box; margin-bottom: 10px; }" +
        "label { display: block; font-weight: bold; margin-bottom: 5px; }";

    private static ProductRepository repository;
    private static ProductService service;

    public static void main(String[] args) throws IOException, SQLException {
        // Database Setup
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/postgres");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");

        // Security Setup
        String jwksUrl = System.getenv().getOrDefault("JWKS_URL", "http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/certs");
        String issuer = System.getenv().getOrDefault("ISSUER_URL", "https://localhost:8446/realms/webshop-realm");
        String tokenUrl = System.getenv().getOrDefault("TOKEN_URL", "http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/token");

        logger.info("Connecting to database at: {}", dbUrl);
        Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        logger.info("Database connection successful.");

        // Initialize schema
        try (InputStream is = ProductManagementApplication.class.getResourceAsStream("/schema.sql")) {
            if (is == null) {
                throw new IOException("Cannot find schema.sql in resources.");
            }
            String schemaSql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(schemaSql);
                logger.info("Database schema initialized.");
            }
        }

        repository = new ProductRepository(connection);
        service = new ProductService(repository);

        SecurityFilter securityFilter = new SecurityFilter(jwksUrl, issuer, "product-manager");

        int port = 8001;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/", new RootHandler()); // Public landing page
        server.createContext("/login", new LoginHandler(tokenUrl)); // Public login page
        
        // Protected Contexts
        HttpContext productsContext = server.createContext("/products", new ProductListHandler());
        productsContext.getFilters().add(securityFilter);
        
        HttpContext createContext = server.createContext("/products/create", new ProductCreateHandler());
        createContext.getFilters().add(securityFilter);
        
        HttpContext editContext = server.createContext("/products/edit", new ProductEditHandler());
        editContext.getFilters().add(securityFilter);
        
        HttpContext deleteContext = server.createContext("/products/delete", new ProductDeleteHandler());
        deleteContext.getFilters().add(securityFilter);
        
        HttpContext syncContext = server.createContext("/products/sync", new ProductSyncHandler());
        syncContext.getFilters().add(securityFilter);

        // Use a thread pool
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        logger.info("Product Management Server started on port {}", port);
    }

    private static void sendResponse(HttpExchange t, String body) throws IOException {
        String html = "<!DOCTYPE html><html><head><style>" + CSS + "</style></head><body><div class='container'>" + body + "</div></body></html>";
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        t.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        t.sendResponseHeaders(200, bytes.length);
        OutputStream os = t.getResponseBody();
        os.write(bytes);
        os.close();
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

    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            logger.info("Received request: {} {}", t.getRequestMethod(), t.getRequestURI().getPath());
            String body = "<h1>Product Management System</h1>" +
                          "<p>Welcome to the Product Management System.</p>" +
                          "<a href='/products' class='btn btn-primary'>Manage Products</a>";
            sendResponse(t, body);
        }
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
                        // Changed cookie name to pm_auth_token
                        t.getResponseHeaders().add("Set-Cookie", "pm_auth_token=" + accessToken + "; Path=/; HttpOnly");
                        redirect(t, "/products");
                    } else {
                        logger.warn("Login failed for user: {}", username);
                        sendResponse(t, "<h1>Login Failed</h1><p>Invalid credentials</p><a href='/login'>Try Again</a>");
                    }
                } catch (InterruptedException e) {
                    logger.error("Error during login", e);
                    sendResponse(t, "<h1>Error</h1><p>" + e.getMessage() + "</p>");
                }
            } else {
                String body = "<h1>Login</h1>" +
                              "<form action='/login' method='post'>" +
                              "<label>Username</label><input type='text' name='username'>" +
                              "<label>Password</label><input type='password' name='password'>" +
                              "<button type='submit' class='btn btn-primary'>Login</button>" +
                              "</form>";
                sendResponse(t, body);
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

    static class ProductListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            logger.info("Received request: {} {}", t.getRequestMethod(), t.getRequestURI().getPath());
            try {
                List<Product> products = repository.findAll();
                StringBuilder sb = new StringBuilder();
                sb.append("<h1>Product List</h1>");
                sb.append("<a href='/products/create' class='btn btn-success' style='margin-bottom: 20px;'>Create New Product</a>");
                sb.append("<table><thead><tr><th>ID</th><th>Type</th><th>Name</th><th>Price</th><th>Unit</th><th>Description</th><th>Actions</th></tr></thead><tbody>");
                
                for (Product p : products) {
                    sb.append("<tr>");
                    sb.append("<td>").append(p.getId()).append("</td>");
                    sb.append("<td>").append(p.getType() != null ? p.getType() : "").append("</td>");
                    sb.append("<td>").append(p.getName()).append("</td>");
                    sb.append("<td>").append(String.format(Locale.US, "%.2f", p.getPrice())).append("</td>");
                    sb.append("<td>").append(p.getUnit() != null ? p.getUnit() : "").append("</td>");
                    sb.append("<td>").append(p.getDescription()).append("</td>");
                    sb.append("<td>");
                    sb.append("<a href='/products/edit?id=").append(p.getId()).append("' class='btn btn-secondary'>Edit</a>");
                    sb.append("<a href='/products/sync?id=").append(p.getId()).append("' class='btn btn-warning'>Sync</a>");
                    sb.append("<a href='/products/delete?id=").append(p.getId()).append("' class='btn btn-danger'>Delete</a>");
                    sb.append("</td>");
                    sb.append("</tr>");
                }
                sb.append("</tbody></table>");
                sendResponse(t, sb.toString());
            } catch (SQLException e) {
                logger.error("Error listing products", e);
                sendResponse(t, "<h1>Error</h1><p>" + e.getMessage() + "</p>");
            }
        }
    }

    static class ProductCreateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            logger.info("Received request: {} {}", t.getRequestMethod(), t.getRequestURI().getPath());
            if ("POST".equalsIgnoreCase(t.getRequestMethod())) {
                InputStream is = t.getRequestBody();
                String formData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> params = parseFormData(formData);
                
                Product p = new Product();
                p.setName(params.get("name"));
                p.setType(params.get("type"));
                p.setDescription(params.get("description"));
                p.setUnit(params.get("unit"));
                try {
                    p.setPrice(Double.parseDouble(params.get("price")));
                } catch (NumberFormatException e) {
                    p.setPrice(0.0);
                }

                try {
                    service.createProduct(p); // Use service
                    redirect(t, "/products");
                } catch (SQLException e) {
                    logger.error("Error creating product", e);
                    sendResponse(t, "<h1>Error Creating Product</h1><p>" + e.getMessage() + "</p>");
                }
            } else {
                String body = "<h1>Create Product</h1>" +
                              "<form action='/products/create' method='post'>" +
                              "<label>Type</label><input type='text' name='type' value='Book'>" +
                              "<label>Name</label><input type='text' name='name'>" +
                              "<label>Price</label><input type='number' name='price' step='0.01'>" +
                              "<label>Unit</label><input type='text' name='unit' value='pcs'>" +
                              "<label>Description</label><input type='text' name='description'>" +
                              "<button type='submit' class='btn btn-success'>Save</button> " +
                              "<a href='/products' class='btn btn-secondary'>Cancel</a>" +
                              "</form>";
                sendResponse(t, body);
            }
        }
    }

    static class ProductEditHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            logger.info("Received request: {} {}", t.getRequestMethod(), t.getRequestURI().getPath());
            String query = t.getRequestURI().getQuery();
            Map<String, String> queryParams = parseFormData(query != null ? query : "");
            String idStr = queryParams.get("id");
            
            if ("POST".equalsIgnoreCase(t.getRequestMethod())) {
                InputStream is = t.getRequestBody();
                String formData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> params = parseFormData(formData);
                
                try {
                    int id = Integer.parseInt(params.get("id"));
                    Product p = new Product();
                    p.setId(id);
                    p.setName(params.get("name"));
                    p.setType(params.get("type"));
                    p.setDescription(params.get("description"));
                    p.setUnit(params.get("unit"));
                    try {
                        p.setPrice(Double.parseDouble(params.get("price")));
                    } catch (NumberFormatException e) {
                        p.setPrice(0.0);
                    }

                    service.updateProduct(p); // Use service
                    redirect(t, "/products");
                } catch (Exception e) {
                    logger.error("Error updating product", e);
                    sendResponse(t, "<h1>Error Updating Product</h1><p>" + e.getMessage() + "</p>");
                }
            } else {
                if (idStr == null) {
                    sendResponse(t, "<h1>Error</h1><p>No Product ID provided</p>");
                    return;
                }
                try {
                    int id = Integer.parseInt(idStr);
                    Product p = repository.findById(id);
                    if (p == null) {
                        sendResponse(t, "<h1>Error</h1><p>Product not found</p>");
                        return;
                    }
                    
                    String body = "<h1>Edit Product</h1>" +
                                  "<form action='/products/edit' method='post'>" +
                                  "<input type='hidden' name='id' value='" + p.getId() + "'>" +
                                  "<label>Type</label><input type='text' name='type' value='" + (p.getType() != null ? p.getType() : "") + "'>" +
                                  "<label>Name</label><input type='text' name='name' value='" + p.getName() + "'>" +
                                  "<label>Price</label><input type='number' name='price' step='0.01' value='" + p.getPrice() + "'>" +
                                  "<label>Unit</label><input type='text' name='unit' value='" + (p.getUnit() != null ? p.getUnit() : "") + "'>" +
                                  "<label>Description</label><input type='text' name='description' value='" + p.getDescription() + "'>" +
                                  "<button type='submit' class='btn btn-primary'>Update</button> " +
                                  "<a href='/products' class='btn btn-secondary'>Cancel</a>" +
                                  "</form>";
                    sendResponse(t, body);
                } catch (Exception e) {
                    logger.error("Error loading product for edit", e);
                    sendResponse(t, "<h1>Error</h1><p>" + e.getMessage() + "</p>");
                }
            }
        }
    }

    static class ProductDeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            logger.info("Received request: {} {}", t.getRequestMethod(), t.getRequestURI().getPath());
            if ("POST".equalsIgnoreCase(t.getRequestMethod())) {
                InputStream is = t.getRequestBody();
                String formData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> params = parseFormData(formData);
                
                try {
                    int id = Integer.parseInt(params.get("id"));
                    service.deleteProduct(id); // Use service (though it doesn't sync delete yet)
                    redirect(t, "/products");
                } catch (Exception e) {
                    logger.error("Error deleting product", e);
                    sendResponse(t, "<h1>Error Deleting Product</h1><p>" + e.getMessage() + "</p>");
                }
            } else {
                String query = t.getRequestURI().getQuery();
                Map<String, String> queryParams = parseFormData(query != null ? query : "");
                String idStr = queryParams.get("id");
                
                if (idStr == null) {
                    sendResponse(t, "<h1>Error</h1><p>No Product ID provided</p>");
                    return;
                }

                try {
                    int id = Integer.parseInt(idStr);
                    Product p = repository.findById(id);
                    if (p == null) {
                        sendResponse(t, "<h1>Error</h1><p>Product not found</p>");
                        return;
                    }

                    String body = "<h1>Delete Product</h1>" +
                                  "<p>Are you sure you want to delete product <strong>" + p.getName() + "</strong>?</p>" +
                                  "<form action='/products/delete' method='post'>" +
                                  "<input type='hidden' name='id' value='" + id + "'>" +
                                  "<button type='submit' class='btn btn-danger'>Yes, Delete</button> " +
                                  "<a href='/products' class='btn btn-secondary'>Cancel</a>" +
                                  "</form>";
                    sendResponse(t, body);
                } catch (Exception e) {
                    logger.error("Error loading product for delete", e);
                    sendResponse(t, "<h1>Error</h1><p>" + e.getMessage() + "</p>");
                }
            }
        }
    }

    static class ProductSyncHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            logger.info("Received request: {} {}", t.getRequestMethod(), t.getRequestURI().getPath());
            String query = t.getRequestURI().getQuery();
            Map<String, String> queryParams = parseFormData(query != null ? query : "");
            String idStr = queryParams.get("id");
            
            if (idStr == null) {
                sendResponse(t, "<h1>Error</h1><p>No Product ID provided</p>");
                return;
            }

            try {
                int id = Integer.parseInt(idStr);
                service.syncProduct(id);
                redirect(t, "/products");
            } catch (Exception e) {
                logger.error("Error syncing product", e);
                sendResponse(t, "<h1>Error Syncing Product</h1><p>" + e.getMessage() + "</p>");
            }
        }
    }
}
