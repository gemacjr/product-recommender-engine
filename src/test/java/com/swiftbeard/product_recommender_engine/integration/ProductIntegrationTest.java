package com.swiftbeard.product_recommender_engine.integration;

import com.swiftbeard.product_recommender_engine.TestDataFactory;
import com.swiftbeard.product_recommender_engine.model.Category;
import com.swiftbeard.product_recommender_engine.model.Product;
import com.swiftbeard.product_recommender_engine.repository.ProductRepository;
import com.swiftbeard.product_recommender_engine.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for Product functionality
 * Tests the full stack from service to repository
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @MockBean
    private com.swiftbeard.product_recommender_engine.service.VectorStoreService vectorStoreService;

    @MockBean
    private org.springframework.ai.chat.model.ChatModel chatModel;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    void createProduct_ShouldPersistProductToDatabase() {
        // Arrange
        Product newProduct = TestDataFactory.createProductWithoutId(
                "Integration Test Product",
                Category.ELECTRONICS,
                new BigDecimal("299.99")
        );

        // Act
        Product savedProduct = productService.createProduct(newProduct);

        // Assert
        assertThat(savedProduct).isNotNull();
        assertThat(savedProduct.getId()).isNotNull();
        assertThat(savedProduct.getName()).isEqualTo("Integration Test Product");
        assertThat(savedProduct.getActive()).isTrue();
        assertThat(savedProduct.getCreatedAt()).isNotNull();
        assertThat(savedProduct.getUpdatedAt()).isNotNull();

        // Verify it's actually in the database
        Product retrieved = productRepository.findById(savedProduct.getId()).orElse(null);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getName()).isEqualTo("Integration Test Product");
    }

    @Test
    void getAllProducts_ShouldReturnPaginatedResults() {
        // Arrange
        productRepository.save(TestDataFactory.createProductWithoutId("Product 1", Category.ELECTRONICS, new BigDecimal("99.99")));
        productRepository.save(TestDataFactory.createProductWithoutId("Product 2", Category.CLOTHING, new BigDecimal("49.99")));
        productRepository.save(TestDataFactory.createProductWithoutId("Product 3", Category.BOOKS, new BigDecimal("29.99")));

        // Act
        Page<Product> result = productService.getAllProducts(0, 10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    void getProductById_ShouldReturnProduct_WhenExists() {
        // Arrange
        Product savedProduct = productRepository.save(
                TestDataFactory.createProductWithoutId("Test Product", Category.ELECTRONICS, new BigDecimal("99.99"))
        );

        // Act
        Product retrieved = productService.getProductById(savedProduct.getId());

        // Assert
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo(savedProduct.getId());
        assertThat(retrieved.getName()).isEqualTo("Test Product");
    }

    @Test
    void getProductById_ShouldThrowException_WhenNotFound() {
        // Act & Assert
        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found with id: 999");
    }

    @Test
    void updateProduct_ShouldModifyExistingProduct() {
        // Arrange
        Product original = productRepository.save(
                TestDataFactory.createProductWithoutId("Original Product", Category.ELECTRONICS, new BigDecimal("99.99"))
        );

        Product updateData = TestDataFactory.createProductWithoutId("Updated Product", Category.ELECTRONICS, new BigDecimal("149.99"));

        // Act
        Product updated = productService.updateProduct(original.getId(), updateData);

        // Assert
        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo(original.getId());
        assertThat(updated.getName()).isEqualTo("Updated Product");
        assertThat(updated.getPrice()).isEqualTo(new BigDecimal("149.99"));

        // Verify in database
        Product retrieved = productRepository.findById(original.getId()).orElse(null);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getName()).isEqualTo("Updated Product");
    }

    @Test
    void deleteProduct_ShouldSoftDeleteProduct() {
        // Arrange
        Product product = productRepository.save(
                TestDataFactory.createProductWithoutId("Product to Delete", Category.ELECTRONICS, new BigDecimal("99.99"))
        );

        // Act
        productService.deleteProduct(product.getId());

        // Assert
        Product deleted = productRepository.findById(product.getId()).orElse(null);
        assertThat(deleted).isNotNull();
        assertThat(deleted.getActive()).isFalse();
    }

    @Test
    void searchProducts_ShouldFindProductsByKeyword() {
        // Arrange
        productRepository.save(TestDataFactory.createProductWithoutId("Wireless Headphones", Category.ELECTRONICS, new BigDecimal("99.99")));
        productRepository.save(TestDataFactory.createProductWithoutId("Wired Headphones", Category.ELECTRONICS, new BigDecimal("49.99")));
        productRepository.save(TestDataFactory.createProductWithoutId("Gaming Mouse", Category.ELECTRONICS, new BigDecimal("79.99")));

        // Act
        Page<Product> results = productService.searchProducts("headphones", 0, 10);

        // Assert
        assertThat(results).isNotNull();
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent())
                .allMatch(p -> p.getName().toLowerCase().contains("headphones"));
    }

    @Test
    void getProductsByCategory_ShouldFilterByCategory() {
        // Arrange
        productRepository.save(TestDataFactory.createProductWithoutId("Product 1", Category.ELECTRONICS, new BigDecimal("99.99")));
        productRepository.save(TestDataFactory.createProductWithoutId("Product 2", Category.ELECTRONICS, new BigDecimal("149.99")));
        productRepository.save(TestDataFactory.createProductWithoutId("Product 3", Category.CLOTHING, new BigDecimal("49.99")));

        // Act
        Page<Product> results = productService.getProductsByCategory(Category.ELECTRONICS, 0, 10);

        // Assert
        assertThat(results).isNotNull();
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent()).allMatch(p -> p.getCategory() == Category.ELECTRONICS);
    }

    @Test
    void getProductsByPriceRange_ShouldFilterByPrice() {
        // Arrange
        productRepository.save(TestDataFactory.createProductWithoutId("Cheap Product", Category.ELECTRONICS, new BigDecimal("29.99")));
        productRepository.save(TestDataFactory.createProductWithoutId("Mid Product", Category.ELECTRONICS, new BigDecimal("99.99")));
        productRepository.save(TestDataFactory.createProductWithoutId("Expensive Product", Category.ELECTRONICS, new BigDecimal("299.99")));

        // Act
        Page<Product> results = productService.getProductsByPriceRange(
                new BigDecimal("50.00"),
                new BigDecimal("150.00"),
                0, 10
        );

        // Assert
        assertThat(results).isNotNull();
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).isEqualTo("Mid Product");
    }

    @Test
    void countByCategory_ShouldReturnCorrectCount() {
        // Arrange
        productRepository.save(TestDataFactory.createProductWithoutId("Product 1", Category.ELECTRONICS, new BigDecimal("99.99")));
        productRepository.save(TestDataFactory.createProductWithoutId("Product 2", Category.ELECTRONICS, new BigDecimal("149.99")));
        productRepository.save(TestDataFactory.createProductWithoutId("Product 3", Category.CLOTHING, new BigDecimal("49.99")));

        // Act
        long count = productService.countByCategory(Category.ELECTRONICS);

        // Assert
        assertThat(count).isEqualTo(2);
    }
}
