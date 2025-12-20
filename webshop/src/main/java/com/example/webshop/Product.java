package com.example.webshop;

public class Product {
    private int id;
    private Integer pmId;
    private String type;
    private String name;
    private String description;
    private double price;
    private String unit;

    public Product() {}

    public Product(int id, Integer pmId, String type, String name, String description, double price, String unit) {
        this.id = id;
        this.pmId = pmId;
        this.type = type;
        this.name = name;
        this.description = description;
        this.price = price;
        this.unit = unit;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public Integer getPmId() { return pmId; }
    public void setPmId(Integer pmId) { this.pmId = pmId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    @Override
    public String toString() {
        return String.format("Product{id=%d, pmId=%d, name='%s', price=%.2f %s}", id, pmId, name, price, unit);
    }
}
