package com.example.webshop;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public class ProductController implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    private static final String CSS = 
        "body { font-family: Arial, Helvetica, sans-serif; background-color: #F8F9FA; color: #343A40; margin: 0; padding: 20px; }" +
        ".container { max-width: 1200px; margin: 0 auto; background-color: #FFFFFF; padding: 20px; border-radius: 4px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
        "h1 { color: #343A40; }" +
        "table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }" +
        "th { background-color: #007BFF; color: #FFFFFF; padding: 12px; text-align: left; }" +
        "td { padding: 12px; border-bottom: 1px solid #DEE2E6; }" +
        "tr:nth-child(even) { background-color: #F2F2F2; }" +
        ".btn { display: inline-block; padding: 10px 20px; border-radius: 4px; text-decoration: none; color: #FFFFFF; font-weight: bold; border: none; cursor: pointer; }" +
        ".btn-primary { background-color: #007BFF; }" +
        ".btn-success { background-color: #28A745; }" +
        ".btn-secondary { background-color: #6C757D; }";

    private static final String JS = 
        "<script>" +
        "function addToCart(id, name, price, stock) {" +
        "  let cart = JSON.parse(localStorage.getItem('cart')) || [];" +
        "  let item = cart.find(i => i.id === id);" +
        "  if (item) {" +
        "    if (item.quantity < stock) {" +
        "      item.quantity++;" +
        "      alert('Added ' + name + ' to cart!');" +
        "    } else {" +
        "      alert('Cannot add more. Out of stock!');" +
        "    }" +
        "  } else {" +
        "    if (stock > 0) {" +
        "      cart.push({id: id, name: name, price: price, quantity: 1, stock: stock});" +
        "      alert('Added ' + name + ' to cart!');" +
        "    } else {" +
        "      alert('Out of stock!');" +
        "    }" +
        "  }" +
        "  localStorage.setItem('cart', JSON.stringify(cart));" +
        "}" +
        "</script>";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        logger.info("Received request: {} {}", exchange.getRequestMethod(), path);
        try {
            List<Product> products = repository.findAll();
            
            // Dynamic URL construction
            String host = exchange.getRequestHeaders().getFirst("Host");
            if (host == null) host = "localhost:8443";
            String baseUrl = "https://" + host;
            if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

            String defaultKeycloakUrl = System.getenv().getOrDefault("KEYCLOAK_URL", "https://localhost:8446");
            String currentKeycloakUrl = defaultKeycloakUrl;
            if (host.contains(":8443")) {
                currentKeycloakUrl = "https://" + host.replace(":8443", ":8446");
            }

            // Use /logout endpoint instead of direct Keycloak logout
            String logoutUrl = "/logout";
            
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><style>").append(CSS).append("</style>");
            html.append(JS).append("</head><body>");
            html.append("<div class='container'>");
            html.append("<div style='display:flex; justify-content:space-between; align-items:center;'>");
            html.append("<h1>Webshop Products</h1>");
            html.append("<div>");
            html.append("<a href='/cart' class='btn btn-secondary' style='margin-right: 10px;'>View Cart</a>");
            html.append("<a href='" + logoutUrl + "' class='btn btn-secondary'>Logout</a>");
            html.append("</div>");
            html.append("</div>");
            html.append("<table>");
            html.append("<thead><tr><th>Name</th><th>Description</th><th>Price</th><th>Stock</th><th>Action</th></tr></thead>");
            html.append("<tbody>");
            
            for (Product p : products) {
                String priceStr = String.format(Locale.US, "%.2f", p.getPrice());
                
                html.append("<tr>");
                html.append("<td>").append(p.getName()).append("</td>");
                html.append("<td>").append(p.getDescription()).append("</td>");
                html.append("<td>").append(priceStr).append(" ").append(p.getUnit()).append("</td>");
                html.append("<td>").append(p.getStock()).append("</td>");
                html.append("<td>");
                html.append("<button onclick=\"addToCart(").append(p.getId()).append(", '")
                    .append(p.getName().replace("'", "\\'")).append("', ")
                    .append(p.getPrice()).append(", ")
                    .append(p.getStock()).append(")\" class='btn btn-success'>Add to Cart</button>");
                html.append("</td>");
                html.append("</tr>");
            }
            
            html.append("</tbody></table>");
            html.append("</div></body></html>");
            
            String response = html.toString();
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (SQLException e) {
            logger.error("Error handling request", e);
            String errorResponse = "Internal Server Error";
            exchange.sendResponseHeaders(500, errorResponse.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorResponse.getBytes());
            }
        }
    }
}
