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
        ".btn { display: inline-block; padding: 10px 20px; border-radius: 4px; text-decoration: none; color: #FFFFFF; font-weight: bold; border: none; cursor: pointer; margin-right: 10px; }" +
        ".btn-danger { background-color: #DC3545; }" +
        ".btn-secondary { background-color: #6C757D; }" +
        ".btn-success { background-color: #28A745; }" +
        ".btn-primary { background-color: #007BFF; }" +
        ".loyalty-section { background-color: #E9ECEF; padding: 15px; border-radius: 4px; margin-bottom: 20px; }";

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
        "  let total = 0;" +
        "  if (cart.length === 0) {" +
        "    tbody.innerHTML = '<tr><td colspan=4>Your cart is empty.</td></tr>';" +
        "    document.getElementById('checkout-btn').style.display = 'none';" +
        "    document.getElementById('potential-loyalty').innerText = '0';" +
        "    fetchLoyaltyBalance();" +
        "    return;" +
        "  }" +
        "  cart.forEach(item => {" +
        "    let itemTotal = item.price * item.quantity;" +
        "    total += itemTotal;" +
        "    let row = `<tr><td>${item.name}</td><td>${item.quantity}</td><td>${itemTotal.toFixed(2)}</td>" +
        "    <td><button onclick='removeFromCart(${item.id})' class='btn btn-danger'>Remove</button></td></tr>`;" +
        "    tbody.innerHTML += row;" +
        "  });" +
        "  document.getElementById('cart-total').innerText = total.toFixed(2);" +
        "  calculatePotentialPoints(cart, total);" +
        "  fetchLoyaltyBalance();" +
        "}" +
        "function calculatePotentialPoints(cart, total) {" +
        "  const order = {" +
        "    totalAmount: total," +
        "    items: cart.map(i => ({ productId: i.id, quantity: i.quantity }))" +
        "  };" +
        "  fetch('/api/loyalty/calculate', {" +
        "    method: 'POST'," +
        "    headers: { 'Content-Type': 'application/json' }," +
        "    body: JSON.stringify(order)" +
        "  }).then(res => res.json())" +
        "  .then(data => {" +
        "    document.getElementById('potential-loyalty').innerText = data.points;" +
        "  }).catch(err => console.error('Failed to calculate points', err));" +
        "}" +
        "function fetchLoyaltyBalance() {" +
        "  const token = getCookie('webshop_auth_token');" +
        "  const username = getCookie('webshop_username');" +
        "  if (!token || !username) return;" +
        "  fetch('/api/loyalty/balance/' + username, {" +
        "    headers: { 'Authorization': 'Bearer ' + token }" +
        "  }).then(res => res.json())" +
        "  .then(data => {" +
        "    document.getElementById('loyalty-points').innerText = data.points;" +
        "    document.getElementById('loyalty-value').innerText = data.value.toFixed(2);" +
        "    document.getElementById('max-points').value = data.points;" +
        "    document.getElementById('loyalty-section').style.display = 'block';" +
        "  }).catch(err => console.error('Failed to fetch loyalty balance', err));" +
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
        "  const username = getCookie('webshop_username');" +
        "  if (!token) {" +
        "    alert('You must be logged in to place an order.');" +
        "    window.location.href = '/login';" + 
        "    return;" +
        "  }" +
        "  let pointsToRedeem = 0;" +
        "  if (document.getElementById('use-points').checked) {" +
        "      pointsToRedeem = parseInt(document.getElementById('points-input').value) || 0;" +
        "  }" +
        "  let total = 0;" +
        "  cart.forEach(item => {" +
        "    total += item.price * item.quantity;" +
        "  });" +
        "  const order = {" +
        "    customerName: username," + 
        "    pointsToRedeem: pointsToRedeem," +
        "    totalAmount: total," +
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
        "      alert('Failed to place order. Insufficient stock or points?');" +
        "    }" +
        "  });" +
        "}" +
        "window.onload = renderCart;" +
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
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><style>").append(CSS).append("</style>");
        html.append(JS).append("</head><body>");
        html.append("<div class='container'>");
        html.append(getHeader());
        html.append("<h1>Shopping Cart</h1>");
        html.append("<table>");
        html.append("<thead><tr><th>Product</th><th>Quantity</th><th>Price</th><th>Action</th></tr></thead>");
        html.append("<tbody id='cart-body'></tbody>");
        html.append("</table>");
        html.append("<h3>Total: <span id='cart-total'>0.00</span> EUR</h3>");
        html.append("<h4>Potential Loyalty Points: <span id='potential-loyalty'>0</span></h4>");
        
        html.append("<div id='loyalty-section' class='loyalty-section' style='display:none;'>");
        html.append("<h4>Loyalty Program</h4>");
        html.append("<p>You have <strong id='loyalty-points'>0</strong> points (Value: <strong id='loyalty-value'>0.00</strong> EUR).</p>");
        html.append("<label><input type='checkbox' id='use-points'> Pay with Points</label>");
        html.append("<input type='number' id='points-input' placeholder='Points to redeem' min='0'>");
        html.append("<input type='hidden' id='max-points'>");
        html.append("</div>");

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
