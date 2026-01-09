package com.example.productmanagement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductRepositoryTest {

    private Connection connection;
    private ProductRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        repository = new ProductRepository(connection);
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS product_groups (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(255), " +
                    "description VARCHAR(255), " +
                    "base_price DOUBLE, " +
                    "base_unit VARCHAR(50))");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "group_id INT, " +
                    "type VARCHAR(50), " +
                    "name VARCHAR(255), " +
                    "description VARCHAR(255), " +
                    "price DOUBLE, " +
                    "unit VARCHAR(50), " +
                    "attributes VARCHAR(1000), " +
                    "FOREIGN KEY (group_id) REFERENCES product_groups(id))");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS products");
            stmt.execute("DROP TABLE IF EXISTS product_groups");
        }
        connection.close();
    }

    @Test
    void testCreateAndFindGroup() throws SQLException {
        ProductGroup group = new ProductGroup();
        group.setName("T-Shirts");
        group.setDescription("Cotton T-Shirts");
        group.setBasePrice(15.0);
        group.setBaseUnit("pcs");

        int id = repository.createGroup(group);
        assertTrue(id > 0);

        ProductGroup found = repository.findGroupById(id);
        assertNotNull(found);
        assertEquals("T-Shirts", found.getName());
        assertEquals(15.0, found.getBasePrice());
    }

    @Test
    void testFindAllGroups() throws SQLException {
        ProductGroup g1 = new ProductGroup();
        g1.setName("G1");
        g1.setBasePrice(10.0);
        repository.createGroup(g1);

        ProductGroup g2 = new ProductGroup();
        g2.setName("G2");
        g2.setBasePrice(20.0);
        repository.createGroup(g2);

        List<ProductGroup> groups = repository.findAllGroups();
        assertEquals(2, groups.size());
    }

    @Test
    void testCreateAndFindProduct() throws SQLException {
        Product p = new Product();
        p.setType("Book");
        p.setName("Test Book");
        p.setDescription("A test book");
        p.setPrice(29.99);
        p.setUnit("pcs");
        p.setAttributes("Pages: 100");

        int id = repository.create(p);
        assertTrue(id > 0);

        Product found = repository.findById(id);
        assertNotNull(found);
        assertEquals("Test Book", found.getName());
        assertEquals(29.99, found.getPrice());
        assertNull(found.getGroupId());
    }

    @Test
    void testCreateProductWithGroup() throws SQLException {
        ProductGroup g = new ProductGroup();
        g.setName("Group");
        g.setBasePrice(10.0);
        int groupId = repository.createGroup(g);

        Product p = new Product();
        p.setGroupId(groupId);
        p.setType("Item");
        p.setName("Item 1");
        p.setPrice(10.0);
        p.setUnit("pcs");

        int id = repository.create(p);
        Product found = repository.findById(id);
        assertEquals(groupId, found.getGroupId());
    }

    @Test
    void testUpdateProduct() throws SQLException {
        Product p = new Product();
        p.setName("Original");
        p.setPrice(10.0);
        int id = repository.create(p);

        p.setId(id);
        p.setName("Updated");
        p.setPrice(20.0);
        repository.update(p);

        Product found = repository.findById(id);
        assertEquals("Updated", found.getName());
        assertEquals(20.0, found.getPrice());
    }

    @Test
    void testDeleteProduct() throws SQLException {
        Product p = new Product();
        p.setName("To Delete");
        p.setPrice(10.0);
        int id = repository.create(p);

        repository.delete(id);

        Product found = repository.findById(id);
        assertNull(found);
    }

    @Test
    void testFindByGroupId() throws SQLException {
        ProductGroup g = new ProductGroup();
        g.setName("Group");
        g.setBasePrice(10.0);
        int groupId = repository.createGroup(g);

        Product p1 = new Product();
        p1.setGroupId(groupId);
        p1.setName("P1");
        p1.setPrice(10.0);
        repository.create(p1);

        Product p2 = new Product();
        p2.setGroupId(groupId);
        p2.setName("P2");
        p2.setPrice(10.0);
        repository.create(p2);

        Product p3 = new Product(); // No group
        p3.setName("P3");
        p3.setPrice(10.0);
        repository.create(p3);

        List<Product> groupProducts = repository.findByGroupId(groupId);
        assertEquals(2, groupProducts.size());
    }

    @Test
    void testFindAllProducts() throws SQLException {
        Product p1 = new Product();
        p1.setName("P1");
        p1.setPrice(10.0);
        repository.create(p1);

        Product p2 = new Product();
        p2.setName("P2");
        p2.setPrice(10.0);
        repository.create(p2);

        List<Product> products = repository.findAll();
        assertEquals(2, products.size());
    }
}
