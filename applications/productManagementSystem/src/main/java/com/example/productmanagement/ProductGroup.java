package com.example.productmanagement;

public class ProductGroup {
    private int id;
    private String name;
    private String description;
    private double basePrice;
    private String baseUnit;

    public ProductGroup() {
    }

    public ProductGroup(int id, String name, String description, double basePrice, String baseUnit) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.baseUnit = baseUnit;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double basePrice) { this.basePrice = basePrice; }

    public String getBaseUnit() { return baseUnit; }
    public void setBaseUnit(String baseUnit) { this.baseUnit = baseUnit; }
}