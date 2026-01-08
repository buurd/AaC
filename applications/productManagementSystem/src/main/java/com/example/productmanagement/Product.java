package com.example.productmanagement;

public class Product {
    private int id;
    private Integer groupId; // Nullable if standalone (though we aim for groups)
    private String type;
    private String name;
    private String description;
    private double price;
    private String unit;
    private String attributes; // JSON string

    public Product() {
    }

    public Product(int id, Integer groupId, String type, String name, String description, double price, String unit, String attributes) {
        this.id = id;
        this.groupId = groupId;
        this.type = type;
        this.name = name;
        this.description = description;
        this.price = price;
        this.unit = unit;
        this.attributes = attributes;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getGroupId() { return groupId; }
    public void setGroupId(Integer groupId) { this.groupId = groupId; }

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

    public String getAttributes() { return attributes; }
    public void setAttributes(String attributes) { this.attributes = attributes; }
}