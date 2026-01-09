package com.example.productmanagement;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProductControllerTest {

    @Mock
    private ProductRepository mockRepo;
    @Mock
    private ProductService mockService;
    @Mock
    private HttpExchange mockExchange;

    private ProductController controller;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new ProductController(mockRepo, mockService);
        responseBody = new ByteArrayOutputStream();
        when(mockExchange.getResponseBody()).thenReturn(responseBody);
        when(mockExchange.getResponseHeaders()).thenReturn(new Headers());
    }

    @Test
    void testHandleList_ReturnsHtml() throws IOException, SQLException {
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/products"));

        Product p1 = new Product();
        p1.setId(1);
        p1.setName("Test Product");
        p1.setPrice(10.0);
        p1.setUnit("pcs");
        when(mockRepo.findAll()).thenReturn(Collections.singletonList(p1));

        controller.handle(mockExchange);

        verify(mockExchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("Products"));
        assertTrue(response.contains("Test Product"));
    }

    @Test
    void testHandleShowCreateForm() throws IOException {
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/products/create"));

        controller.handle(mockExchange);

        verify(mockExchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("Create Product"));
    }

    @Test
    void testHandleCreate_Success() throws IOException, SQLException {
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/products/create"));
        String formData = "type=Book&name=New+Book&description=Desc&price=20.0&unit=pcs";
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(formData.getBytes(StandardCharsets.UTF_8)));

        controller.handle(mockExchange);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(mockService).createProduct(captor.capture());
        assertEquals("New Book", captor.getValue().getName());
        verify(mockExchange).sendResponseHeaders(eq(302), anyLong());
    }

    @Test
    void testHandleShowEditForm_Success() throws IOException, SQLException {
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/products/edit?id=1"));
        
        Product p = new Product();
        p.setId(1);
        p.setName("Edit Me");
        p.setPrice(10.0);
        p.setUnit("pcs");
        when(mockRepo.findById(1)).thenReturn(p);

        controller.handle(mockExchange);

        verify(mockExchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("Edit Product"));
        assertTrue(response.contains("Edit Me"));
    }

    @Test
    void testHandleEdit_Success() throws IOException, SQLException {
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/products/edit"));
        String formData = "id=1&type=Book&name=Updated&description=Desc&price=25.0&unit=pcs";
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(formData.getBytes(StandardCharsets.UTF_8)));

        controller.handle(mockExchange);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(mockService).updateProduct(captor.capture());
        assertEquals("Updated", captor.getValue().getName());
        verify(mockExchange).sendResponseHeaders(eq(302), anyLong());
    }

    @Test
    void testHandleShowDeleteForm_Success() throws IOException, SQLException {
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/products/delete?id=1"));
        
        Product p = new Product();
        p.setId(1);
        p.setName("Delete Me");
        when(mockRepo.findById(1)).thenReturn(p);

        controller.handle(mockExchange);

        verify(mockExchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("Delete Product"));
        assertTrue(response.contains("Delete Me"));
    }

    @Test
    void testHandleDelete_Success() throws IOException, SQLException {
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/products/delete"));
        String formData = "id=1";
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(formData.getBytes(StandardCharsets.UTF_8)));

        controller.handle(mockExchange);

        verify(mockService).deleteProduct(1);
        verify(mockExchange).sendResponseHeaders(eq(302), anyLong());
    }

    @Test
    void testHandleSync_Success() throws IOException, SQLException {
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/products/sync?id=1"));

        controller.handle(mockExchange);

        verify(mockService).syncProduct(1);
        verify(mockExchange).sendResponseHeaders(eq(302), anyLong());
    }

    @Test
    void testHandleListGroups_ReturnsHtml() throws IOException, SQLException {
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/groups"));

        ProductGroup g = new ProductGroup();
        g.setId(1);
        g.setName("Group 1");
        g.setBasePrice(10.0);
        g.setBaseUnit("pcs");
        when(mockRepo.findAllGroups()).thenReturn(Collections.singletonList(g));

        controller.handle(mockExchange);

        verify(mockExchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("Product Groups"));
        assertTrue(response.contains("Group 1"));
    }

    @Test
    void testHandleShowCreateGroupForm() throws IOException {
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/groups/create"));

        controller.handle(mockExchange);

        verify(mockExchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("Create Product Group"));
    }

    @Test
    void testHandleCreateGroup_Success() throws IOException, SQLException {
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/groups/create"));
        String formData = "name=New+Group&description=Desc&basePrice=10.0&baseUnit=pcs";
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(formData.getBytes(StandardCharsets.UTF_8)));

        controller.handle(mockExchange);

        ArgumentCaptor<ProductGroup> captor = ArgumentCaptor.forClass(ProductGroup.class);
        verify(mockService).createProductGroup(captor.capture());
        assertEquals("New Group", captor.getValue().getName());
        verify(mockExchange).sendResponseHeaders(eq(302), anyLong());
    }

    @Test
    void testHandleShowGenerateVariantsForm() throws IOException {
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/groups/generate?id=1"));

        controller.handle(mockExchange);

        verify(mockExchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("Generate Variants"));
    }

    @Test
    void testHandleGenerateVariants_Success() throws IOException, SQLException {
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/groups/generate"));
        String formData = "id=1&attributes=Color:+Red,+Blue%0D%0ASize:+S,+M";
        when(mockExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(formData.getBytes(StandardCharsets.UTF_8)));

        controller.handle(mockExchange);

        verify(mockService).generateVariants(eq(1), any(Map.class));
        verify(mockExchange).sendResponseHeaders(eq(302), anyLong());
    }

    @Test
    void testHandleSyncGroup_Success() throws IOException, SQLException {
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/groups/sync?id=1"));

        controller.handle(mockExchange);

        verify(mockService).syncGroup(1);
        verify(mockExchange).sendResponseHeaders(eq(302), anyLong());
    }

    @Test
    void testHandle_NotFound() throws IOException {
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRequestURI()).thenReturn(URI.create("/unknown"));

        controller.handle(mockExchange);

        verify(mockExchange).sendResponseHeaders(eq(404), anyLong());
    }
}
