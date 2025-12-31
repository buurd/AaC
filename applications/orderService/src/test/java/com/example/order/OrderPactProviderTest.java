package com.example.order;

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

@Provider("OrderService")
@PactFolder("../../pacts")
@Tag("pact-provider")
public class OrderPactProviderTest {

    private static HttpServer server;

    @BeforeAll
    static void start() throws IOException, SQLException {
        // Mock Repository
        OrderRepository mockRepo = new OrderRepository(null) {
            @Override
            public List<Order> findAll() throws SQLException {
                List<Order> orders = new ArrayList<>();
                Order o = new Order(1, "John Doe", "CONFIRMED");
                orders.add(o);
                return orders;
            }
        };
        
        StockReservationService mockStockService = new StockReservationService(null, null);
        OrderFulfillmentService mockFulfillmentService = new OrderFulfillmentService(null, null);
        
        // Mock Invoice Repository
        InvoiceRepository mockInvoiceRepo = new InvoiceRepository(null) {
            @Override
            public void createInvoice(Invoice invoice) throws SQLException {
                // No-op
            }
        };
        
        // Mock Credit Service
        CreditService mockCreditService = new CreditService(mockInvoiceRepo) {
            @Override
            public boolean checkCreditLimit(String customerName) {
                return true;
            }
            @Override
            public boolean checkOverdueInvoices(String customerName) {
                return true;
            }
        };

        // Start Server
        server = HttpServer.create(new InetSocketAddress(8083), 0);
        
        // Mount controller directly
        server.createContext("/api/orders", new OrderController(mockRepo, mockStockService, mockFulfillmentService, mockCreditService, mockInvoiceRepo));
        
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Test Server started on port 8083");
    }

    @AfterAll
    static void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", 8083));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("Orders exist for customer")
    public void toOrdersExistState() {
    }
}
