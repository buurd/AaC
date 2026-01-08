package com.example.loyalty;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {

    @Test
    void testGettersAndSetters() {
        OrderItem item = new OrderItem();
        item.setProductId("p1");
        item.setQuantity(2);
        item.setPrice(10.0);
        item.setCategory("Books");

        assertEquals("p1", item.getProductId());
        assertEquals(2, item.getQuantity());
        assertEquals(10.0, item.getPrice());
        assertEquals("Books", item.getCategory());
    }

    @Test
    void testConstructor() {
        OrderItem item = new OrderItem("p2", 5, 20.0, "Electronics");
        
        assertEquals("p2", item.getProductId());
        assertEquals(5, item.getQuantity());
        assertEquals(20.0, item.getPrice());
        assertEquals("Electronics", item.getCategory());
    }
}
