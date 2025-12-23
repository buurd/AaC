package com.example.webshop;

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(MockitoExtension.class)
@PactTestFor(providerName = "OrderService")
@PactDirectory("../../pacts")
@Tag("pact-consumer")
public class OrderServicePactTest {

    @Mock
    private TokenService tokenService;

    @Pact(consumer = "Webshop")
    public V4Pact createPact(PactBuilder builder) {
        return builder
            .usingLegacyDsl()
            .given("Orders exist for customer")
            .uponReceiving("A request to get orders for a customer")
                .path("/api/orders")
                .query("customer=John Doe") // Expect decoded value
                .method("GET")
                .headers("Authorization", "Bearer dummy-token")
            .willRespondWith()
                .status(200)
                .headers(java.util.Map.of("Content-Type", "application/json"))
                .body("[{\"id\":1,\"customerName\":\"John Doe\",\"status\":\"CONFIRMED\"}]")
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "createPact")
    void testGetOrders(MockServer mockServer) throws IOException, ExecutionException, InterruptedException {
        // Setup Mocks
        when(tokenService.getAccessToken()).thenReturn(CompletableFuture.completedFuture("dummy-token"));

        // Initialize Service with MockServer URL
        String mockUrl = mockServer.getUrl() + "/api/orders";
        OrderService service = new OrderService(mockUrl, tokenService);

        // Execute
        String result = service.getOrdersForCustomer("John Doe").get();
        
        assertTrue(result.contains("John Doe"));
    }
}
