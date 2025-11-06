package com.swiftbeard.product_recommender_engine.service;

import com.swiftbeard.product_recommender_engine.TestDataFactory;
import com.swiftbeard.product_recommender_engine.config.ApplicationProperties;
import com.swiftbeard.product_recommender_engine.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VectorStoreService Unit Tests")
class VectorStoreServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private ApplicationProperties.RecommendationConfig recommendationConfig;

    @InjectMocks
    private VectorStoreService vectorStoreService;

    private Product testProduct;
    private List<Product> testProducts;
    private List<Document> testDocuments;

    @BeforeEach
    void setUp() {
        testProduct = TestDataFactory.createElectronicsProduct();
        testProducts = TestDataFactory.createProductList();

        when(applicationProperties.getRecommendation()).thenReturn(recommendationConfig);
        when(recommendationConfig.getSimilarityThreshold()).thenReturn(0.7);

        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("productId", "1");
        metadata1.put("name", "Wireless Headphones");
        metadata1.put("category", "ELECTRONICS");
        metadata1.put("price", "99.99");
        metadata1.put("rating", "4.5");
        metadata1.put("brand", "Test Brand");
        metadata1.put("sku", "SKU-1");

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("productId", "2");
        metadata2.put("name", "Cotton T-Shirt");
        metadata2.put("category", "CLOTHING");
        metadata2.put("price", "29.99");
        metadata2.put("rating", "4.5");
        metadata2.put("brand", "Test Brand");
        metadata2.put("sku", "SKU-2");

        testDocuments = Arrays.asList(
                new Document("1", "Product: Wireless Headphones", metadata1),
                new Document("2", "Product: Cotton T-Shirt", metadata2)
        );
    }

    @Test
    @DisplayName("Should add product to vector store")
    void addProduct_ShouldAddProductToVectorStore() {
        doNothing().when(vectorStore).add(anyList());

        vectorStoreService.addProduct(testProduct);

        verify(vectorStore).add(argThat(docs ->
                docs.size() == 1 &&
                        docs.get(0).getId().equals("1")
        ));
    }

    @Test
    @DisplayName("Should add multiple products to vector store")
    void addProducts_ShouldAddMultipleProductsToVectorStore() {
        doNothing().when(vectorStore).add(anyList());

        vectorStoreService.addProducts(testProducts);

        verify(vectorStore).add(argThat(docs -> docs.size() == 5));
    }

    @Test
    @DisplayName("Should update product by deleting and adding")
    void updateProduct_ShouldDeleteAndAddProduct() {
        doNothing().when(vectorStore).delete(anyList());
        doNothing().when(vectorStore).add(anyList());

        vectorStoreService.updateProduct(testProduct);

        verify(vectorStore).delete(argThat((List<String> ids) -> ids.contains("1")));
        verify(vectorStore).add(argThat(docs -> docs.size() == 1));
    }

    @Test
    @DisplayName("Should remove product from vector store")
    void deleteProduct_ShouldRemoveProductFromVectorStore() {
        doNothing().when(vectorStore).delete(anyList());

        vectorStoreService.deleteProduct(1L);

        verify(vectorStore).delete(argThat((List<String> ids) ->
                ids.size() == 1 && ids.get(0).equals("1")
        ));
    }

    @Test
    @DisplayName("Should handle exception gracefully when deleting product")
    void deleteProduct_ShouldHandleExceptionGracefully() {
        doThrow(new RuntimeException("Delete failed")).when(vectorStore).delete(anyList());

        vectorStoreService.deleteProduct(1L);

        verify(vectorStore).delete(anyList());
    }

    @Test
    @DisplayName("Should return matching products from semantic search")
    void semanticSearch_ShouldReturnMatchingProducts() {
        when(vectorStore.similaritySearch(anyString())).thenReturn(testDocuments);

        List<Product> results = vectorStoreService.semanticSearch("wireless headphones", 10);

        assertThat(results).isNotNull();
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getId()).isEqualTo(1L);
        assertThat(results.get(0).getName()).isEqualTo("Wireless Headphones");
        // Note: documentToProduct only sets id and name, not full product details
        verify(vectorStore).similaritySearch("wireless headphones");
    }

    @Test
    @DisplayName("Should return empty list when no search results")
    void semanticSearch_ShouldReturnEmptyList_WhenNoResults() {
        when(vectorStore.similaritySearch(anyString())).thenReturn(Collections.emptyList());

        List<Product> results = vectorStoreService.semanticSearch("nonexistent product", 10);

        assertThat(results).isEmpty();
        verify(vectorStore).similaritySearch(anyString());
    }

    @Test
    @DisplayName("Should limit search results to topK")
    void semanticSearch_ShouldLimitResultsToTopK() {
        List<Document> manyDocs = Arrays.asList(
                testDocuments.get(0), testDocuments.get(1),
                new Document("3", "Product 3", new HashMap<>()),
                new Document("4", "Product 4", new HashMap<>()),
                new Document("5", "Product 5", new HashMap<>())
        );
        when(vectorStore.similaritySearch(anyString())).thenReturn(manyDocs);

        List<Product> results = vectorStoreService.semanticSearch("query", 2);

        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("Should return similar products excluding original")
    void findSimilarProducts_ShouldReturnSimilarProductsExcludingOriginal() {
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("productId", "1");
        metadata1.put("name", "Wireless Headphones");

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("productId", "2");
        metadata2.put("name", "Bluetooth Headphones");

        Map<String, Object> metadata3 = new HashMap<>();
        metadata3.put("productId", "3");
        metadata3.put("name", "Gaming Headset");

        List<Document> allDocs = Arrays.asList(
                new Document("1", "Wireless Headphones", metadata1),
                new Document("2", "Bluetooth Headphones", metadata2),
                new Document("3", "Gaming Headset", metadata3)
        );

        when(vectorStore.similaritySearch(anyString())).thenReturn(allDocs);

        List<Product> results = vectorStoreService.findSimilarProducts(testProduct, 5);

        assertThat(results).isNotNull();
        assertThat(results).hasSizeLessThanOrEqualTo(5);
        assertThat(results).noneMatch(p -> p.getId().equals(1L));
        verify(vectorStore).similaritySearch(anyString());
    }

    @Test
    @DisplayName("Should return relevant context documents")
    void getRelevantContext_ShouldReturnDocuments() {
        when(vectorStore.similaritySearch(anyString())).thenReturn(testDocuments);

        List<Document> results = vectorStoreService.getRelevantContext("headphones", 5);

        assertThat(results).isNotNull();
        assertThat(results).hasSize(2);
        assertThat(results).containsExactlyElementsOf(testDocuments);
        verify(vectorStore).similaritySearch("headphones");
    }

    @Test
    @DisplayName("Should limit context to topK documents")
    void getRelevantContext_ShouldLimitToTopK() {
        List<Document> manyDocs = Arrays.asList(
                testDocuments.get(0), testDocuments.get(1),
                new Document("3", "Product 3", new HashMap<>())
        );
        when(vectorStore.similaritySearch(anyString())).thenReturn(manyDocs);

        List<Document> results = vectorStoreService.getRelevantContext("query", 2);

        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("Should return true when vector store responds")
    void isHealthy_ShouldReturnTrue_WhenVectorStoreResponds() {
        when(vectorStore.similaritySearch(anyString())).thenReturn(Collections.emptyList());

        boolean result = vectorStoreService.isHealthy();

        assertThat(result).isTrue();
        verify(vectorStore).similaritySearch("test");
    }

    @Test
    @DisplayName("Should return false when vector store throws exception")
    void isHealthy_ShouldReturnFalse_WhenVectorStoreThrowsException() {
        when(vectorStore.similaritySearch(anyString()))
                .thenThrow(new RuntimeException("Vector store error"));

        boolean result = vectorStoreService.isHealthy();

        assertThat(result).isFalse();
        verify(vectorStore).similaritySearch(anyString());
    }

    @Test
    @DisplayName("Should execute clear vector store without error")
    void clearVectorStore_ShouldExecuteWithoutError() {
        vectorStoreService.clearVectorStore();
        // Should not throw exception
    }
}

