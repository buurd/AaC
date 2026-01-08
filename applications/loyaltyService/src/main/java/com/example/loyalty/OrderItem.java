package com.example.loyalty;

public class OrderItem {
    private String productId;
    private int quantity;
    private double price;
    private String category;

    public OrderItem() {}

    public OrderItem(String productId, int quantity, double price, String category) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.category = category;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
