package com.example.order;

import java.util.ArrayList;
import java.util.List;

public class Order {
    private int id;
    private String customerName;
    private String status;
    private List<OrderItem> items = new ArrayList<>();

    public Order() {}

    public Order(int id, String customerName, String status) {
        this.id = id;
        this.customerName = customerName;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    
    public void addItem(OrderItem item) {
        this.items.add(item);
    }
}
