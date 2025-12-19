package com.example.webshop;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class ProductController implements HttpHandler {

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
        ".btn-primary { background-color: #007BFF; }";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            List<Product> products = repository.findAll();
            
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><style>").append(CSS).append("</style></head><body>");
            html.append("<div class='container'>");
            html.append("<h1>Webshop Products</h1>");
            html.append("<table>");
            html.append("<thead><tr><th>ID</th><th>Name</th><th>Description</th><th>Price</th><th>Unit</th></tr></thead>");
            html.append("<tbody>");
            
            for (Product p : products) {
                html.append("<tr>");
                html.append("<td>").append(p.getId()).append("</td>");
                html.append("<td>").append(p.getName()).append("</td>");
                html.append("<td>").append(p.getDescription()).append("</td>");
                html.append("<td>").append(String.format("%.2f", p.getPrice())).append("</td>");
                html.append("<td>").append(p.getUnit()).append("</td>");
                html.append("</tr>");
            }
            
            html.append("</tbody></table>");
            html.append("</div></body></html>");
            
            String response = html.toString();
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            String errorResponse = "Internal Server Error";
            exchange.sendResponseHeaders(500, errorResponse.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorResponse.getBytes());
            }
        }
    }
}
