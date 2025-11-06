package com.swiftbeard.product_recommender_engine;

import com.swiftbeard.product_recommender_engine.dto.ProductRequest;
import com.swiftbeard.product_recommender_engine.dto.QueryRequest;
import com.swiftbeard.product_recommender_engine.dto.RecommendationRequest;
import com.swiftbeard.product_recommender_engine.model.Category;
import com.swiftbeard.product_recommender_engine.model.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Factory class for creating test data objects
 */
public class TestDataFactory {

    public static Product createProduct(Long id, String name, Category category, BigDecimal price) {
        return Product.builder()
                .id(id)
                .name(name)
                .description("Test description for " + name)
                .category(category)
                .price(price)
                .brand("Test Brand")
                .sku("SKU-" + id)
                .tags(Arrays.asList("tag1", "tag2"))
                .features(Arrays.asList("feature1", "feature2"))
                .stockQuantity(100)
                .rating(new BigDecimal("4.5"))
                .reviewCount(50)
                .imageUrl("http://example.com/image.jpg")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static Product createElectronicsProduct() {
        return createProduct(1L, "Wireless Headphones", Category.ELECTRONICS, new BigDecimal("99.99"));
    }

    public static Product createClothingProduct() {
        return createProduct(2L, "Cotton T-Shirt", Category.CLOTHING, new BigDecimal("29.99"));
    }

    public static Product createBookProduct() {
        return createProduct(3L, "Java Programming Guide", Category.BOOKS, new BigDecimal("49.99"));
    }

    public static Product createProductWithoutId(String name, Category category, BigDecimal price) {
        Product product = createProduct(null, name, category, price);
        product.setId(null);
        return product;
    }

    public static List<Product> createProductList() {
        return Arrays.asList(
                createElectronicsProduct(),
                createClothingProduct(),
                createBookProduct(),
                createProduct(4L, "Gaming Mouse", Category.ELECTRONICS, new BigDecimal("79.99")),
                createProduct(5L, "Running Shoes", Category.SPORTS_OUTDOORS, new BigDecimal("119.99"))
        );
    }

    public static ProductRequest createProductRequest() {
        return ProductRequest.builder()
                .name("Test Product")
                .description("This is a test product description")
                .category(Category.ELECTRONICS)
                .price(new BigDecimal("99.99"))
                .brand("Test Brand")
                .sku("TEST-SKU-001")
                .tags(Arrays.asList("test", "electronics"))
                .features(Arrays.asList("Feature 1", "Feature 2"))
                .stockQuantity(50)
                .rating(new BigDecimal("4.0"))
                .reviewCount(10)
                .imageUrl("http://example.com/test.jpg")
                .build();
    }

    public static ProductRequest createInvalidProductRequest() {
        return ProductRequest.builder()
                .name("AB") // Too short
                .description("Short") // Too short
                .category(null) // Required
                .price(new BigDecimal("-10.00")) // Invalid
                .stockQuantity(-5) // Invalid
                .rating(new BigDecimal("6.0")) // Too high
                .reviewCount(-1) // Invalid
                .build();
    }

    public static QueryRequest createQueryRequest(String query) {
        return QueryRequest.builder()
                .query(query)
                .userPreferences("tech enthusiast")
                .context("product search")
                .build();
    }

    public static RecommendationRequest createRecommendationRequest() {
        return RecommendationRequest.builder()
                .userPreferences("electronics and gaming")
                .limit(10)
                .viewedProductIds(Arrays.asList(1L, 2L, 3L))
                .build();
    }

    public static Product createProductWithRating(Long id, BigDecimal rating, int reviewCount) {
        Product product = createElectronicsProduct();
        product.setId(id);
        product.setRating(rating);
        product.setReviewCount(reviewCount);
        return product;
    }

    public static Product createProductInPriceRange(Long id, BigDecimal price) {
        Product product = createElectronicsProduct();
        product.setId(id);
        product.setPrice(price);
        return product;
    }
}
