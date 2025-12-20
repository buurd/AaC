package com.example.warehouse;

import java.util.ArrayList;
import java.util.List;

public class Delivery {
    private int id;
    private String sender;
    private List<ProductIndividual> individuals = new ArrayList<>();

    public Delivery() {}

    public Delivery(int id, String sender) {
        this.id = id;
        this.sender = sender;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public List<ProductIndividual> getIndividuals() { return individuals; }
    public void setIndividuals(List<ProductIndividual> individuals) { this.individuals = individuals; }
    
    public void addIndividual(ProductIndividual individual) {
        this.individuals.add(individual);
    }
}
