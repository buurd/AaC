package com.example.productmanagement;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactDirectory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.when;

@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(MockitoExtension.class)
@PactTestFor(providerName = "WarehouseService")
@PactDirectory("../pacts")
@Tag("pact-consumer")
public class ProductServiceWarehousePactTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TokenService tokenService;

    @Pact(consumer = "ProductManagementSystem")
    public V4Pact createPact(PactBuilder builder) {
        return builder
            .usingLegacyDsl()
            .given("Warehouse is up")
            .uponReceiving("A request to sync a product")
                .path("/api/products/sync")
                .method("POST")
                .headers("Content-Type", "application/json", "Authorization", "Bearer dummy-token")
                .body("{\"id\":1,\"name\":\"Pact Book\"}") // Warehouse only needs ID and Name
            .willRespondWith()
                .status(200)
                .headers(java.util.Map.of("Content-Type", "application/json"))
                .body("{\"status\":\"success\"}")
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "createPact")
    void testSyncProductToWarehouse(MockServer mockServer) throws IOException, SQLException {
        // Setup Mocks
        Product product = new Product();
        product.setId(1);
        product.setType("Book");
        product.setName("Pact Book");
        product.setDescription("A book about Pact");
        product.setPrice(29.99);
        product.setUnit("pcs");

        when(productRepository.findById(1)).thenReturn(product);
        when(tokenService.getAccessToken()).thenReturn(CompletableFuture.completedFuture("dummy-token"));

        // Initialize Service with MockServer URL for Warehouse
        String mockUrl = mockServer.getUrl() + "/api/products/sync";
        ProductService service = new ProductService(
            productRepository,
            "http://webshop-dummy", // Webshop URL (not tested here)
            mockUrl, // Warehouse URL -> MockServer
            tokenService
        );

        // Execute
        service.syncProduct(1);
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
