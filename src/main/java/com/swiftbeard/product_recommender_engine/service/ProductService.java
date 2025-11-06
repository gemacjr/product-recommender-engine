package com.swiftbeard.product_recommender_engine.service;

import com.swiftbeard.product_recommender_engine.config.ApplicationProperties;
import com.swiftbeard.product_recommender_engine.model.Category;
import com.swiftbeard.product_recommender_engine.model.Product;
import com.swiftbeard.product_recommender_engine.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Service for managing product catalog
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final VectorStoreService vectorStoreService;
    private final ChatModel chatModel;
    private final ApplicationProperties applicationProperties;

    /**
     * Get all products with pagination
     */
    @Transactional(readOnly = true)
    public Page<Product> getAllProducts(int page, int size) {
        log.debug("Getting all products: page={}, size={}", page, size);

        size = Math.min(size, applicationProperties.getProduct().getMaxLimit());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return productRepository.findByActiveTrue(pageable);
    }

    /**
     * Get product by ID
     */
    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        log.debug("Getting product by ID: {}", id);

        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    /**
     * Get products by category
     */
    @Transactional(readOnly = true)
    public Page<Product> getProductsByCategory(Category category, int page, int size) {
        log.debug("Getting products by category: {}", category);

        size = Math.min(size, applicationProperties.getProduct().getMaxLimit());
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());

        return productRepository.findByCategoryAndActiveTrue(category, pageable);
    }

    /**
     * Search products by keyword
     */
    @Transactional(readOnly = true)
    public Page<Product> searchProducts(String keyword, int page, int size) {
        log.debug("Searching products with keyword: {}", keyword);

        size = Math.min(size, applicationProperties.getProduct().getMaxLimit());
        Pageable pageable = PageRequest.of(page, size);

        return productRepository.searchByKeyword(keyword, pageable);
    }

    /**
     * Get products by price range
     */
    @Transactional(readOnly = true)
    public Page<Product> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        log.debug("Getting products by price range: {} - {}", minPrice, maxPrice);

        size = Math.min(size, applicationProperties.getProduct().getMaxLimit());
        Pageable pageable = PageRequest.of(page, size, Sort.by("price").ascending());

        return productRepository.findByPriceRange(minPrice, maxPrice, pageable);
    }

    /**
     * Get top-rated products
     */
    @Transactional(readOnly = true)
    public Page<Product> getTopRatedProducts(int page, int size) {
        log.debug("Getting top-rated products");

        size = Math.min(size, applicationProperties.getProduct().getMaxLimit());
        Pageable pageable = PageRequest.of(page, size);

        return productRepository.findTopRated(pageable);
    }

    /**
     * Create a new product
     */
    @Transactional
    public Product createProduct(Product product) {
        log.debug("Creating new product: {}", product.getName());

        Product savedProduct = productRepository.save(product);

        // Add to vector store for semantic search
        try {
            vectorStoreService.addProduct(savedProduct);
        } catch (Exception e) {
            log.error("Failed to add product to vector store", e);
            // Don't fail the entire operation if vector store fails
        }

        log.info("Product created with ID: {}", savedProduct.getId());
        return savedProduct;
    }

    /**
     * Update an existing product
     */
    @Transactional
    public Product updateProduct(Long id, Product productDetails) {
        log.debug("Updating product: {}", id);

        Product product = getProductById(id);

        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setCategory(productDetails.getCategory());
        product.setPrice(productDetails.getPrice());
        product.setBrand(productDetails.getBrand());
        product.setSku(productDetails.getSku());
        product.setTags(productDetails.getTags());
        product.setFeatures(productDetails.getFeatures());
        product.setStockQuantity(productDetails.getStockQuantity());
        product.setRating(productDetails.getRating());
        product.setReviewCount(productDetails.getReviewCount());
        product.setImageUrl(productDetails.getImageUrl());

        Product updatedProduct = productRepository.save(product);

        // Update in vector store
        try {
            vectorStoreService.updateProduct(updatedProduct);
        } catch (Exception e) {
            log.error("Failed to update product in vector store", e);
        }

        log.info("Product updated: {}", updatedProduct.getId());
        return updatedProduct;
    }

    /**
     * Delete a product (soft delete)
     */
    @Transactional
    public void deleteProduct(Long id) {
        log.debug("Deleting product: {}", id);

        Product product = getProductById(id);
        product.setActive(false);
        productRepository.save(product);

        // Remove from vector store
        try {
            vectorStoreService.deleteProduct(id);
        } catch (Exception e) {
            log.error("Failed to delete product from vector store", e);
        }

        log.info("Product deleted: {}", id);
    }

    /**
     * Generate personalized product description using AI
     */
    public String generatePersonalizedDescription(Long productId, String userPreferences) {
        log.debug("Generating personalized description for product: {}", productId);

        Product product = getProductById(productId);

        String promptTemplate = """
                You are a helpful product marketing assistant. Generate a personalized product description
                based on the following product information and user preferences.

                Product Information:
                Name: {name}
                Category: {category}
                Original Description: {description}
                Features: {features}
                Price: ${price}
                Rating: {rating} stars

                User Preferences: {userPreferences}

                Create a compelling, personalized product description that highlights aspects most relevant
                to the user's preferences. Keep it concise (2-3 sentences) and engaging.
                """;

        PromptTemplate prompt = new PromptTemplate(promptTemplate);
        Map<String, Object> params = Map.of(
                "name", product.getName(),
                "category", product.getCategory().getDisplayName(),
                "description", product.getDescription(),
                "features", String.join(", ", product.getFeatures()),
                "price", product.getPrice(),
                "rating", product.getRating(),
                "userPreferences", userPreferences != null ? userPreferences : "general quality and value"
        );

        Prompt generatedPrompt = prompt.create(params);
        String personalizedDescription = chatModel.call(generatedPrompt).getResult().getOutput().getContent();

        log.info("Generated personalized description for product: {}", productId);
        return personalizedDescription;
    }

    /**
     * Bulk ingest products to vector store
     */
    @Transactional
    public void ingestProductsToVectorStore() {
        log.info("Ingesting all products to vector store");

        List<Product> allProducts = productRepository.findAll();
        vectorStoreService.addProducts(allProducts);

        log.info("Ingested {} products to vector store", allProducts.size());
    }

    /**
     * Get all distinct brands
     */
    @Transactional(readOnly = true)
    public List<String> getAllBrands() {
        return productRepository.findDistinctBrands();
    }

    /**
     * Get all distinct tags
     */
    @Transactional(readOnly = true)
    public List<String> getAllTags() {
        return productRepository.findDistinctTags();
    }

    /**
     * Count products by category
     */
    @Transactional(readOnly = true)
    public long countByCategory(Category category) {
        return productRepository.countByCategoryAndActiveTrue(category);
    }
}
