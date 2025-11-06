package com.swiftbeard.product_recommender_engine.service;

import com.swiftbeard.product_recommender_engine.TestDataFactory;
import com.swiftbeard.product_recommender_engine.config.ApplicationProperties;
import com.swiftbeard.product_recommender_engine.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

        // Setup application properties mock
        when(applicationProperties.getRecommendation()).thenReturn(recommendationConfig);
        when(recommendationConfig.getSimilarityThreshold()).thenReturn(0.7);

        // Setup test documents
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
    void addProduct_ShouldAddProductToVectorStore() {
        // Arrange
        doNothing().when(vectorStore).add(anyList());

        // Act
        vectorStoreService.addProduct(testProduct);

        // Assert
        verify(vectorStore).add(argThat(docs ->
                docs.size() == 1 &&
                        docs.get(0).getId().equals("1")
        ));
    }

    @Test
    void addProducts_ShouldAddMultipleProductsToVectorStore() {
        // Arrange
        doNothing().when(vectorStore).add(anyList());

        // Act
        vectorStoreService.addProducts(testProducts);

        // Assert
        verify(vectorStore).add(argThat(docs -> docs.size() == 5));
    }

    @Test
    void updateProduct_ShouldDeleteAndAddProduct() {
        // Arrange
        doNothing().when(vectorStore).delete(anyList());
        doNothing().when(vectorStore).add(anyList());

        // Act
        vectorStoreService.updateProduct(testProduct);

        // Assert
        verify(vectorStore).delete(argThat((List<String> ids) -> ids.contains("1")));
        verify(vectorStore).add(argThat(docs -> docs.size() == 1));
    }

    @Test
    void deleteProduct_ShouldRemoveProductFromVectorStore() {
        // Arrange
        doNothing().when(vectorStore).delete(anyList());

        // Act
        vectorStoreService.deleteProduct(1L);

        // Assert
        verify(vectorStore).delete(argThat((List<String> ids) ->
                ids.size() == 1 && ids.get(0).equals("1")
        ));
    }

    @Test
    void deleteProduct_ShouldHandleExceptionGracefully() {
        // Arrange
        doThrow(new RuntimeException("Delete failed")).when(vectorStore).delete(anyList());

        // Act - should not throw exception
        vectorStoreService.deleteProduct(1L);

        // Assert
        verify(vectorStore).delete(anyList());
    }

    @Test
    void semanticSearch_ShouldReturnMatchingProducts() {
        // Arrange
        when(vectorStore.similaritySearch(anyString())).thenReturn(testDocuments);

        // Act
        List<Product> results = vectorStoreService.semanticSearch("wireless headphones", 10);

        // Assert
        assertThat(results).isNotNull();
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getId()).isEqualTo(1L);
        assertThat(results.get(0).getName()).isEqualTo("Wireless Headphones");
        verify(vectorStore).similaritySearch("wireless headphones");
    }

    @Test
    void semanticSearch_ShouldReturnEmptyList_WhenNoResults() {
        // Arrange
        when(vectorStore.similaritySearch(anyString())).thenReturn(Collections.emptyList());

        // Act
        List<Product> results = vectorStoreService.semanticSearch("nonexistent product", 10);

        // Assert
        assertThat(results).isEmpty();
        verify(vectorStore).similaritySearch(anyString());
    }

    @Test
    void findSimilarProducts_ShouldReturnSimilarProductsExcludingOriginal() {
        // Arrange
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

        // Act
        List<Product> results = vectorStoreService.findSimilarProducts(testProduct, 5);

        // Assert
        assertThat(results).isNotNull();
        assertThat(results).hasSizeLessThanOrEqualTo(5);
        assertThat(results).noneMatch(p -> p.getId().equals(1L)); // Should exclude original product
        verify(vectorStore).similaritySearch(anyString());
    }

    @Test
    void getRelevantContext_ShouldReturnDocuments() {
        // Arrange
        when(vectorStore.similaritySearch(anyString())).thenReturn(testDocuments);

        // Act
        List<Document> results = vectorStoreService.getRelevantContext("headphones", 5);

        // Assert
        assertThat(results).isNotNull();
        assertThat(results).hasSize(2);
        assertThat(results).containsExactlyElementsOf(testDocuments);
        verify(vectorStore).similaritySearch("headphones");
    }

    @Test
    void isHealthy_ShouldReturnTrue_WhenVectorStoreResponds() {
        // Arrange
        when(vectorStore.similaritySearch(anyString())).thenReturn(Collections.emptyList());

        // Act
        boolean result = vectorStoreService.isHealthy();

        // Assert
        assertThat(result).isTrue();
        verify(vectorStore).similaritySearch("test");
    }

    @Test
    void isHealthy_ShouldReturnFalse_WhenVectorStoreThrowsException() {
        // Arrange
        when(vectorStore.similaritySearch(anyString()))
                .thenThrow(new RuntimeException("Vector store error"));

        // Act
        boolean result = vectorStoreService.isHealthy();

        // Assert
        assertThat(result).isFalse();
        verify(vectorStore).similaritySearch(anyString());
    }

    @Test
    void clearVectorStore_ShouldExecuteWithoutError() {
        // This is a placeholder method, so we just verify it doesn't throw
        // Act & Assert - should not throw exception
        vectorStoreService.clearVectorStore();
    }
}
