package com.swiftbeard.product_recommender_engine.repository;

import com.swiftbeard.product_recommender_engine.TestDataFactory;
import com.swiftbeard.product_recommender_engine.model.Category;
import com.swiftbeard.product_recommender_engine.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    private Product product1;
    private Product product2;
    private Product product3;
    private Product inactiveProduct;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        product1 = TestDataFactory.createProduct(null, "Wireless Headphones", Category.ELECTRONICS, new BigDecimal("99.99"));
        product1.setRating(new BigDecimal("4.5"));
        product1.setReviewCount(100);
        product1.setBrand("AudioTech");
        product1.setSku("AUD-WH-001");
        product1.setTags(Arrays.asList("wireless", "audio", "premium"));

        product2 = TestDataFactory.createProduct(null, "Gaming Mouse", Category.ELECTRONICS, new BigDecimal("79.99"));
        product2.setRating(new BigDecimal("4.7"));
        product2.setReviewCount(150);
        product2.setBrand("GamerPro");
        product2.setSku("GAM-MOU-002");
        product2.setTags(Arrays.asList("gaming", "rgb", "precision"));

        product3 = TestDataFactory.createProduct(null, "Cotton T-Shirt", Category.CLOTHING, new BigDecimal("29.99"));
        product3.setRating(new BigDecimal("4.2"));
        product3.setReviewCount(50);
        product3.setBrand("FashionCo");
        product3.setSku("FASH-TS-003");
        product3.setTags(Arrays.asList("cotton", "casual", "comfortable"));

        inactiveProduct = TestDataFactory.createProduct(null, "Discontinued Item", Category.BOOKS, new BigDecimal("19.99"));
        inactiveProduct.setActive(false);

        product1 = productRepository.save(product1);
        product2 = productRepository.save(product2);
        product3 = productRepository.save(product3);
        inactiveProduct = productRepository.save(inactiveProduct);
    }

    @Test
    void findByActiveTrue_ShouldReturnOnlyActiveProducts() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Product> result = productRepository.findByActiveTrue(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent()).allMatch(Product::getActive);
        assertThat(result.getContent()).doesNotContain(inactiveProduct);
    }

    @Test
    void findByCategoryAndActiveTrue_ShouldReturnProductsInCategory() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Product> result = productRepository.findByCategoryAndActiveTrue(Category.ELECTRONICS, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(p -> p.getCategory() == Category.ELECTRONICS);
        assertThat(result.getContent()).extracting(Product::getName)
                .containsExactlyInAnyOrder("Wireless Headphones", "Gaming Mouse");
    }

    @Test
    void findByNameContainingIgnoreCaseAndActiveTrue_ShouldReturnMatchingProducts() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Product> result = productRepository.findByNameContainingIgnoreCaseAndActiveTrue("headphones", pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Wireless Headphones");
    }

    @Test
    void findByBrandIgnoreCaseAndActiveTrue_ShouldReturnProductsByBrand() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Product> result = productRepository.findByBrandIgnoreCaseAndActiveTrue("audiotech", pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getBrand()).isEqualToIgnoringCase("AudioTech");
    }

    @Test
    void findByPriceRange_ShouldReturnProductsInRange() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        BigDecimal minPrice = new BigDecimal("50.00");
        BigDecimal maxPrice = new BigDecimal("100.00");

        // Act
        Page<Product> result = productRepository.findByPriceRange(minPrice, maxPrice, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(p ->
                p.getPrice().compareTo(minPrice) >= 0 && p.getPrice().compareTo(maxPrice) <= 0
        );
    }

    @Test
    void findByMinimumRating_ShouldReturnHighlyRatedProducts() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        BigDecimal minRating = new BigDecimal("4.5");

        // Act
        Page<Product> result = productRepository.findByMinimumRating(minRating, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(result.getContent()).allMatch(p -> p.getRating().compareTo(minRating) >= 0);
        // Verify ordering by rating descending
        for (int i = 0; i < result.getContent().size() - 1; i++) {
            assertThat(result.getContent().get(i).getRating())
                    .isGreaterThanOrEqualTo(result.getContent().get(i + 1).getRating());
        }
    }

    @Test
    void findBySkuAndActiveTrue_ShouldReturnProduct_WhenExists() {
        // Act
        Optional<Product> result = productRepository.findBySkuAndActiveTrue("AUD-WH-001");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Wireless Headphones");
    }

    @Test
    void findBySkuAndActiveTrue_ShouldReturnEmpty_WhenNotFound() {
        // Act
        Optional<Product> result = productRepository.findBySkuAndActiveTrue("NONEXISTENT");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findByTag_ShouldReturnProductsWithTag() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Product> result = productRepository.findByTag("wireless", pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTags()).contains("wireless");
    }

    @Test
    void searchByKeyword_ShouldFindInName() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Product> result = productRepository.searchByKeyword("gaming", pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(result.getContent()).anyMatch(p -> p.getName().toLowerCase().contains("gaming"));
    }

    @Test
    void searchByKeyword_ShouldFindInDescription() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        product1.setDescription("This product has excellent noise cancellation");
        productRepository.save(product1);

        // Act
        Page<Product> result = productRepository.searchByKeyword("noise", pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(result.getContent()).anyMatch(p -> p.getDescription().toLowerCase().contains("noise"));
    }

    @Test
    void searchByKeyword_ShouldFindInFeatures() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        product1.setFeatures(Arrays.asList("Bluetooth 5.0", "Active Noise Cancellation"));
        productRepository.save(product1);

        // Act
        Page<Product> result = productRepository.searchByKeyword("bluetooth", pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void findTopRated_ShouldReturnProductsOrderedByRating() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Product> result = productRepository.findTopRated(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSizeGreaterThan(0);
        // Verify ordering: first should have highest rating
        assertThat(result.getContent().get(0).getRating()).isEqualTo(new BigDecimal("4.7"));
        assertThat(result.getContent().get(0).getName()).isEqualTo("Gaming Mouse");
    }

    @Test
    void findByIdInAndActiveTrue_ShouldReturnSpecifiedProducts() {
        // Arrange
        List<Long> ids = Arrays.asList(product1.getId(), product3.getId());

        // Act
        List<Product> result = productRepository.findByIdInAndActiveTrue(ids);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Product::getId).containsExactlyInAnyOrder(product1.getId(), product3.getId());
    }

    @Test
    void findByIdInAndActiveTrue_ShouldExcludeInactiveProducts() {
        // Arrange
        List<Long> ids = Arrays.asList(product1.getId(), inactiveProduct.getId());

        // Act
        List<Product> result = productRepository.findByIdInAndActiveTrue(ids);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(product1.getId());
    }

    @Test
    void countByCategoryAndActiveTrue_ShouldReturnCorrectCount() {
        // Act
        long count = productRepository.countByCategoryAndActiveTrue(Category.ELECTRONICS);

        // Assert
        assertThat(count).isEqualTo(2);
    }

    @Test
    void countByCategoryAndActiveTrue_ShouldReturnZero_WhenNoProducts() {
        // Act
        long count = productRepository.countByCategoryAndActiveTrue(Category.TOYS_GAMES);

        // Assert
        assertThat(count).isZero();
    }

    @Test
    void findDistinctBrands_ShouldReturnUniqueBrands() {
        // Act
        List<String> brands = productRepository.findDistinctBrands();

        // Assert
        assertThat(brands).isNotNull();
        assertThat(brands).hasSize(3);
        assertThat(brands).containsExactlyInAnyOrder("AudioTech", "FashionCo", "GamerPro");
    }

    @Test
    void findDistinctTags_ShouldReturnUniqueTags() {
        // Act
        List<String> tags = productRepository.findDistinctTags();

        // Assert
        assertThat(tags).isNotNull();
        assertThat(tags).containsExactlyInAnyOrder(
                "wireless", "audio", "premium",
                "gaming", "rgb", "precision",
                "cotton", "casual", "comfortable"
        );
    }

    @Test
    void findAll_ShouldIncludeInactiveProducts() {
        // Act
        List<Product> all = productRepository.findAll();

        // Assert
        assertThat(all).hasSize(4);
        assertThat(all).contains(inactiveProduct);
    }

    @Test
    void save_ShouldSetTimestamps() {
        // Arrange
        Product newProduct = TestDataFactory.createProductWithoutId("New Product", Category.BOOKS, new BigDecimal("19.99"));

        // Act
        Product saved = productRepository.save(newProduct);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void update_ShouldUpdateTimestamp() throws InterruptedException {
        // Arrange
        Product product = productRepository.findById(product1.getId()).orElseThrow();
        var originalUpdatedAt = product.getUpdatedAt();

        // Wait a bit to ensure timestamp difference
        Thread.sleep(10);

        // Act
        product.setName("Updated Name");
        Product updated = productRepository.save(product);

        // Assert
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(updated.getCreatedAt()).isEqualTo(product1.getCreatedAt());
    }
}
