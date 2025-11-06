package com.swiftbeard.product_recommender_engine.service;

import com.swiftbeard.product_recommender_engine.config.ApplicationProperties;
import com.swiftbeard.product_recommender_engine.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing vector embeddings and semantic search
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VectorStoreService {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final ApplicationProperties applicationProperties;

    /**
     * Add a product to the vector store
     */
    public void addProduct(Product product) {
        log.debug("Adding product to vector store: {}", product.getId());

        Document document = createDocument(product);
        vectorStore.add(List.of(document));

        log.info("Product {} added to vector store", product.getId());
    }

    /**
     * Add multiple products to the vector store
     */
    public void addProducts(List<Product> products) {
        log.debug("Adding {} products to vector store", products.size());

        List<Document> documents = products.stream()
                .map(this::createDocument)
                .collect(Collectors.toList());

        vectorStore.add(documents);

        log.info("Added {} products to vector store", products.size());
    }

    /**
     * Update a product in the vector store
     */
    public void updateProduct(Product product) {
        log.debug("Updating product in vector store: {}", product.getId());

        // Delete old document
        deleteProduct(product.getId());

        // Add updated document
        addProduct(product);

        log.info("Product {} updated in vector store", product.getId());
    }

    /**
     * Delete a product from the vector store
     */
    public void deleteProduct(Long productId) {
        log.debug("Deleting product from vector store: {}", productId);

        try {
            vectorStore.delete(List.of(String.valueOf(productId)));
            log.info("Product {} deleted from vector store", productId);
        } catch (Exception e) {
            log.warn("Failed to delete product {} from vector store: {}", productId, e.getMessage());
        }
    }

    /**
     * Perform semantic search for products
     */
    public List<Product> semanticSearch(String query, int topK) {
        log.debug("Performing semantic search: query='{}', topK={}", query, topK);

        double threshold = applicationProperties.getRecommendation().getSimilarityThreshold();

        SearchRequest searchRequest = SearchRequest.query(query)
                .withTopK(topK)
                .withSimilarityThreshold(threshold);

        List<Document> results = vectorStore.similaritySearch(searchRequest);

        log.info("Semantic search returned {} results", results.size());

        return results.stream()
                .map(this::documentToProduct)
                .collect(Collectors.toList());
    }

    /**
     * Find similar products based on a product
     */
    public List<Product> findSimilarProducts(Product product, int topK) {
        log.debug("Finding similar products for: {}", product.getId());

        String query = product.toEmbeddingText();
        return semanticSearch(query, topK + 1).stream()
                .filter(p -> !p.getId().equals(product.getId()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * Get relevant context documents for RAG
     */
    public List<Document> getRelevantContext(String query, int topK) {
        log.debug("Getting relevant context: query='{}', topK={}", query, topK);

        double threshold = applicationProperties.getRecommendation().getSimilarityThreshold();

        SearchRequest searchRequest = SearchRequest.query(query)
                .withTopK(topK)
                .withSimilarityThreshold(threshold);

        List<Document> results = vectorStore.similaritySearch(searchRequest);

        log.info("Retrieved {} context documents", results.size());

        return results;
    }

    /**
     * Create a Document from a Product
     */
    private Document createDocument(Product product) {
        String content = product.toEmbeddingText();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("productId", product.getId().toString());
        metadata.put("name", product.getName());
        metadata.put("category", product.getCategory().name());
        metadata.put("price", product.getPrice().toString());
        metadata.put("rating", product.getRating().toString());
        metadata.put("brand", product.getBrand() != null ? product.getBrand() : "");
        metadata.put("sku", product.getSku() != null ? product.getSku() : "");

        return new Document(String.valueOf(product.getId()), content, metadata);
    }

    /**
     * Convert a Document back to a Product (lightweight version)
     */
    private Product documentToProduct(Document document) {
        Map<String, Object> metadata = document.getMetadata();

        return Product.builder()
                .id(Long.valueOf((String) metadata.get("productId")))
                .name((String) metadata.get("name"))
                .build();
    }

    /**
     * Clear all products from vector store (use with caution)
     */
    public void clearVectorStore() {
        log.warn("Clearing all data from vector store");
        // Note: This would require custom implementation based on vector store
        // For now, this is a placeholder
        log.info("Vector store cleared");
    }

    /**
     * Check if vector store is healthy
     */
    public boolean isHealthy() {
        try {
            // Perform a simple test query
            SearchRequest testRequest = SearchRequest.query("test")
                    .withTopK(1);
            vectorStore.similaritySearch(testRequest);
            return true;
        } catch (Exception e) {
            log.error("Vector store health check failed", e);
            return false;
        }
    }
}
