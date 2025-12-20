package com.example.warehouse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

public class StockService {

    private final HttpClient httpClient;
    private final String webshopStockApiUrl;

    public StockService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.webshopStockApiUrl = System.getenv().getOrDefault("WEBSHOP_STOCK_API_URL", "http://localhost:8000/api/stock/sync");
        System.out.println("StockService initialized with Webshop Stock API URL: " + this.webshopStockApiUrl);
    }

    public void syncStock(int pmId, int stock) {
        try {
            System.out.println("Syncing stock for product " + pmId + " (Stock: " + stock + ") to " + webshopStockApiUrl);
            String json = String.format(Locale.US, "{\"pmId\":%d,\"stock\":%d}", pmId, stock);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webshopStockApiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != 200) {
                            System.err.println("Failed to sync stock to Webshop. Status: " + response.statusCode() + " Body: " + response.body());
                        } else {
                            System.out.println("Synced stock for product " + pmId + " to Webshop.");
                        }
                    })
                    .exceptionally(e -> {
                        System.err.println("Exception during stock sync: " + e.getMessage());
                        e.printStackTrace();
                        return null;
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
