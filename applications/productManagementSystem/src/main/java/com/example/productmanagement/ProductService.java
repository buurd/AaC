package com.example.productmanagement;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ProductService {

    private final ProductRepository repository;
    private final HttpClient httpClient;
    private final String webshopApiUrl;
    private final String warehouseApiUrl;
    private final TokenService tokenService;

    public ProductService(ProductRepository repository) {
        this(repository, 
             System.getenv().getOrDefault("WEBSHOP_API_URL", "http://webshop-demo:8000/api/products/sync"),
             System.getenv().getOrDefault("WAREHOUSE_API_URL", "http://warehouse-demo:8002/api/products/sync"),
             new KeycloakTokenService(
                 System.getenv().getOrDefault("TOKEN_URL", "http://keycloak:8080/realms/webshop-realm/protocol/openid-connect/token"),
                 System.getenv().getOrDefault("CLIENT_ID", "pm-client"),
                 System.getenv().getOrDefault("CLIENT_SECRET", "pm-secret")
             ));
    }

    public ProductService(ProductRepository repository, String webshopApiUrl, String warehouseApiUrl, TokenService tokenService) {
        this.repository = repository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.webshopApiUrl = webshopApiUrl;
        this.warehouseApiUrl = warehouseApiUrl;
        this.tokenService = tokenService;

        System.out.println("ProductService initialized with Webshop API URL: " + this.webshopApiUrl);
        System.out.println("ProductService initialized with Warehouse API URL: " + this.warehouseApiUrl);
    }

    public void createProduct(Product product) throws SQLException {
        int newId = repository.create(product);
        product.setId(newId);
    }

    public void updateProduct(Product product) throws SQLException {
        repository.update(product);
    }

    public void deleteProduct(int id) throws SQLException {
        repository.delete(id);
        syncDeleteToWebshop(id);
        syncDeleteToWarehouse(id);
    }

    public void syncProduct(int id) throws SQLException {
        Product p = repository.findById(id);
        if (p != null) {
            syncToWebshop(p);
            syncToWarehouse(p);
        }
    }

    // --- Group Logic ---

    public void createProductGroup(ProductGroup group) throws SQLException {
        repository.createGroup(group);
    }

    public void generateVariants(int groupId, Map<String, List<String>> attributes) throws SQLException {
        ProductGroup group = repository.findGroupById(groupId);
        if (group == null) return;

        List<Product> existingVariants = repository.findByGroupId(groupId);
        List<Map<String, String>> combinations = generateCombinations(attributes);

        for (Map<String, String> combo : combinations) {
            String attributesJson = mapToJson(combo);
            // Check if a variant with the same attributes already exists
            boolean exists = existingVariants.stream().anyMatch(v -> {
                // Simple string comparison of JSON might be brittle if order changes, 
                // but for this implementation we assume consistent ordering or exact string match.
                // A better approach would be parsing JSON and comparing maps.
                // For now, let's try to be robust by parsing if possible or just comparing strings.
                return v.getAttributes() != null && v.getAttributes().equals(attributesJson);
            });

            if (!exists) {
                Product p = new Product();
                p.setGroupId(groupId);
                p.setType("Variant");
                // Use the group name as the base name, but don't append attributes to the name field
                // The attributes will be displayed separately in the UI
                p.setName(group.getName()); 
                p.setDescription(group.getDescription());
                p.setPrice(group.getBasePrice());
                p.setUnit(group.getBaseUnit());
                p.setAttributes(attributesJson);
                
                repository.create(p);
            }
        }
    }

    private List<Map<String, String>> generateCombinations(Map<String, List<String>> attributes) {
        List<Map<String, String>> combinations = new ArrayList<>();
        // Sort keys to ensure consistent order for combinations
        List<String> keys = new ArrayList<>(attributes.keySet());
        keys.sort(String::compareTo);
        
        if (keys.isEmpty()) return combinations;

        generateCombinationsRecursive(attributes, keys, 0, new HashMap<>(), combinations);
        return combinations;
    }

    private void generateCombinationsRecursive(Map<String, List<String>> attributes, List<String> keys, int index, Map<String, String> current, List<Map<String, String>> combinations) {
        if (index == keys.size()) {
            combinations.add(new HashMap<>(current));
            return;
        }

        String key = keys.get(index);
        List<String> values = attributes.get(key);

        for (String value : values) {
            current.put(key, value);
            generateCombinationsRecursive(attributes, keys, index + 1, current, combinations);
            current.remove(key);
        }
    }

    private String generateVariantName(String baseName, Map<String, String> attributes) {
        // Sort attributes by key to ensure consistent naming
        String attrs = attributes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.joining(" "));
        return baseName + " - " + attrs;
    }
    
    // Helper method to parse JSON attributes back to a Map
    private Map<String, String> parseAttributes(String json) {
        // Use TreeMap to ensure keys are sorted alphabetically
        Map<String, String> attributes = new TreeMap<>();
        if (json == null || json.isEmpty() || json.equals("{}")) {
            return attributes;
        }
        
        String content = json.substring(1, json.length() - 1); // Remove { and }
        String[] pairs = content.split(",");
        for (String pair : pairs) {
            String[] parts = pair.split(":");
            if (parts.length == 2) {
                String key = parts[0].trim().replace("\"", "");
                String value = parts[1].trim().replace("\"", "");
                attributes.put(key, value);
            }
        }
        return attributes;
    }

    private String mapToJson(Map<String, String> map) {
        // Sort by key to ensure consistent JSON string for comparison
        return "{" + map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> "\"" + e.getKey() + "\": \"" + e.getValue() + "\"")
                .collect(Collectors.joining(", ")) + "}";
    }

    public void syncGroup(int groupId) throws SQLException {
        List<Product> variants = repository.findByGroupId(groupId);
        for (Product p : variants) {
            syncToWebshop(p);
            syncToWarehouse(p);
        }
    }

    // --- Sync Logic ---

    private void syncToWebshop(Product p) {
        tokenService.getAccessToken().thenAccept(token -> {
            try {
                System.out.println("Syncing product " + p.getId() + " to " + webshopApiUrl);
                
                // Flattening: We construct the display name dynamically here for the downstream system
                String displayName = p.getName();
                if (p.getAttributes() != null && !p.getAttributes().isEmpty()) {
                    Map<String, String> attrs = parseAttributes(p.getAttributes());
                    if (!attrs.isEmpty()) {
                        String variantSuffix = attrs.values().stream().collect(Collectors.joining(" "));
                        displayName = displayName + " - " + variantSuffix;
                    }
                }

                String json = String.format(Locale.US,
                    "{\"id\":%d,\"type\":\"%s\",\"name\":\"%s\",\"description\":\"%s\",\"price\":%.2f,\"unit\":\"%s\"}",
                    p.getId(), p.getType(), displayName, p.getDescription(), p.getPrice(), p.getUnit()
                );
                sendRequest(webshopApiUrl, json, token);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void syncToWarehouse(Product p) {
        tokenService.getAccessToken().thenAccept(token -> {
            try {
                System.out.println("Syncing product " + p.getId() + " to " + warehouseApiUrl);
                
                // Flattening: We construct the display name dynamically here for the downstream system
                String displayName = p.getName();
                if (p.getAttributes() != null && !p.getAttributes().isEmpty()) {
                    Map<String, String> attrs = parseAttributes(p.getAttributes());
                    if (!attrs.isEmpty()) {
                        String variantSuffix = attrs.values().stream().collect(Collectors.joining(" "));
                        displayName = displayName + " - " + variantSuffix;
                    }
                }

                // Warehouse only needs ID and Name
                String json = String.format(Locale.US,
                    "{\"id\":%d,\"name\":\"%s\"}",
                    p.getId(), displayName
                );
                sendRequest(warehouseApiUrl, json, token);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendRequest(String url, String json, String token) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        System.err.println("Failed to sync to " + url + ". Status: " + response.statusCode() + " Body: " + response.body());
                    } else {
                        System.out.println("Synced to " + url + ". Response: " + response.body());
                    }
                })
                .exceptionally(e -> {
                    System.err.println("Exception during sync to " + url + ": " + e.getMessage());
                    throw new RuntimeException("Exception during sync to " + url, e);
                });
    }

    private void syncDeleteToWebshop(int id) {
        tokenService.getAccessToken().thenAccept(token -> {
            sendDeleteRequest(webshopApiUrl, id, token);
        });
    }

    private void syncDeleteToWarehouse(int id) {
        tokenService.getAccessToken().thenAccept(token -> {
            sendDeleteRequest(warehouseApiUrl, id, token);
        });
    }

    private void sendDeleteRequest(String url, int id, String token) {
        try {
            System.out.println("Syncing delete product " + id + " to " + url);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "?id=" + id))
                    .header("Authorization", "Bearer " + token)
                    .DELETE()
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != 200) {
                            System.err.println("Failed to sync delete to " + url + ". Status: " + response.statusCode() + " Body: " + response.body());
                        } else {
                            System.out.println("Synced delete product " + id + " to " + url + ". Response: " + response.body());
                        }
                    })
                    .exceptionally(e -> {
                        System.err.println("Exception during delete sync to " + url + ": " + e.getMessage());
                        throw new RuntimeException("Exception during delete sync to " + url, e);
                    });
        } catch (Exception e) {
            System.err.println("Error preparing delete sync request to " + url + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
