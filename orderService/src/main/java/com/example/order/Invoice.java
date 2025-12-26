package com.example.order;

import java.time.LocalDate;

public class Invoice {
    private int id;
    private int orderId;
    private String customerName;
    private double amount;
    private LocalDate dueDate;
    private boolean paid;

    public Invoice() {}

    public Invoice(int orderId, String customerName, double amount, LocalDate dueDate) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.amount = amount;
        this.dueDate = dueDate;
        this.paid = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
}
