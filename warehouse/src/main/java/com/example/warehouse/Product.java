package com.example.warehouse;

public class Product {
    private int id;
    private Integer pmId;
    private String name;

    public Product() {}

    public Product(int id, Integer pmId, String name) {
        this.id = id;
        this.pmId = pmId;
        this.name = name;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getPmId() { return pmId; }
    public void setPmId(Integer pmId) { this.pmId = pmId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
