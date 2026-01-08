package com.example.warehouse;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    @Test
    void testProduct() {
        Product p = new Product();
        p.setId(1);
        p.setPmId(101);
        p.setName("Test Product");

        assertEquals(1, p.getId());
        assertEquals(101, p.getPmId());
        assertEquals("Test Product", p.getName());

        Product p2 = new Product(2, 102, "Product 2");
        assertEquals(2, p2.getId());
        assertEquals(102, p2.getPmId());
        assertEquals("Product 2", p2.getName());
    }

    @Test
    void testDelivery() {
        Delivery d = new Delivery();
        d.setId(1);
        d.setSender("Sender A");
        
        List<ProductIndividual> individuals = new ArrayList<>();
        d.setIndividuals(individuals);

        assertEquals(1, d.getId());
        assertEquals("Sender A", d.getSender());
        assertEquals(individuals, d.getIndividuals());

        Delivery d2 = new Delivery(2, "Sender B");
        assertEquals(2, d2.getId());
        assertEquals("Sender B", d2.getSender());
        
        ProductIndividual pi = new ProductIndividual();
        d2.addIndividual(pi);
        assertEquals(1, d2.getIndividuals().size());
        assertEquals(pi, d2.getIndividuals().get(0));
    }

    @Test
    void testFulfillmentOrder() {
        FulfillmentOrder fo = new FulfillmentOrder();
        fo.setId(1);
        fo.setOrderId(500);
        fo.setStatus("PENDING");

        assertEquals(1, fo.getId());
        assertEquals(500, fo.getOrderId());
        assertEquals("PENDING", fo.getStatus());

        FulfillmentOrder fo2 = new FulfillmentOrder(2, 501, "SHIPPED");
        assertEquals(2, fo2.getId());
        assertEquals(501, fo2.getOrderId());
        assertEquals("SHIPPED", fo2.getStatus());
    }

    @Test
    void testProductIndividual() {
        ProductIndividual pi = new ProductIndividual();
        pi.setId(1);
        pi.setDeliveryId(10);
        pi.setProductId(100);
        pi.setSerialNumber("SN123");
        pi.setState("NEW");

        assertEquals(1, pi.getId());
        assertEquals(10, pi.getDeliveryId());
        assertEquals(100, pi.getProductId());
        assertEquals("SN123", pi.getSerialNumber());
        assertEquals("NEW", pi.getState());

        ProductIndividual pi2 = new ProductIndividual(2, 20, 200, "SN456", "USED");
        assertEquals(2, pi2.getId());
        assertEquals(20, pi2.getDeliveryId());
        assertEquals(200, pi2.getProductId());
        assertEquals("SN456", pi2.getSerialNumber());
        assertEquals("USED", pi2.getState());
    }
}
