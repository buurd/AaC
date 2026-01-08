package com.example.productmanagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    private ProductRepository mockRepository;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        mockRepository = mock(ProductRepository.class);
        // We don't need the sync functionality for this test, so we can pass nulls
        productService = new ProductService(mockRepository, null, null, null);
    }

    @Test
    void testGenerateVariants() throws SQLException {
        // 1. Arrange
        int groupId = 1;
        ProductGroup group = new ProductGroup(groupId, "Classic T-Shirt", "A cool t-shirt", 20.0, "pcs");
        
        // Mock repository calls
        when(mockRepository.findGroupById(groupId)).thenReturn(group);
        when(mockRepository.findByGroupId(groupId)).thenReturn(new ArrayList<>()); // No existing variants

        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("Color", List.of("Red", "Blue"));
        attributes.put("Size", List.of("M", "L"));

        // 2. Act
        productService.generateVariants(groupId, attributes);

        // 3. Assert
        // Verify that the repository's create method was called 4 times (2 colors * 2 sizes)
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(mockRepository, times(4)).create(productCaptor.capture());

        List<Product> createdProducts = productCaptor.getAllValues();
        assertEquals(4, createdProducts.size());

        // Check one of the created products to ensure its properties are set correctly
        Product redM = createdProducts.stream()
            .filter(p -> p.getAttributes().contains("\"Color\": \"Red\"") && p.getAttributes().contains("\"Size\": \"M\""))
            .findFirst()
            .orElse(null);
        
        assertEquals("Classic T-Shirt", redM.getName());
        assertEquals(20.0, redM.getPrice());
        assertEquals("{\"Color\": \"Red\", \"Size\": \"M\"}", redM.getAttributes());
    }

    @Test
    void testGenerateVariants_doesNotCreateDuplicates() throws SQLException {
        // 1. Arrange
        int groupId = 1;
        ProductGroup group = new ProductGroup(groupId, "Classic T-Shirt", "A cool t-shirt", 20.0, "pcs");
        
        // Simulate one variant already existing
        Product existingVariant = new Product();
        existingVariant.setAttributes("{\"Color\": \"Red\", \"Size\": \"M\"}");
        List<Product> existingVariants = List.of(existingVariant);

        when(mockRepository.findGroupById(groupId)).thenReturn(group);
        when(mockRepository.findByGroupId(groupId)).thenReturn(existingVariants);

        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("Color", List.of("Red"));
        attributes.put("Size", List.of("M", "L"));

        // 2. Act
        productService.generateVariants(groupId, attributes);

        // 3. Assert
        // Should only be called once for the new "L" size variant, not for the existing "M"
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(mockRepository, times(1)).create(productCaptor.capture());
        
        Product newProduct = productCaptor.getValue();
        assertEquals("{\"Color\": \"Red\", \"Size\": \"L\"}", newProduct.getAttributes());
    }
}
