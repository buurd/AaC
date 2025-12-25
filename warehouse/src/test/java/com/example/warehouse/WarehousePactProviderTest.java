package com.example.warehouse;

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

@Provider("WarehouseService")
@PactFolder("../../pacts")
@Tag("pact-provider")
public class WarehousePactProviderTest {

    private static HttpServer server;

    @BeforeAll
    static void start() throws IOException, SQLException {
        // Mock Repositories
        ProductRepository mockProductRepo = new ProductRepository(null) {
            @Override
            public void upsert(Product p) throws SQLException {
                System.out.println("Mock Repository: Upserted product " + p.getName());
            }
            @Override
            public Product findById(int id) throws SQLException {
                Product p = new Product();
                p.setId(id);
                p.setPmId(100 + id); // Dummy PM ID
                return p;
            }
        };
        
        DeliveryRepository mockDeliveryRepo = new DeliveryRepository(null) {
            @Override
            public boolean reserveStock(int productId, int quantity) throws SQLException {
                System.out.println("Mock Repository: Reserved " + quantity + " of product " + productId);
                return true; // Always succeed for this test
            }
            @Override
            public int countStock(int productId) throws SQLException {
                return 100; // Always return enough stock
            }
            @Override
            public void createFulfillmentOrder(int orderId) throws SQLException {
                System.out.println("Mock Repository: Created fulfillment order " + orderId);
            }
        };
        
        StockService mockStockService = new StockService(null, null) {
            @Override
            public void syncStock(int pmId, int stock) {
                System.out.println("Mock Service: Synced stock " + stock + " for PM ID " + pmId);
            }
        };

        // Start Server
        server = HttpServer.create(new InetSocketAddress(8082), 0);
        
        // Mount controllers directly
        server.createContext("/api/products/sync", new ProductSyncController(mockProductRepo));
        server.createContext("/api/stock/reserve", new StockReservationController(mockDeliveryRepo, mockProductRepo, mockStockService));
        server.createContext("/api/fulfillment/order", new OrderFulfillmentController(mockDeliveryRepo));

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Test Server started on port 8082");
    }

    @AfterAll
    static void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", 8082));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("Warehouse is up")
    public void toWarehouseIsUpState() {
    }
}
