package com.example.warehouse;

public class ProductIndividual {
    private int id;
    private int deliveryId;
    private int productId;
    private String serialNumber;
    private String state;

    public ProductIndividual() {}

    public ProductIndividual(int id, int deliveryId, int productId, String serialNumber, String state) {
        this.id = id;
        this.deliveryId = deliveryId;
        this.productId = productId;
        this.serialNumber = serialNumber;
        this.state = state;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getDeliveryId() { return deliveryId; }
    public void setDeliveryId(int deliveryId) { this.deliveryId = deliveryId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}
