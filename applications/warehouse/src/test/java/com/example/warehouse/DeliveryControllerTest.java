package com.example.warehouse;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DeliveryControllerTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockService stockService;

    @Mock
    private HttpExchange exchange;

    private DeliveryController controller;
    private Headers responseHeaders;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new DeliveryController(deliveryRepository, productRepository, stockService);
        responseHeaders = new Headers();
        responseBody = new ByteArrayOutputStream();

        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        when(exchange.getResponseBody()).thenReturn(responseBody);
    }

    @Test
    void testListDeliveries() throws IOException, SQLException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/deliveries"));
        
        Delivery d1 = new Delivery(1, "Sender A");
        when(deliveryRepository.findAll()).thenReturn(Collections.singletonList(d1));

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        assertTrue(responseBody.toString().contains("Sender A"));
    }

    @Test
    void testCreateDelivery() throws IOException, SQLException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(URI.create("/deliveries"));
        String body = "sender=New+Sender";
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));

        controller.handle(exchange);

        verify(deliveryRepository).createDelivery("New Sender");
        verify(exchange).sendResponseHeaders(eq(302), eq(-1L));
        assertEquals("/deliveries", responseHeaders.getFirst("Location"));
    }

    @Test
    void testViewDelivery() throws IOException, SQLException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/deliveries/view?id=1"));
        
        Delivery d = new Delivery(1, "Sender A");
        ProductIndividual pi = new ProductIndividual(1, 1, 100, "SN1", "New");
        d.addIndividual(pi);
        
        when(deliveryRepository.findById(1)).thenReturn(d);
        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        when(productRepository.findById(100)).thenReturn(new Product(100, 1001, "Product X"));

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("Delivery #1"));
        assertTrue(response.contains("Product X"));
        assertTrue(response.contains("SN1"));
    }

    @Test
    void testAddItem() throws IOException, SQLException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(URI.create("/deliveries/add-item"));
        String body = "deliveryId=1&productId=100&serialNumber=SN2&state=New";
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        
        when(deliveryRepository.countStock(100)).thenReturn(5);
        when(productRepository.findById(100)).thenReturn(new Product(100, 1001, "Product X"));

        controller.handle(exchange);

        verify(deliveryRepository).addIndividual(any(ProductIndividual.class));
        verify(stockService).syncStock(1001, 5);
        verify(exchange).sendResponseHeaders(eq(302), eq(-1L));
        assertEquals("/deliveries/view?id=1", responseHeaders.getFirst("Location"));
    }

    @Test
    void testReturnDelivery() throws IOException, SQLException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(URI.create("/deliveries/return"));
        String body = "id=1";
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));

        controller.handle(exchange);

        verify(deliveryRepository).deleteDelivery(1);
        verify(exchange).sendResponseHeaders(eq(302), eq(-1L));
        assertEquals("/deliveries", responseHeaders.getFirst("Location"));
    }
    
    @Test
    void testNotFound() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/unknown"));

        controller.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(404), eq(-1L));
    }
}
