package com.example.webshop;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class ProductController implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    private final ProductRepository repository;
    private final HttpClient httpClient;

    public ProductController(ProductRepository repository) {
        this(repository, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
    }

    // Added constructor for testing
    public ProductController(ProductRepository repository, HttpClient httpClient) {
        this.repository = repository;
        this.httpClient = httpClient;
    }

    private static final String CSS = 
        "body { font-family: Arial, Helvetica, sans-serif; background-color: #F8F9FA; color: #343A40; margin: 0; padding: 20px; }" +
        ".container { max-width: 1200px; margin: 0 auto; background-color: #FFFFFF; padding: 20px; border-radius: 4px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
        "h1 { color: #343A40; }" +
        "table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }" +
        "th { background-color: #007BFF; color: #FFFFFF; padding: 12px; text-align: left; }" +
        "td { padding: 12px; border-bottom: 1px solid #DEE2E6; }" +
        "tr:nth-child(even) { background-color: #F2F2F2; }" +
        ".btn { display: inline-block; padding: 10px 20px; border-radius: 4px; text-decoration: none; color: #FFFFFF; font-weight: bold; border: none; cursor: pointer; margin-right: 10px; }" +
        ".btn-primary { background-color: #007BFF; }" +
        ".btn-success { background-color: #28A745; }" +
        ".btn-secondary { background-color: #6C757D; }" +
        ".product-card { border: 1px solid #DEE2E6; padding: 15px; margin-bottom: 15px; border-radius: 4px; }" +
        ".variant-selector { margin-bottom: 10px; }" +
        ".campaign-info { font-size: 0.9em; color: #28A745; margin-top: 5px; font-style: italic; }";

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
        "      cart.push({id: id, name: name, price: price, quantity: 1});" +
        "      alert('Added ' + name + ' to cart!');" +
        "    } else {" +
        "      alert('Out of stock!');" +
        "    }" +
        "  }" +
        "  localStorage.setItem('cart', JSON.stringify(cart));" +
        "}" +
        "function updateVariant(groupId) {" +
        "  const selectors = document.querySelectorAll('.variant-selector-' + groupId);" +
        "  const selectedAttributes = {};" +
        "  selectors.forEach(sel => selectedAttributes[sel.dataset.attribute] = sel.value);" +
        "  " +
        "  const variants = JSON.parse(document.getElementById('variants-' + groupId).textContent);" +
        "  const match = variants.find(v => {" +
        "    const attrs = v.attributes;" +
        "    for (const key in selectedAttributes) {" +
        "      if (attrs[key] !== selectedAttributes[key]) return false;" +
        "    }" +
        "    return true;" +
        "  });" +
        "  " +
        "  const display = document.getElementById('product-display-' + groupId);" +
        "  const addToCartBtn = document.getElementById('add-to-cart-' + groupId);" +
        "  " +
        "  if (match) {" +
        "    display.innerHTML = 'Price: ' + match.price.toFixed(2) + ' ' + match.unit + '<br>Stock: ' + match.stock;" +
        "    addToCartBtn.disabled = match.stock <= 0;" +
        "    addToCartBtn.onclick = function() { addToCart(match.id, match.name, match.price, match.stock); };" +
        "    addToCartBtn.textContent = match.stock > 0 ? 'Add to Cart' : 'Out of Stock';" +
        "  } else {" +
        "    display.innerHTML = 'Variant not available';" +
        "    addToCartBtn.disabled = true;" +
        "    addToCartBtn.textContent = 'Unavailable';" +
        "  }" +
        "}" +
        "</script>";

    private String getHeader() {
        return "<div style='display:flex; justify-content:space-between; align-items:center; margin-bottom: 20px;'>" +
               "<div>" +
               "<button onclick=\"window.location.href='/'\" class='btn btn-primary'>Webshop Home</button>" +
               "<button onclick=\"window.location.href='/products'\" class='btn btn-secondary'>Products</button>" +
               "<button onclick=\"window.location.href='/cart'\" class='btn btn-secondary'>Cart</button>" +
               "<button onclick=\"window.location.href='/my-orders'\" class='btn btn-secondary'>My Orders</button>" +
               "</div>" +
               "<button onclick=\"window.location.href='/logout'\" class='btn btn-secondary'>Logout</button>" +
               "</div>";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        logger.info("Received request: {} {}", exchange.getRequestMethod(), path);
        try {
            List<Product> products = repository.findAll();
            String campaigns = fetchCampaigns();
            
            // Group products by base name (assuming format "GroupName - Attributes")
            Map<String, List<Product>> groupedProducts = new HashMap<>();
            for (Product p : products) {
                String baseName = p.getName().split(" - ")[0];
                groupedProducts.computeIfAbsent(baseName, k -> new ArrayList<>()).add(p);
            }

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><style>").append(CSS).append("</style>");
            html.append(JS).append("</head><body>");
            html.append("<div class='container'>");
            html.append(getHeader());
            html.append("<h1>Webshop Products</h1>");
            
            int groupIdCounter = 0;
            for (Map.Entry<String, List<Product>> entry : groupedProducts.entrySet()) {
                String groupName = entry.getKey();
                List<Product> variants = entry.getValue();
                Product firstVariant = variants.get(0); // Use first variant for description
                groupIdCounter++;
                
                html.append("<div class='product-card'>");
                html.append("<h2>").append(groupName).append("</h2>");
                html.append("<p>").append(firstVariant.getDescription()).append("</p>");
                if (!campaigns.isEmpty()) {
                    html.append("<p class='campaign-info'>Possible campaigns: ").append(campaigns).append("</p>");
                }
                
                // Extract all unique attributes
                Map<String, List<String>> attributesMap = new HashMap<>();
                for (Product v : variants) {
                    Map<String, String> attrs = parseAttributesFromName(v.getName());
                    for (Map.Entry<String, String> attr : attrs.entrySet()) {
                        attributesMap.computeIfAbsent(attr.getKey(), k -> new ArrayList<>()).add(attr.getValue());
                    }
                }
                
                // Deduplicate attribute values
                for (Map.Entry<String, List<String>> attrEntry : attributesMap.entrySet()) {
                    attrEntry.setValue(attrEntry.getValue().stream().distinct().sorted().collect(Collectors.toList()));
                }

                // Render Dropdowns
                if (!attributesMap.isEmpty()) {
                    for (Map.Entry<String, List<String>> attr : attributesMap.entrySet()) {
                        html.append("<div class='variant-selector'>");
                        html.append("<label>").append(attr.getKey()).append(": </label>");
                        html.append("<select class='variant-selector-").append(groupIdCounter).append("' data-attribute='").append(attr.getKey()).append("' onchange='updateVariant(").append(groupIdCounter).append(")'>");
                        for (String val : attr.getValue()) {
                            html.append("<option value='").append(val).append("'>").append(val).append("</option>");
                        }
                        html.append("</select>");
                        html.append("</div>");
                    }
                }

                // Initial Display (First Variant)
                String priceStr = String.format(Locale.US, "%.2f", firstVariant.getPrice());
                
                html.append("<div id='product-display-").append(groupIdCounter).append("'>");
                html.append("Price: ").append(priceStr).append(" ").append(firstVariant.getUnit()).append("<br>");
                html.append("Stock: ").append(firstVariant.getStock());
                html.append("</div>");

                // Add to Cart Button
                Integer productId = firstVariant.getPmId() != null ? firstVariant.getPmId() : firstVariant.getId();
                html.append("<button id='add-to-cart-").append(groupIdCounter).append("' onclick=\"addToCart(")
                    .append(productId).append(", '")
                    .append(firstVariant.getName().replace("'", "\\'")).append("', ")
                    .append(firstVariant.getPrice()).append(", ")
                    .append(firstVariant.getStock()).append(")\" class='btn btn-success'>Add to Cart</button>");

                // Hidden JSON data for variants
                html.append("<script id='variants-").append(groupIdCounter).append("' type='application/json'>");
                html.append("[");
                for (int i = 0; i < variants.size(); i++) {
                    Product v = variants.get(i);
                    Integer vId = v.getPmId() != null ? v.getPmId() : v.getId();
                    Map<String, String> attrs = parseAttributesFromName(v.getName());
                    html.append("{");
                    html.append("\"id\":").append(vId).append(",");
                    html.append("\"name\":\"").append(v.getName().replace("\"", "\\\"")).append("\",");
                    html.append("\"price\":").append(v.getPrice()).append(",");
                    html.append("\"unit\":\"").append(v.getUnit()).append("\",");
                    html.append("\"stock\":").append(v.getStock()).append(",");
                    html.append("\"attributes\":{");
                    int k = 0;
                    for (Map.Entry<String, String> attr : attrs.entrySet()) {
                        html.append("\"").append(attr.getKey()).append("\":\"").append(attr.getValue()).append("\"");
                        if (k < attrs.size() - 1) html.append(",");
                        k++;
                    }
                    html.append("}");
                    html.append("}");
                    if (i < variants.size() - 1) html.append(",");
                }
                html.append("]");
                html.append("</script>");
                
                html.append("</div>");
            }
            
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

    private String fetchCampaigns() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://loyalty-service:8084/api/loyalty/rules"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String json = response.body();
                // Simple parsing: remove [" and "] and replace "," with ", "
                return json.replace("[\"", "").replace("\"]", "").replace("\",\"", ", ");
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.warn("Failed to fetch campaigns", e);
        }
        return "";
    }

    private Map<String, String> parseAttributesFromName(String fullName) {
        Map<String, String> attributes = new HashMap<>();
        String[] parts = fullName.split(" - ");
        if (parts.length > 1) {
            String suffix = parts[1];
            String[] values = suffix.split(" ");
            for (int i = 0; i < values.length; i++) {
                attributes.put("Option " + (i + 1), values[i]);
            }
        }
        return attributes;
    }
}
