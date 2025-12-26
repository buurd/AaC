package com.example.order;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderController implements HttpHandler {

    private final OrderRepository repository;
    private final StockReservationService stockService;
    private final OrderFulfillmentService fulfillmentService;
    private final CreditService creditService;
    private final InvoiceRepository invoiceRepository;

    public OrderController(OrderRepository repository, StockReservationService stockService, OrderFulfillmentService fulfillmentService, CreditService creditService, InvoiceRepository invoiceRepository) {
        this.repository = repository;
        this.stockService = stockService;
        this.fulfillmentService = fulfillmentService;
        this.creditService = creditService;
        this.invoiceRepository = invoiceRepository;
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
        ".btn-secondary { background-color: #6C757D; }";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("/orders".equals(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                handleListOrders(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        } else if ("/api/orders".equals(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                handleApiListOrders(exchange);
            } else if ("POST".equalsIgnoreCase(method)) {
                handleCreateOrder(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private void handleApiListOrders(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String customerName = null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && "customer".equals(pair[0])) {
                    customerName = java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                }
            }
        }

        try {
            List<Order> orders = repository.findAll(); // Should filter by customer in DB, but filtering in memory for now
            List<Order> filteredOrders = new ArrayList<>();
            if (customerName != null) {
                for (Order o : orders) {
                    if (customerName.equals(o.getCustomerName())) {
                        filteredOrders.add(o);
                    }
                }
            } else {
                filteredOrders = orders;
            }

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < filteredOrders.size(); i++) {
                Order o = filteredOrders.get(i);
                json.append(String.format("{\"id\":%d,\"customerName\":\"%s\",\"status\":\"%s\"}", 
                    o.getId(), o.getCustomerName(), o.getStatus()));
                if (i < filteredOrders.size() - 1) {
                    json.append(",");
                }
            }
            json.append("]");

            String response = json.toString();
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleListOrders(HttpExchange exchange) throws IOException {
        try {
            List<Order> orders = repository.findAll();
            String keycloakUrl = System.getenv().getOrDefault("KEYCLOAK_URL", "https://localhost:8446");
            String logoutUrl = keycloakUrl + "/realms/webshop-realm/protocol/openid-connect/logout?redirect_uri=https://localhost:8447/";
            
            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html><html><head><style>").append(CSS).append("</style></head><body><div class='container'>");
            sb.append("<h1>Order Management</h1>");
            sb.append("<div style='margin-bottom: 20px;'>");
            sb.append("<a href='" + logoutUrl + "' class='btn btn-secondary' style='float: right;'>Logout</a>");
            sb.append("</div>");
            sb.append("<table><thead><tr><th>ID</th><th>Customer</th><th>Status</th><th>Items</th></tr></thead><tbody>");
            
            for (Order o : orders) {
                sb.append("<tr>");
                sb.append("<td>").append(o.getId()).append("</td>");
                sb.append("<td>").append(o.getCustomerName()).append("</td>");
                sb.append("<td>").append(o.getStatus()).append("</td>");
                sb.append("<td>").append(o.getItems().size()).append("</td>");
                sb.append("</tr>");
            }
            sb.append("</tbody></table></div></body></html>");
            
            String response = sb.toString();
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleCreateOrder(HttpExchange exchange) throws IOException {
        int orderId = -1;
        try {
            InputStream is = exchange.getRequestBody();
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("Received Order: " + json);
            
            Order order = parseOrder(json);
            
            if (order.getItems().isEmpty()) {
                System.out.println("Warning: No items found in order!");
            }

            order.setStatus("PENDING");
            orderId = repository.createOrder(order);
            
            // Reserve stock
            boolean allReserved = true;
            for (OrderItem item : order.getItems()) {
                boolean reserved = stockService.reserveStock(item.getProductId(), item.getQuantity()).join();
                if (!reserved) {
                    allReserved = false;
                    break;
                }
            }
            
            if (allReserved) {
                repository.updateStatus(orderId, "CONFIRMED");
                
                // Create Invoice
                double totalAmount = 0; // In a real app, get prices from Product service
                invoiceRepository.createInvoice(new Invoice(orderId, order.getCustomerName(), totalAmount, LocalDate.now().plusDays(30)));
                
                // Notify Warehouse for fulfillment
                boolean notified = fulfillmentService.notifyOrderConfirmed(orderId).join();
                if (!notified) {
                    repository.updateStatus(orderId, "CONFIRMATION_FAILED");
                    throw new IOException("Failed to notify warehouse for fulfillment");
                }

                String response = "{\"status\":\"CONFIRMED\", \"orderId\":" + orderId + "}";
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                repository.updateStatus(orderId, "REJECTED");
                String response = "{\"status\":\"REJECTED\", \"orderId\":" + orderId + "}";
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(409, bytes.length); // Conflict
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            if (orderId != -1) {
                try {
                    repository.updateStatus(orderId, "ERROR");
                } catch (SQLException ex) {
                    e.addSuppressed(ex);
                }
            }
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private Order parseOrder(String json) {
        Order order = new Order();
        
        // Extract customerName
        Pattern customerNamePattern = Pattern.compile("\"customerName\"\\s*:\\s*\"([^\"]+)\"");
        Matcher customerNameMatcher = customerNamePattern.matcher(json);
        if (customerNameMatcher.find()) {
            order.setCustomerName(customerNameMatcher.group(1));
        } else {
            order.setCustomerName("Unknown Customer"); // Default
        }

        // Extract items - robust parsing
        // Split by "}" to separate objects roughly
        String[] parts = json.split("\\}");
        for (String part : parts) {
            if (part.contains("productId") && part.contains("quantity")) {
                int productId = -1;
                int quantity = -1;
                
                Pattern pId = Pattern.compile("\"productId\"\\s*:\\s*(\\d+)");
                Matcher mId = pId.matcher(part);
                if (mId.find()) {
                    productId = Integer.parseInt(mId.group(1));
                }
                
                Pattern pQty = Pattern.compile("\"quantity\"\\s*:\\s*(\\d+)");
                Matcher mQty = pQty.matcher(part);
                if (mQty.find()) {
                    quantity = Integer.parseInt(mQty.group(1));
                }
                
                if (productId != -1 && quantity != -1) {
                    order.addItem(new OrderItem(productId, quantity));
                }
            }
        }

        return order;
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
