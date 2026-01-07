package com.example.loyalty;

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
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Provider("LoyaltyService")
@PactFolder("../pacts")
@Tag("pact-provider")
public class LoyaltyPactProviderTest {

    private static HttpServer server;

    @BeforeAll
    static void start() throws IOException {
        // Mock Dependencies
        PointService mockPointService = mock(PointService.class);
        BonusRuleEngine mockRuleEngine = mock(BonusRuleEngine.class);

        // Setup Mocks
        when(mockPointService.redeemPoints(anyString(), anyInt())).thenReturn(true);
        
        // Default for user1 (OrderService test expects 50 remaining)
        when(mockPointService.getBalance("user1")).thenReturn(50);
        
        // For user2 (Webshop test expects 100)
        when(mockPointService.getBalance("user2")).thenReturn(100);

        // Start Server
        server = HttpServer.create(new InetSocketAddress(8084), 0);
        server.createContext("/", new LoyaltyController(mockPointService, mockRuleEngine));
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Test Server started on port 8084");
    }

    @AfterAll
    static void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", 8084));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("Loyalty Service is up")
    public void toLoyaltyServiceIsUpState() {
    }
}
