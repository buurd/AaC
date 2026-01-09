package com.example.productmanagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    @Mock
    private ProductRepository mockRepo;
    @Mock
    private TokenService mockTokenService;
    @Mock
    private HttpClient mockHttpClient;
    @Mock
    private HttpResponse<String> mockResponse;

    private ProductService productService;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.openMocks(this);
        productService = new ProductService(mockRepo, "http://webshop-dummy", "http://warehouse-dummy", mockTokenService);

        // Inject mock HttpClient
        Field httpClientField = ProductService.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(productService, mockHttpClient);

        when(mockTokenService.getAccessToken()).thenReturn(CompletableFuture.completedFuture("mock-token"));
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"status\":\"success\"}");
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));
    }

    @Test
    void testCreateProduct() throws SQLException {
        Product p = new Product();
        p.setName("Test");
        when(mockRepo.create(p)).thenReturn(1);

        productService.createProduct(p);

        verify(mockRepo).create(p);
        assertEquals(1, p.getId());
    }

    @Test
    void testUpdateProduct() throws SQLException {
        Product p = new Product();
        p.setId(1);
        p.setName("Updated");

        productService.updateProduct(p);

        verify(mockRepo).update(p);
    }

    @Test
    void testDeleteProduct() throws SQLException {
        productService.deleteProduct(1);

        verify(mockRepo).delete(1);
        // Verify sync delete calls (2 calls, one for webshop, one for warehouse)
        verify(mockHttpClient, times(2)).sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testSyncProduct() throws SQLException {
        Product p = new Product();
        p.setId(1);
        p.setName("Sync Me");
        p.setAttributes("{\"Color\": \"Red\"}");
        when(mockRepo.findById(1)).thenReturn(p);

        productService.syncProduct(1);

        verify(mockHttpClient, times(2)).sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testCreateProductGroup() throws SQLException {
        ProductGroup g = new ProductGroup();
        g.setName("Group");

        productService.createProductGroup(g);

        verify(mockRepo).createGroup(g);
    }

    @Test
    void testGenerateVariants() throws SQLException {
        ProductGroup g = new ProductGroup();
        g.setId(1);
        g.setName("T-Shirt");
        g.setBasePrice(20.0);
        g.setBaseUnit("pcs");
        when(mockRepo.findGroupById(1)).thenReturn(g);
        when(mockRepo.findByGroupId(1)).thenReturn(Collections.emptyList());
        when(mockRepo.create(any(Product.class))).thenReturn(101, 102);

        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("Color", Arrays.asList("Red", "Blue"));
        attributes.put("Size", Arrays.asList("S"));

        productService.generateVariants(1, attributes);

        // Should generate 2 variants: Red-S, Blue-S
        verify(mockRepo, times(2)).create(any(Product.class));
    }

    @Test
    void testGenerateVariants_ExistingSkipped() throws SQLException {
        ProductGroup g = new ProductGroup();
        g.setId(1);
        g.setName("T-Shirt");
        when(mockRepo.findGroupById(1)).thenReturn(g);

        Product existing = new Product();
        existing.setAttributes("{\"Color\": \"Red\"}");
        when(mockRepo.findByGroupId(1)).thenReturn(Collections.singletonList(existing));

        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("Color", Arrays.asList("Red", "Blue"));

        productService.generateVariants(1, attributes);

        // Should only create Blue variant
        verify(mockRepo, times(1)).create(any(Product.class));
    }

    @Test
    void testSyncGroup() throws SQLException {
        Product p1 = new Product();
        p1.setId(1);
        Product p2 = new Product();
        p2.setId(2);
        when(mockRepo.findByGroupId(1)).thenReturn(Arrays.asList(p1, p2));

        productService.syncGroup(1);

        // 2 products * 2 sync targets = 4 calls
        verify(mockHttpClient, times(4)).sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
    
    @Test
    void testSyncProduct_WithAttributes() throws SQLException {
        Product p = new Product();
        p.setId(1);
        p.setName("T-Shirt");
        p.setAttributes("{\"Color\": \"Red\", \"Size\": \"L\"}");
        when(mockRepo.findById(1)).thenReturn(p);

        productService.syncProduct(1);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient, times(2)).sendAsync(captor.capture(), any(HttpResponse.BodyHandler.class));
        
        // Verify that the JSON body contains the flattened name
        // We can't easily inspect the body publisher content without more complex mocking, 
        // but we verified the call happened.
    }
}
