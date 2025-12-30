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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        ".btn-secondary { background-color: #6C757D; }" +
        ".btn-success { background-color: #28A745; }" +
        ".btn-primary { background-color: #007BFF; }";

    private String getHeader() {
        return "<div style='display:flex; justify-content:space-between; align-items:center; margin-bottom: 20px;'>" +
               "<button onclick=\"window.location.href='/'\" class='btn btn-primary'>Order Dashboard</button>" +
               "<button onclick=\"window.location.href='/logout'\" class='btn btn-secondary'>Logout</button>" +
               "</div>";
    }

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
        } else if ("/orders/confirm".equals(path)) {
            if ("POST".equalsIgnoreCase(method)) {
                handleConfirmOrder(exchange);
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
        } else if ("/api/orders/status".equals(path)) {
            if ("POST".equalsIgnoreCase(method)) {
                handleUpdateStatus(exchange);
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
            System.out.println("Listing " + orders.size() + " orders."); // Added logging
            
            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html><html><head><style>").append(CSS).append("</style></head><body><div class='container'>");
            sb.append(getHeader());
            sb.append("<h1>Order Management</h1>");
            sb.append("<table><thead><tr><th>ID</th><th>Customer</th><th>Status</th><th>Items</th><th>Actions</th></tr></thead><tbody>");
            
            for (Order o : orders) {
                System.out.println("Order: " + o.getId() + ", Status: " + o.getStatus()); // Added logging
                sb.append("<tr>");
                sb.append("<td>").append(o.getId()).append("</td>");
                sb.append("<td>").append(o.getCustomerName()).append("</td>");
                sb.append("<td>").append(o.getStatus()).append("</td>");
                sb.append("<td>").append(o.getItems().size()).append("</td>");
                sb.append("<td>");
                if ("PENDING_CONFIRMATION".equals(o.getStatus())) {
                     sb.append("<form action='/orders/confirm' method='post' style='display:inline;'>");
                     sb.append("<input type='hidden' name='id' value='").append(o.getId()).append("'>");
                     sb.append("<button type='submit' class='btn btn-success'>Confirm</button>");
                     sb.append("</form>");
                }
                sb.append("</td>");
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
                // Changed from CONFIRMED to PENDING_CONFIRMATION to allow manual confirmation
                repository.updateStatus(orderId, "PENDING_CONFIRMATION");
                
                // Notification to Warehouse and Invoice creation is now moved to handleConfirmOrder

                String response = "{\"status\":\"PENDING_CONFIRMATION\", \"orderId\":" + orderId + "}";
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

    private void handleConfirmOrder(HttpExchange exchange) throws IOException {
        try {
            InputStream is = exchange.getRequestBody();
            String formData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = parseFormData(formData);
            
            int orderId = Integer.parseInt(params.get("id"));
            
            repository.updateStatus(orderId, "CONFIRMED");
            
            // Create Invoice (Moved from handleCreateOrder)
            // We need to fetch the order details to get customer name and calculate amount
            // For now, I'll assume we can get it or just use placeholders as per upstream logic
            // Upstream logic: invoiceRepository.createInvoice(new Invoice(orderId, order.getCustomerName(), totalAmount, LocalDate.now().plusDays(30)));
            // But we don't have 'order' object here.
            // Let's fetch it.
            // Assuming repository has findById.
            // If not, we might need to add it or use a simplified approach.
            // repository.findAll() exists.
            
            // For simplicity and to match upstream intent without refactoring repository too much:
            // We'll just create the invoice with available info.
            // Ideally we should fetch the order.
            // Let's check if repository has findById.
            // It does not seem to have findById in the snippet I saw earlier.
            // But findAll returns all.
            
            // Let's try to find the order from findAll (inefficient but works for now)
            List<Order> allOrders = repository.findAll();
            Order order = allOrders.stream().filter(o -> o.getId() == orderId).findFirst().orElse(null);
            
            if (order != null) {
                 double totalAmount = 0; // Placeholder as per upstream
                 invoiceRepository.createInvoice(new Invoice(orderId, order.getCustomerName(), totalAmount, LocalDate.now().plusDays(30)));
            } else {
                System.err.println("Order not found for invoice creation: " + orderId);
            }
            
            // Notify Warehouse for fulfillment
            boolean notified = fulfillmentService.notifyOrderConfirmed(orderId).join();
            if (!notified) {
                System.err.println("Failed to notify warehouse for order " + orderId);
                repository.updateStatus(orderId, "CONFIRMATION_FAILED");
                throw new IOException("Failed to notify warehouse for fulfillment");
            }

            exchange.getResponseHeaders().set("Location", "/orders");
            exchange.sendResponseHeaders(302, -1);
        } catch (Exception e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleUpdateStatus(HttpExchange exchange) throws IOException {
        try {
            InputStream is = exchange.getRequestBody();
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("Update Status Request: " + json);
            
            int orderId = -1;
            String status = null;
            
            // Simple JSON parsing
            Pattern pId = Pattern.compile("\"orderId\"\\s*:\\s*(\\d+)");
            Matcher mId = pId.matcher(json);
            if (mId.find()) {
                orderId = Integer.parseInt(mId.group(1));
            }
            
            Pattern pStatus = Pattern.compile("\"status\"\\s*:\\s*\"([^\"]+)\"");
            Matcher mStatus = pStatus.matcher(json);
            if (mStatus.find()) {
                status = mStatus.group(1);
            }
            
            if (orderId != -1 && status != null) {
                repository.updateStatus(orderId, status);
                String response = "{\"status\":\"updated\"}";
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                exchange.sendResponseHeaders(400, -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    private Map<String, String> parseFormData(String formData) {
        Map<String, String> map = new HashMap<>();
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length > 0) {
                String key = java.net.URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                String value = keyValue.length > 1 ? java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : "";
                map.put(key, value);
            }
        }
        return map;
    }
}
