package com.swiftbeard.product_recommender_engine.service;

import com.swiftbeard.product_recommender_engine.config.ApplicationProperties;
import com.swiftbeard.product_recommender_engine.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating product recommendations
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationService {

    private final VectorStoreService vectorStoreService;
    private final ProductService productService;
    private final ApplicationProperties applicationProperties;

    /**
     * Get similar products based on a specific product
     */
    public List<Product> getSimilarProducts(Long productId, int limit) {
        log.debug("Getting similar products for product ID: {}", productId);

        Product product = productService.getProductById(productId);

        int maxResults = Math.min(limit, applicationProperties.getRecommendation().getMaxResults());

        List<Product> similarProducts = vectorStoreService.findSimilarProducts(product, maxResults);

        log.info("Found {} similar products for product {}", similarProducts.size(), productId);
        return similarProducts;
    }

    /**
     * Get product recommendations based on semantic search
     */
    public List<Product> getRecommendationsByQuery(String query, int limit) {
        log.debug("Getting recommendations for query: {}", query);

        int maxResults = Math.min(limit, applicationProperties.getRecommendation().getMaxResults());

        List<Product> recommendations = vectorStoreService.semanticSearch(query, maxResults);

        log.info("Found {} recommendations for query: {}", recommendations.size(), query);
        return recommendations;
    }

    /**
     * Get personalized recommendations based on user preferences
     */
    public List<Product> getPersonalizedRecommendations(String userPreferences, int limit) {
        log.debug("Getting personalized recommendations");

        // Create a rich query from user preferences
        String enrichedQuery = enrichUserPreferences(userPreferences);

        return getRecommendationsByQuery(enrichedQuery, limit);
    }

    /**
     * Get recommendations based on browsing history
     */
    public List<Product> getRecommendationsFromHistory(List<Long> viewedProductIds, int limit) {
        if (viewedProductIds == null || viewedProductIds.isEmpty()) {
            log.warn("No browsing history provided");
            return Collections.emptyList();
        }

        log.debug("Getting recommendations from browsing history of {} products", viewedProductIds.size());

        // Get the most recently viewed products
        List<Long> recentProducts = viewedProductIds.stream()
                .limit(5)
                .collect(Collectors.toList());

        // Collect similar products for each viewed product
        Set<Product> recommendedProducts = new HashSet<>();

        for (Long productId : recentProducts) {
            try {
                List<Product> similar = getSimilarProducts(productId, 5);
                recommendedProducts.addAll(similar);
            } catch (Exception e) {
                log.warn("Failed to get recommendations for product {}", productId, e);
            }

            if (recommendedProducts.size() >= limit) {
                break;
            }
        }

        // Remove products that were already viewed
        List<Product> filtered = recommendedProducts.stream()
                .filter(p -> !viewedProductIds.contains(p.getId()))
                .limit(limit)
                .collect(Collectors.toList());

        log.info("Generated {} recommendations from browsing history", filtered.size());
        return filtered;
    }

    /**
     * Get complementary products (frequently bought together)
     */
    public List<Product> getComplementaryProducts(Long productId, int limit) {
        log.debug("Getting complementary products for product ID: {}", productId);

        Product product = productService.getProductById(productId);

        // Create a query focusing on complementary aspects
        String complementaryQuery = String.format(
                "Products that complement %s in category %s, accessories and related items",
                product.getName(),
                product.getCategory().getDisplayName()
        );

        List<Product> complementary = vectorStoreService.semanticSearch(complementaryQuery, limit + 1);

        // Filter out the original product
        List<Product> filtered = complementary.stream()
                .filter(p -> !p.getId().equals(productId))
                .limit(limit)
                .collect(Collectors.toList());

        log.info("Found {} complementary products", filtered.size());
        return filtered;
    }

    /**
     * Get trending products (placeholder - in production, this would use analytics data)
     */
    public List<Product> getTrendingProducts(int limit) {
        log.debug("Getting trending products");

        // For now, return top-rated products as a proxy for trending
        // In production, this would use actual analytics and purchase data
        return productService.getTopRatedProducts(0, limit).getContent();
    }

    /**
     * Get recommendations for a specific category with user context
     */
    public List<Product> getCategoryRecommendations(String category, String userContext, int limit) {
        log.debug("Getting {} recommendations for category: {}", limit, category);

        String query = String.format("%s products in %s category", userContext, category);

        return getRecommendationsByQuery(query, limit);
    }

    /**
     * Get diverse recommendations across multiple categories
     */
    public List<Product> getDiverseRecommendations(String userInterests, int limit) {
        log.debug("Getting diverse recommendations");

        // Increase search results to ensure diversity
        List<Product> candidates = vectorStoreService.semanticSearch(
                userInterests,
                limit * 3
        );

        // Group by category
        Map<String, List<Product>> byCategory = candidates.stream()
                .collect(Collectors.groupingBy(p -> p.getCategory().name()));

        // Select products from different categories
        List<Product> diverse = new ArrayList<>();
        Iterator<Map.Entry<String, List<Product>>> iterator = byCategory.entrySet().iterator();

        while (diverse.size() < limit && iterator.hasNext()) {
            Map.Entry<String, List<Product>> entry = iterator.next();
            List<Product> categoryProducts = entry.getValue();

            if (!categoryProducts.isEmpty()) {
                diverse.add(categoryProducts.get(0));
                categoryProducts.remove(0);
            }

            if (!iterator.hasNext() && diverse.size() < limit) {
                iterator = byCategory.entrySet().iterator();
            }
        }

        log.info("Generated {} diverse recommendations across {} categories",
                diverse.size(), byCategory.size());

        return diverse;
    }

    /**
     * Get budget-friendly alternatives
     */
    public List<Product> getBudgetAlternatives(Long productId, int limit) {
        log.debug("Getting budget alternatives for product ID: {}", productId);

        Product originalProduct = productService.getProductById(productId);

        // Find similar products
        List<Product> similar = getSimilarProducts(productId, limit * 2);

        // Filter for products cheaper than the original
        List<Product> budgetAlternatives = similar.stream()
                .filter(p -> p.getPrice().compareTo(originalProduct.getPrice()) < 0)
                .sorted(Comparator.comparing(Product::getPrice).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        log.info("Found {} budget alternatives", budgetAlternatives.size());
        return budgetAlternatives;
    }

    /**
     * Get premium alternatives
     */
    public List<Product> getPremiumAlternatives(Long productId, int limit) {
        log.debug("Getting premium alternatives for product ID: {}", productId);

        Product originalProduct = productService.getProductById(productId);

        // Find similar products
        List<Product> similar = getSimilarProducts(productId, limit * 2);

        // Filter for products more expensive than the original
        List<Product> premiumAlternatives = similar.stream()
                .filter(p -> p.getPrice().compareTo(originalProduct.getPrice()) > 0)
                .sorted(Comparator.comparing(Product::getPrice))
                .limit(limit)
                .collect(Collectors.toList());

        log.info("Found {} premium alternatives", premiumAlternatives.size());
        return premiumAlternatives;
    }

    /**
     * Enrich user preferences with context
     */
    private String enrichUserPreferences(String userPreferences) {
        if (userPreferences == null || userPreferences.trim().isEmpty()) {
            return "popular high-quality products";
        }

        return String.format(
                "Products matching user preferences: %s. Looking for quality, value, and relevance.",
                userPreferences
        );
    }
}
