package com.example.warehouse;

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

import static org.mockito.Mockito.when;

@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(MockitoExtension.class)
@PactTestFor(providerName = "WebshopService")
@PactDirectory("../pacts")
@Tag("pact-consumer")
public class StockServicePactTest {

    @Mock
    private TokenService tokenService;

    @Pact(consumer = "WarehouseService")
    public V4Pact createPact(PactBuilder builder) {
        return builder
            .usingLegacyDsl()
            .given("Webshop is up")
            .uponReceiving("A request to sync stock")
                .path("/api/stock/sync")
                .method("POST")
                .headers("Content-Type", "application/json", "Authorization", "Bearer dummy-token")
                .body("{\"pmId\":1,\"stock\":50}")
            .willRespondWith()
                .status(200)
                .headers(java.util.Map.of("Content-Type", "application/json"))
                .body("{\"status\":\"success\"}")
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "createPact")
    void testSyncStock(MockServer mockServer) throws IOException {
        // Setup Mocks
        when(tokenService.getAccessToken()).thenReturn(CompletableFuture.completedFuture("dummy-token"));

        // Initialize Service with MockServer URL
        String mockUrl = mockServer.getUrl() + "/api/stock/sync";
        StockService service = new StockService(mockUrl, tokenService);

        // Execute
        service.syncStock(1, 50);
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
