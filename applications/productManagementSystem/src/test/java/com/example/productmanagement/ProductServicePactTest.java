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
@PactTestFor(providerName = "WebshopService")
@PactDirectory("../../pacts")
@Tag("pact-consumer")
public class ProductServicePactTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TokenService tokenService;

    @Pact(consumer = "ProductManagementSystem")
    public V4Pact createPact(PactBuilder builder) {
        return builder
            .usingLegacyDsl()
            .given("Webshop is up")
            .uponReceiving("A request to sync a product")
                .path("/api/products/sync")
                .method("POST")
                .headers("Content-Type", "application/json", "Authorization", "Bearer dummy-token")
                .body("{\"id\":1,\"type\":\"Book\",\"name\":\"Pact Book\",\"description\":\"A book about Pact\",\"price\":29.99,\"unit\":\"pcs\"}")
            .willRespondWith()
                .status(200)
                .headers(java.util.Map.of("Content-Type", "application/json"))
                .body("{\"status\":\"success\"}")
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "createPact")
    void testSyncProduct(MockServer mockServer) throws IOException, SQLException {
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

        // Initialize Service with MockServer URL
        String mockUrl = mockServer.getUrl() + "/api/products/sync";
        ProductService service = new ProductService(
            productRepository,
            mockUrl, // Webshop URL -> MockServer
            "http://warehouse-dummy", // Warehouse URL (not tested here)
            tokenService
        );

        // Execute
        service.syncProduct(1);
        
        // We need to wait a bit because syncProduct is async (it uses thenAccept)
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
