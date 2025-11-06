package com.swiftbeard.product_recommender_engine.controller;

import com.swiftbeard.product_recommender_engine.dto.QueryRequest;
import com.swiftbeard.product_recommender_engine.dto.QueryResponse;
import com.swiftbeard.product_recommender_engine.dto.UrlQueryRequest;
import com.swiftbeard.product_recommender_engine.dto.UrlQueryResponse;
import com.swiftbeard.product_recommender_engine.service.RagService;
import com.swiftbeard.product_recommender_engine.service.WebScrapingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for RAG-based Query Answering
 */
@RestController
@RequestMapping("/api/queries")
@RequiredArgsConstructor
@Slf4j
public class QueryController {

    private final RagService ragService;
    private final WebScrapingService webScrapingService;

    /**
     * Answer a customer query using RAG
     */
    @PostMapping("/ask")
    public ResponseEntity<QueryResponse> answerQuery(@Valid @RequestBody QueryRequest request) {
        log.info("POST /api/queries/ask - query: {}", request.getQuery());

        String answer = ragService.answerQuery(request.getQuery());

        QueryResponse response = QueryResponse.builder()
                .query(request.getQuery())
                .answer(answer)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get product recommendation with explanation
     */
    @PostMapping("/recommend-with-explanation")
    public ResponseEntity<Map<String, String>> getRecommendationWithExplanation(
            @Valid @RequestBody QueryRequest request) {

        log.info("POST /api/queries/recommend-with-explanation");

        String recommendation = ragService.getRecommendationWithExplanation(
                request.getQuery(),
                request.getUserPreferences()
        );

        Map<String, String> response = new HashMap<>();
        response.put("query", request.getQuery());
        response.put("recommendation", recommendation);

        return ResponseEntity.ok(response);
    }

    /**
     * Compare products
     */
    @PostMapping("/compare-products")
    public ResponseEntity<Map<String, String>> compareProducts(@RequestBody List<Long> productIds) {
        log.info("POST /api/queries/compare-products - {} products", productIds.size());

        String comparison = ragService.compareProducts(productIds);

        Map<String, String> response = new HashMap<>();
        response.put("comparison", comparison);

        return ResponseEntity.ok(response);
    }

    /**
     * Answer FAQ about a specific product
     */
    @PostMapping("/product-faq/{productId}")
    public ResponseEntity<Map<String, String>> answerProductFaq(
            @PathVariable Long productId,
            @RequestBody Map<String, String> requestBody) {

        String question = requestBody.get("question");
        log.info("POST /api/queries/product-faq/{} - question: {}", productId, question);

        String answer = ragService.answerProductFaq(productId, question);

        Map<String, String> response = new HashMap<>();
        response.put("productId", productId.toString());
        response.put("question", question);
        response.put("answer", answer);

        return ResponseEntity.ok(response);
    }

    /**
     * Get personalized shopping suggestions
     */
    @PostMapping("/personalized-suggestions")
    public ResponseEntity<Map<String, String>> getPersonalizedSuggestions(
            @RequestBody Map<String, String> requestBody) {

        String userProfile = requestBody.getOrDefault("userProfile", "general customer");
        String occasion = requestBody.getOrDefault("occasion", "general shopping");

        log.info("POST /api/queries/personalized-suggestions - occasion: {}", occasion);

        String suggestions = ragService.getPersonalizedSuggestions(userProfile, occasion);

        Map<String, String> response = new HashMap<>();
        response.put("userProfile", userProfile);
        response.put("occasion", occasion);
        response.put("suggestions", suggestions);

        return ResponseEntity.ok(response);
    }

    /**
     * Answer a query about a shopping website URL
     */
    @PostMapping("/ask-url")
    public ResponseEntity<?> answerUrlQuery(@Valid @RequestBody UrlQueryRequest request) {
        log.info("POST /api/queries/ask-url - URL: {}, Query: {}", request.getUrl(), request.getQuery());

        try {
            String answer = ragService.answerUrlQuery(request.getUrl(), request.getQuery());

            // Get content preview for debugging
            String contentPreview = null;
            try {
                String scrapedContent = webScrapingService.scrapeWebsite(request.getUrl());
                contentPreview = webScrapingService.getContentPreview(scrapedContent);
            } catch (Exception e) {
                log.warn("Failed to get content preview: {}", e.getMessage());
            }

            UrlQueryResponse response = UrlQueryResponse.builder()
                    .url(request.getUrl())
                    .query(request.getQuery())
                    .answer(answer)
                    .scrapedContentPreview(contentPreview)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing URL query", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to process your request: " + e.getMessage());
            errorResponse.put("url", request.getUrl());
            errorResponse.put("query", request.getQuery());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Health check for RAG service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        log.info("GET /api/queries/health");

        boolean healthy = ragService.isHealthy();

        Map<String, Object> response = new HashMap<>();
        response.put("status", healthy ? "UP" : "DOWN");
        response.put("service", "RAG Query Service");

        return ResponseEntity.ok(response);
    }
}
