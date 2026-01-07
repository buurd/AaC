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
import org.mockito.Mockito;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Provider("OrderService")
@PactFolder("../pacts")
@Tag("pact-provider")
public class OrderPactProviderTest {

    private static HttpServer server;

    @BeforeAll
    static void start() throws IOException, SQLException {
        // Mock Repository using Mockito
        OrderRepository mockRepo = mock(OrderRepository.class);
        
        when(mockRepo.createOrder(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            System.out.println("Mock Repository: Created order " + order.getCustomerName());
            return 123; // Return dummy ID
        });

        when(mockRepo.findAll()).thenAnswer(invocation -> {
            List<Order> orders = new ArrayList<>();
            // Return ID 1 to match the existing Pact expectation
            Order o = new Order(1, "John Doe", "CONFIRMED");
            o.addItem(new OrderItem(1, 2));
            orders.add(o);
            return orders;
        });

        doAnswer(invocation -> {
            int orderId = invocation.getArgument(0);
            String status = invocation.getArgument(1);
            System.out.println("Mock Repository: Updated status for order " + orderId + " to " + status);
            return null;
        }).when(mockRepo).updateStatus(anyInt(), anyString());
        
        StockReservationService mockStockService = mock(StockReservationService.class);
        when(mockStockService.reserveStock(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(true));

        OrderFulfillmentService mockFulfillmentService = mock(OrderFulfillmentService.class);
        when(mockFulfillmentService.notifyOrderConfirmed(anyInt())).thenAnswer(invocation -> {
            int orderId = invocation.getArgument(0);
            System.out.println("Mock Service: Notified fulfillment for order " + orderId);
            return CompletableFuture.completedFuture(true);
        });

        CreditService mockCreditService = mock(CreditService.class);
        when(mockCreditService.checkCreditLimit(anyString())).thenReturn(true);

        InvoiceRepository mockInvoiceRepo = mock(InvoiceRepository.class);

        // Start Server
        server = HttpServer.create(new InetSocketAddress(8083), 0);
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

    @State("Order Service is up")
    public void toOrderServiceIsUpState() {
    }

    @State("Orders exist for customer")
    public void toOrdersExistForCustomerState() {
    }
}
