package com.example.webshop;

public class Product {
    private int id;
    private String type;
    private String name;
    private String description;
    private double price;
    private String unit;

    public Product(int id, String type, String name, String description, double price, String unit) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.description = description;
        this.price = price;
        this.unit = unit;
    }

    // Getters
    public int getId() { return id; }
    public String getType() { return type; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public String getUnit() { return unit; }
    
    @Override
    public String toString() {
        return String.format("Product{id=%d, name='%s', price=%.2f %s}", id, name, price, unit);
    }
}
