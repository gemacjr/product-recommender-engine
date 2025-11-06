package com.swiftbeard.product_recommender_engine.controller;

import com.swiftbeard.product_recommender_engine.dto.ProductRequest;
import com.swiftbeard.product_recommender_engine.dto.ProductResponse;
import com.swiftbeard.product_recommender_engine.model.Category;
import com.swiftbeard.product_recommender_engine.model.Product;
import com.swiftbeard.product_recommender_engine.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for Product operations
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    /**
     * Get all products
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /api/products - page: {}, size: {}", page, size);

        Page<Product> productsPage = productService.getAllProducts(page, size);

        Map<String, Object> response = createPageResponse(productsPage);

        return ResponseEntity.ok(response);
    }

    /**
     * Get product by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        log.info("GET /api/products/{}", id);

        Product product = productService.getProductById(id);
        return ResponseEntity.ok(ProductResponse.fromEntity(product));
    }

    /**
     * Search products by keyword
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /api/products/search - keyword: {}", keyword);

        Page<Product> productsPage = productService.searchProducts(keyword, page, size);

        Map<String, Object> response = createPageResponse(productsPage);

        return ResponseEntity.ok(response);
    }

    /**
     * Get products by category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> getProductsByCategory(
            @PathVariable Category category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /api/products/category/{}", category);

        Page<Product> productsPage = productService.getProductsByCategory(category, page, size);

        Map<String, Object> response = createPageResponse(productsPage);

        return ResponseEntity.ok(response);
    }

    /**
     * Get products by price range
     */
    @GetMapping("/price-range")
    public ResponseEntity<Map<String, Object>> getProductsByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /api/products/price-range - min: {}, max: {}", minPrice, maxPrice);

        Page<Product> productsPage = productService.getProductsByPriceRange(minPrice, maxPrice, page, size);

        Map<String, Object> response = createPageResponse(productsPage);

        return ResponseEntity.ok(response);
    }

    /**
     * Get top-rated products
     */
    @GetMapping("/top-rated")
    public ResponseEntity<Map<String, Object>> getTopRatedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /api/products/top-rated");

        Page<Product> productsPage = productService.getTopRatedProducts(page, size);

        Map<String, Object> response = createPageResponse(productsPage);

        return ResponseEntity.ok(response);
    }

    /**
     * Create a new product
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        log.info("POST /api/products - Creating product: {}", request.getName());

        Product product = mapToEntity(request);
        Product createdProduct = productService.createProduct(product);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProductResponse.fromEntity(createdProduct));
    }

    /**
     * Update a product
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        log.info("PUT /api/products/{} - Updating product", id);

        Product product = mapToEntity(request);
        Product updatedProduct = productService.updateProduct(id, product);

        return ResponseEntity.ok(ProductResponse.fromEntity(updatedProduct));
    }

    /**
     * Delete a product
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Long id) {
        log.info("DELETE /api/products/{}", id);

        productService.deleteProduct(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Product deleted successfully");
        response.put("id", id.toString());

        return ResponseEntity.ok(response);
    }

    /**
     * Generate personalized description for a product
     */
    @PostMapping("/{id}/personalized-description")
    public ResponseEntity<Map<String, String>> getPersonalizedDescription(
            @PathVariable Long id,
            @RequestParam(required = false) String userPreferences) {

        log.info("POST /api/products/{}/personalized-description", id);

        String description = productService.generatePersonalizedDescription(id, userPreferences);

        Map<String, String> response = new HashMap<>();
        response.put("productId", id.toString());
        response.put("personalizedDescription", description);

        return ResponseEntity.ok(response);
    }

    /**
     * Ingest all products to vector store
     */
    @PostMapping("/ingest")
    public ResponseEntity<Map<String, String>> ingestProducts() {
        log.info("POST /api/products/ingest");

        productService.ingestProductsToVectorStore();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Products ingested to vector store successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Get all brands
     */
    @GetMapping("/brands")
    public ResponseEntity<List<String>> getAllBrands() {
        log.info("GET /api/products/brands");
        return ResponseEntity.ok(productService.getAllBrands());
    }

    /**
     * Get all tags
     */
    @GetMapping("/tags")
    public ResponseEntity<List<String>> getAllTags() {
        log.info("GET /api/products/tags");
        return ResponseEntity.ok(productService.getAllTags());
    }

    /**
     * Helper method to create paginated response
     */
    private Map<String, Object> createPageResponse(Page<Product> page) {
        Map<String, Object> response = new HashMap<>();

        List<ProductResponse> products = page.getContent().stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());

        response.put("products", products);
        response.put("currentPage", page.getNumber());
        response.put("totalItems", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());

        return response;
    }

    /**
     * Helper method to map DTO to entity
     */
    private Product mapToEntity(ProductRequest request) {
        return Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .price(request.getPrice())
                .brand(request.getBrand())
                .sku(request.getSku())
                .tags(request.getTags())
                .features(request.getFeatures())
                .stockQuantity(request.getStockQuantity())
                .rating(request.getRating())
                .reviewCount(request.getReviewCount())
                .imageUrl(request.getImageUrl())
                .build();
    }
}
