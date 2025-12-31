package com.example.order;

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
@PactTestFor(providerName = "WarehouseService")
@PactDirectory("../../pacts")
@Tag("pact-consumer")
public class OrderFulfillmentServicePactTest {

    @Mock
    private TokenService tokenService;

    @Pact(consumer = "OrderService")
    public V4Pact createPact(PactBuilder builder) {
        return builder
            .usingLegacyDsl()
            .given("Warehouse is up")
            .uponReceiving("A request to fulfill an order")
                .path("/api/fulfillment/order")
                .method("POST")
                .headers("Content-Type", "application/json", "Authorization", "Bearer dummy-token")
                .body("{\"orderId\":123}")
            .willRespondWith()
                .status(200)
                .headers(java.util.Map.of("Content-Type", "application/json"))
                .body("{\"status\":\"processing\"}")
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "createPact")
    void testNotifyOrderConfirmed(MockServer mockServer) throws IOException, ExecutionException, InterruptedException {
        // Setup Mocks
        when(tokenService.getAccessToken()).thenReturn(CompletableFuture.completedFuture("dummy-token"));

        // Initialize Service with MockServer URL
        String mockUrl = mockServer.getUrl() + "/api/fulfillment/order";
        OrderFulfillmentService service = new OrderFulfillmentService(mockUrl, tokenService);

        // Execute
        boolean result = service.notifyOrderConfirmed(123).get();
        
        assertTrue(result);
    }
}
