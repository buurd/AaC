package com.example.webshop;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ShoppingCartController implements HttpHandler {

    private static final String CSS = 
        "body { font-family: Arial, Helvetica, sans-serif; background-color: #F8F9FA; color: #343A40; margin: 0; padding: 20px; }" +
        ".container { max-width: 1200px; margin: 0 auto; background-color: #FFFFFF; padding: 20px; border-radius: 4px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
        "h1 { color: #343A40; }" +
        "table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }" +
        "th { background-color: #007BFF; color: #FFFFFF; padding: 12px; text-align: left; }" +
        "td { padding: 12px; border-bottom: 1px solid #DEE2E6; }" +
        ".btn { display: inline-block; padding: 10px 20px; border-radius: 4px; text-decoration: none; color: #FFFFFF; font-weight: bold; border: none; cursor: pointer; }" +
        ".btn-danger { background-color: #DC3545; }" +
        ".btn-secondary { background-color: #6C757D; }" +
        ".btn-success { background-color: #28A745; }";

    private static final String JS = 
        "<script>" +
        "function getCookie(name) {" +
        "  const value = `; ${document.cookie}`;" +
        "  const parts = value.split(`; ${name}=`);" +
        "  if (parts.length === 2) return parts.pop().split(';').shift();" +
        "}" +
        "function renderCart() {" +
        "  let cart = JSON.parse(localStorage.getItem('cart')) || [];" +
        "  let tbody = document.getElementById('cart-body');" +
        "  tbody.innerHTML = '';" +
        "  if (cart.length === 0) {" +
        "    tbody.innerHTML = '<tr><td colspan=4>Your cart is empty.</td></tr>';" +
        "    document.getElementById('checkout-btn').style.display = 'none';" +
        "    return;" +
        "  }" +
        "  cart.forEach(item => {" +
        "    let row = `<tr><td>${item.name}</td><td>${item.quantity}</td><td>${(item.price * item.quantity).toFixed(2)}</td>" +
        "    <td><button onclick='removeFromCart(${item.id})' class='btn btn-danger'>Remove</button></td></tr>`;" +
        "    tbody.innerHTML += row;" +
        "  });" +
        "}" +
        "function removeFromCart(id) {" +
        "  let cart = JSON.parse(localStorage.getItem('cart')) || [];" +
        "  cart = cart.filter(i => i.id !== id);" +
        "  localStorage.setItem('cart', JSON.stringify(cart));" +
        "  renderCart();" +
        "}" +
        "function checkout() {" +
        "  let cart = JSON.parse(localStorage.getItem('cart')) || [];" +
        "  if (cart.length === 0) { alert('Cart is empty!'); return; }" +
        "  const token = getCookie('webshop_auth_token');" +
        "  if (!token) {" +
        "    alert('You must be logged in to place an order.');" +
        "    window.location.href = '/login';" + // Assuming a login page or redirect to Keycloak
        "    return;" +
        "  }" +
        "  const order = {" +
        "    customerName: 'John Doe'," + // Ideally, this should come from the token claims
        "    items: cart.map(i => ({ productId: i.id, quantity: i.quantity }))" +
        "  };" +
        "  fetch('/api/orders', {" +
        "    method: 'POST'," +
        "    headers: { " +
        "      'Content-Type': 'application/json'," +
        "      'Authorization': 'Bearer ' + token" +
        "    }," +
        "    body: JSON.stringify(order)" +
        "  }).then(res => {" +
        "    if (res.ok) {" +
        "      alert('Order placed successfully!');" +
        "      localStorage.removeItem('cart');" +
        "      window.location.href = '/products';" +
        "    } else {" +
        "      alert('Failed to place order. Insufficient stock?');" +
        "    }" +
        "  });" +
        "}" +
        "window.onload = renderCart;" +
        "</script>";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><style>").append(CSS).append("</style>");
        html.append(JS).append("</head><body>");
        html.append("<div class='container'>");
        html.append("<h1>Shopping Cart</h1>");
        html.append("<table>");
        html.append("<thead><tr><th>Product</th><th>Quantity</th><th>Price</th><th>Action</th></tr></thead>");
        html.append("<tbody id='cart-body'></tbody>");
        html.append("</table>");
        html.append("<button id='checkout-btn' onclick='checkout()' class='btn btn-success'>Checkout</button> ");
        html.append("<button onclick=\"window.location.href='/products'\" class='btn btn-secondary'>Back to Products</button>");
        html.append("</div></body></html>");
        
        String response = html.toString();
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
