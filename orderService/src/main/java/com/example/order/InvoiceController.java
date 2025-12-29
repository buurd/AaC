package com.example.order;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.net.URLDecoder;

public class InvoiceController implements HttpHandler {

    private final InvoiceRepository repository;
    private final OrderRepository orderRepository;

    public InvoiceController(InvoiceRepository repository, OrderRepository orderRepository) {
        this.repository = repository;
        this.orderRepository = orderRepository;
    }

    public InvoiceController(InvoiceRepository repository) {
        this(repository, null);
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
        ".btn-success { background-color: #28A745; }";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("/invoices".equals(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                handleListInvoices(exchange);
            } else if ("POST".equalsIgnoreCase(method)) {
                handleMarkPaid(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private void handleListInvoices(HttpExchange exchange) throws IOException {
        try {
            List<Invoice> invoices = repository.findAll();
            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html><html><head><style>").append(CSS).append("</style></head><body><div class='container'>");
            sb.append("<h1>Invoices</h1>");
            sb.append("<table><thead><tr><th>ID</th><th>Order ID</th><th>Customer</th><th>Amount</th><th>Due Date</th><th>Paid</th><th>Action</th></tr></thead><tbody>");
            
            for (Invoice i : invoices) {
                sb.append("<tr>");
                sb.append("<td>").append(i.getId()).append("</td>");
                sb.append("<td>").append(i.getOrderId()).append("</td>");
                sb.append("<td>").append(i.getCustomerName()).append("</td>");
                sb.append("<td>").append(i.getAmount()).append("</td>");
                sb.append("<td>").append(i.getDueDate()).append("</td>");
                sb.append("<td>").append(i.isPaid() ? "Yes" : "No").append("</td>");
                sb.append("<td>");
                if (!i.isPaid()) {
                    sb.append("<form action='/invoices' method='post' style='margin:0;'>");
                    sb.append("<input type='hidden' name='id' value='").append(i.getId()).append("'>");
                    sb.append("<button type='submit' class='btn btn-success'>Mark Paid</button>");
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

    private void handleMarkPaid(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        String formData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = parseFormData(formData);
        
        try {
            int id = Integer.parseInt(params.get("id"));
            markPaid(id);
            
            // Redirect back to list
            exchange.getResponseHeaders().set("Location", "/invoices");
            exchange.sendResponseHeaders(302, -1);
        } catch (Exception e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, -1);
        }
    }

    // Method required by REQ-066
    public void markPaid(int invoiceId) throws SQLException {
        repository.markPaid(invoiceId);
        
        // Update Order Status to PAID
        if (orderRepository != null) {
            Invoice invoice = repository.findById(invoiceId);
            if (invoice != null) {
                orderRepository.updateStatus(invoice.getOrderId(), "PAID");
            }
        }
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
