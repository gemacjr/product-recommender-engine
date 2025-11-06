package com.swiftbeard.product_recommender_engine.controller;

import com.swiftbeard.product_recommender_engine.dto.QueryRequest;
import com.swiftbeard.product_recommender_engine.dto.QueryResponse;
import com.swiftbeard.product_recommender_engine.service.RagService;
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
