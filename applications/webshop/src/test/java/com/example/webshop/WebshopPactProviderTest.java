package com.example.webshop;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.concurrent.Executors;

@Provider("WebshopService")
@PactFolder("../../pacts") // Point to the shared pacts directory
@Tag("pact-provider")
public class WebshopPactProviderTest {

    private static HttpServer server;

    @BeforeAll
    static void start() throws IOException, SQLException {
        // Mock Repository
        ProductRepository mockRepo = new ProductRepository(null) {
            @Override
            public void upsert(Product p) throws SQLException {
                System.out.println("Mock Repository: Upserted product " + p.getName());
            }
            
            @Override
            public void updateStock(int pmId, int stock) throws SQLException {
                System.out.println("Mock Repository: Updated stock for product " + pmId + " to " + stock);
            }
        };

        // Start Server
        server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        // Mount controllers directly
        server.createContext("/api/products/sync", new ProductSyncController(mockRepo));
        server.createContext("/api/stock/sync", new StockSyncController(mockRepo));

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Test Server started on port 8080");
    }

    @AfterAll
    static void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", 8080));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("Webshop is up")
    public void toWebshopIsUpState() {
        // Prepare service state if needed
    }
}
