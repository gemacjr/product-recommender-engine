package com.swiftbeard.product_recommender_engine.controller;

import com.swiftbeard.product_recommender_engine.dto.ProductResponse;
import com.swiftbeard.product_recommender_engine.dto.RecommendationRequest;
import com.swiftbeard.product_recommender_engine.model.Product;
import com.swiftbeard.product_recommender_engine.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Product Recommendations
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * Get similar products based on a product ID
     */
    @GetMapping("/similar/{productId}")
    public ResponseEntity<List<ProductResponse>> getSimilarProducts(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("GET /api/recommendations/similar/{} - limit: {}", productId, limit);

        List<Product> similar = recommendationService.getSimilarProducts(productId, limit);

        return ResponseEntity.ok(
                similar.stream()
                        .map(ProductResponse::fromEntity)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Get product recommendations based on a search query
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> getRecommendationsByQuery(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("GET /api/recommendations/search - query: {}, limit: {}", query, limit);

        List<Product> recommendations = recommendationService.getRecommendationsByQuery(query, limit);

        return ResponseEntity.ok(
                recommendations.stream()
                        .map(ProductResponse::fromEntity)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Get personalized recommendations
     */
    @PostMapping("/personalized")
    public ResponseEntity<List<ProductResponse>> getPersonalizedRecommendations(
            @Valid @RequestBody RecommendationRequest request) {

        log.info("POST /api/recommendations/personalized");

        List<Product> recommendations = recommendationService.getPersonalizedRecommendations(
                request.getUserPreferences(),
                request.getLimit()
        );

        return ResponseEntity.ok(
                recommendations.stream()
                        .map(ProductResponse::fromEntity)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Get recommendations based on browsing history
     */
    @PostMapping("/from-history")
    public ResponseEntity<List<ProductResponse>> getRecommendationsFromHistory(
            @Valid @RequestBody RecommendationRequest request) {

        log.info("POST /api/recommendations/from-history");

        List<Product> recommendations = recommendationService.getRecommendationsFromHistory(
                request.getViewedProductIds(),
                request.getLimit()
        );

        return ResponseEntity.ok(
                recommendations.stream()
                        .map(ProductResponse::fromEntity)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Get complementary products
     */
    @GetMapping("/complementary/{productId}")
    public ResponseEntity<List<ProductResponse>> getComplementaryProducts(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("GET /api/recommendations/complementary/{} - limit: {}", productId, limit);

        List<Product> complementary = recommendationService.getComplementaryProducts(productId, limit);

        return ResponseEntity.ok(
                complementary.stream()
                        .map(ProductResponse::fromEntity)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Get trending products
     */
    @GetMapping("/trending")
    public ResponseEntity<List<ProductResponse>> getTrendingProducts(
            @RequestParam(defaultValue = "10") int limit) {

        log.info("GET /api/recommendations/trending - limit: {}", limit);

        List<Product> trending = recommendationService.getTrendingProducts(limit);

        return ResponseEntity.ok(
                trending.stream()
                        .map(ProductResponse::fromEntity)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Get diverse recommendations across categories
     */
    @GetMapping("/diverse")
    public ResponseEntity<List<ProductResponse>> getDiverseRecommendations(
            @RequestParam(required = false) String userInterests,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("GET /api/recommendations/diverse - limit: {}", limit);

        String interests = userInterests != null ? userInterests : "general shopping";

        List<Product> diverse = recommendationService.getDiverseRecommendations(interests, limit);

        return ResponseEntity.ok(
                diverse.stream()
                        .map(ProductResponse::fromEntity)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Get budget-friendly alternatives
     */
    @GetMapping("/budget-alternatives/{productId}")
    public ResponseEntity<List<ProductResponse>> getBudgetAlternatives(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "5") int limit) {

        log.info("GET /api/recommendations/budget-alternatives/{}", productId);

        List<Product> alternatives = recommendationService.getBudgetAlternatives(productId, limit);

        return ResponseEntity.ok(
                alternatives.stream()
                        .map(ProductResponse::fromEntity)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Get premium alternatives
     */
    @GetMapping("/premium-alternatives/{productId}")
    public ResponseEntity<List<ProductResponse>> getPremiumAlternatives(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "5") int limit) {

        log.info("GET /api/recommendations/premium-alternatives/{}", productId);

        List<Product> alternatives = recommendationService.getPremiumAlternatives(productId, limit);

        return ResponseEntity.ok(
                alternatives.stream()
                        .map(ProductResponse::fromEntity)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Get category recommendations with user context
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductResponse>> getCategoryRecommendations(
            @PathVariable String category,
            @RequestParam(required = false) String userContext,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("GET /api/recommendations/category/{}", category);

        String context = userContext != null ? userContext : "quality products";

        List<Product> recommendations = recommendationService.getCategoryRecommendations(
                category,
                context,
                limit
        );

        return ResponseEntity.ok(
                recommendations.stream()
                        .map(ProductResponse::fromEntity)
                        .collect(Collectors.toList())
        );
    }
}
