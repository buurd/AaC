package com.example.warehouse;

public class FulfillmentOrder {
    private int id;
    private int orderId;
    private String status;

    public FulfillmentOrder() {}

    public FulfillmentOrder(int id, int orderId, String status) {
        this.id = id;
        this.orderId = orderId;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
