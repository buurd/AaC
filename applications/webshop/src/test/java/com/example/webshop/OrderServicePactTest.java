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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(MockitoExtension.class)
@PactTestFor(providerName = "OrderService")
@PactDirectory("../pacts")
@Tag("pact-consumer")
public class OrderServicePactTest {

    @Mock
    private TokenService tokenService;

    @Pact(consumer = "Webshop")
    public V4Pact createPact(PactBuilder builder) {
        return builder
            .usingLegacyDsl()
            .given("Order Service is up")
            .uponReceiving("A request to create an order")
                .path("/api/orders")
                .method("POST")
                .headers("Content-Type", "application/json", "Authorization", "Bearer dummy-token")
                .body("{\"customerName\":\"John Doe\",\"items\":[{\"productId\":1,\"quantity\":2}]}")
            .willRespondWith()
                .status(200)
                .headers(java.util.Map.of("Content-Type", "application/json"))
                .body("{\"status\":\"PENDING_CONFIRMATION\",\"orderId\":123}")
            .given("Orders exist for customer")
            .uponReceiving("A request to get orders for a customer")
                .path("/api/orders")
                .method("GET")
                .query("customer=John Doe")
                .headers("Authorization", "Bearer dummy-token")
            .willRespondWith()
                .status(200)
                .headers(java.util.Map.of("Content-Type", "application/json"))
                .body("[{\"id\":123,\"customerName\":\"John Doe\",\"status\":\"CONFIRMED\"}]")
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "createPact")
    void testOrderInteractions(MockServer mockServer) throws IOException, ExecutionException, InterruptedException {
        // Setup Mocks
        when(tokenService.getAccessToken()).thenReturn(CompletableFuture.completedFuture("dummy-token"));

        // Initialize Service with MockServer URL
        String mockUrl = mockServer.getUrl() + "/api/orders";
        OrderService service = new OrderService(mockUrl, tokenService);

        // Test Create Order
        String orderJson = "{\"customerName\":\"John Doe\",\"items\":[{\"productId\":1,\"quantity\":2}]}";
        service.createOrder(orderJson).get();
        
        // Test Get Orders
        String response = service.getOrdersForCustomer("John Doe").get();
        assertEquals("[{\"id\":123,\"customerName\":\"John Doe\",\"status\":\"CONFIRMED\"}]", response);
    }
}
