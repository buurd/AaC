package com.example.productmanagement;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductController implements HttpHandler {

    private final ProductRepository repository;
    private final ProductService productService;

    public ProductController(ProductRepository repository, ProductService productService) {
        this.repository = repository;
        this.productService = productService;
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
        ".btn-secondary { background-color: #6C757D; }" +
        ".btn-danger { background-color: #DC3545; }" +
        "input[type='text'], input[type='number'], input[type='submit'] { padding: 8px; border: 1px solid #CED4DA; border-radius: 4px; width: 100%; box-sizing: border-box; margin-bottom: 10px; }" +
        "label { display: block; font-weight: bold; margin-bottom: 5px; }";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/products".equals(path) && "GET".equalsIgnoreCase(method)) {
            handleList(exchange);
        } else if ("/products/create".equals(path)) {
            if ("GET".equalsIgnoreCase(method)) {
                handleShowCreateForm(exchange);
            } else if ("POST".equalsIgnoreCase(method)) {
                handleCreate(exchange);
            }
        } else if (path.startsWith("/products/edit")) {
            if ("GET".equalsIgnoreCase(method)) {
                handleShowEditForm(exchange);
            } else if ("POST".equalsIgnoreCase(method)) {
                handleEdit(exchange);
            }
        } else if (path.startsWith("/products/delete")) {
            if ("GET".equalsIgnoreCase(method)) {
                handleShowDeleteForm(exchange);
            } else if ("POST".equalsIgnoreCase(method)) {
                handleDelete(exchange);
            }
        } else if (path.startsWith("/products/sync")) {
            if ("GET".equalsIgnoreCase(method)) {
                handleSync(exchange);
            }
        } else {
            sendResponse(exchange, 404, "Not Found");
        }
    }

    private void handleList(HttpExchange exchange) throws IOException {
        try {
            List<Product> products = repository.findAll();
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><style>").append(CSS).append("</style></head><body><div class='container'>");
            html.append("<h1>Products</h1>");
            html.append("<a href='/products/create' class='btn btn-primary'>Create New Product</a>");
            html.append("<a href='/logout' class='btn btn-secondary' style='float: right;'>Logout</a>");
            html.append("<table><thead><tr><th>ID</th><th>Name</th><th>Price</th><th>Actions</th></tr></thead><tbody>");
            for (Product p : products) {
                html.append("<tr>");
                html.append("<td>").append(p.getId()).append("</td>");
                html.append("<td>").append(p.getName()).append("</td>");
                html.append("<td>").append(String.format("%.2f %s", p.getPrice(), p.getUnit())).append("</td>");
                html.append("<td>");
                html.append("<a href='/products/edit?id=").append(p.getId()).append("' class='btn btn-secondary'>Edit</a>");
                html.append("<a href='/products/delete?id=").append(p.getId()).append("' class='btn btn-danger'>Delete</a>");
                html.append("<a href='/products/sync?id=").append(p.getId()).append("' class='btn btn-primary'>Sync</a>");
                html.append("</td>");
                html.append("</tr>");
            }
            html.append("</tbody></table></div></body></html>");
            sendResponse(exchange, 200, html.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Database error");
        }
    }

    private void handleShowCreateForm(HttpExchange exchange) throws IOException {
        String form = "<h1>Create Product</h1>" +
                      "<form action='/products/create' method='post'>" +
                      "<label>Type:</label><input type='text' name='type' value='Book'><br>" +
                      "<label>Name:</label><input type='text' name='name'><br>" +
                      "<label>Description:</label><input type='text' name='description'><br>" +
                      "<label>Price:</label><input type='number' step='0.01' name='price'><br>" +
                      "<label>Unit:</label><input type='text' name='unit' value='pcs'><br>" +
                      "<button type='submit' class='btn btn-primary'>Create</button>" +
                      "</form>";
        sendResponse(exchange, 200, wrapInContainer(form));
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        try {
            Map<String, String> params = parseFormData(exchange.getRequestBody());
            Product p = new Product();
            p.setType(params.get("type"));
            p.setName(params.get("name"));
            p.setDescription(params.get("description"));
            p.setPrice(Double.parseDouble(params.get("price")));
            p.setUnit(params.get("unit"));
            productService.createProduct(p);
            redirect(exchange, "/products");
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Failed to create product");
        }
    }

    private void handleShowEditForm(HttpExchange exchange) throws IOException {
        try {
            int id = Integer.parseInt(getQueryParam(exchange.getRequestURI().getQuery(), "id"));
            Product p = repository.findById(id);
            if (p == null) {
                sendResponse(exchange, 404, "Product not found");
                return;
            }
            String form = "<h1>Edit Product</h1>" +
                          "<form action='/products/edit' method='post'>" +
                          "<input type='hidden' name='id' value='" + p.getId() + "'>" +
                          "<label>Type:</label><input type='text' name='type' value='" + p.getType() + "'><br>" +
                          "<label>Name:</label><input type='text' name='name' value='" + p.getName() + "'><br>" +
                          "<label>Description:</label><input type='text' name='description' value='" + p.getDescription() + "'><br>" +
                          "<label>Price:</label><input type='number' step='0.01' name='price' value='" + p.getPrice() + "'><br>" +
                          "<label>Unit:</label><input type='text' name='unit' value='" + p.getUnit() + "'><br>" +
                          "<button type='submit' class='btn btn-primary'>Update</button>" +
                          "</form>";
            sendResponse(exchange, 200, wrapInContainer(form));
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Server error");
        }
    }

    private void handleEdit(HttpExchange exchange) throws IOException {
        try {
            Map<String, String> params = parseFormData(exchange.getRequestBody());
            Product p = new Product();
            p.setId(Integer.parseInt(params.get("id")));
            p.setType(params.get("type"));
            p.setName(params.get("name"));
            p.setDescription(params.get("description"));
            p.setPrice(Double.parseDouble(params.get("price")));
            p.setUnit(params.get("unit"));
            productService.updateProduct(p);
            redirect(exchange, "/products");
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Failed to update product");
        }
    }

    private void handleShowDeleteForm(HttpExchange exchange) throws IOException {
        try {
            int id = Integer.parseInt(getQueryParam(exchange.getRequestURI().getQuery(), "id"));
            Product p = repository.findById(id);
            if (p == null) {
                sendResponse(exchange, 404, "Product not found");
                return;
            }
            String form = "<h1>Delete Product</h1>" +
                          "<p>Are you sure you want to delete '" + p.getName() + "'?</p>" +
                          "<form action='/products/delete' method='post'>" +
                          "<input type='hidden' name='id' value='" + p.getId() + "'>" +
                          "<button type='submit' class='btn btn-danger'>Delete</button>" +
                          "<a href='/products' class='btn btn-secondary'>Cancel</a>" +
                          "</form>";
            sendResponse(exchange, 200, wrapInContainer(form));
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Server error");
        }
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        try {
            Map<String, String> params = parseFormData(exchange.getRequestBody());
            int id = Integer.parseInt(params.get("id"));
            productService.deleteProduct(id);
            redirect(exchange, "/products");
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Failed to delete product");
        }
    }

    private void handleSync(HttpExchange exchange) throws IOException {
        try {
            int id = Integer.parseInt(getQueryParam(exchange.getRequestURI().getQuery(), "id"));
            productService.syncProduct(id);
            redirect(exchange, "/products");
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Failed to sync product");
        }
    }

    private String getQueryParam(String query, String paramName) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1 && pair[0].equals(paramName)) {
                return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private Map<String, String> parseFormData(InputStream is) throws IOException {
        String formData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> map = new HashMap<>();
        for (String pair : formData.split("&")) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                map.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8), URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private String wrapInContainer(String content) {
        return "<!DOCTYPE html><html><head><style>" + CSS + "</style></head><body><div class='container'>" + content + "</div></body></html>";
    }
}
