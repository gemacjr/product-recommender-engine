package com.swiftbeard.product_recommender_engine.service;

import com.swiftbeard.product_recommender_engine.TestDataFactory;
import com.swiftbeard.product_recommender_engine.config.ApplicationProperties;
import com.swiftbeard.product_recommender_engine.model.Category;
import com.swiftbeard.product_recommender_engine.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private ProductService productService;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private ApplicationProperties.RecommendationConfig recommendationConfig;

    @InjectMocks
    private RecommendationService recommendationService;

    private Product testProduct;
    private List<Product> testProducts;

    @BeforeEach
    void setUp() {
        testProduct = TestDataFactory.createElectronicsProduct();
        testProducts = TestDataFactory.createProductList();

        // Setup application properties mock
        when(applicationProperties.getRecommendation()).thenReturn(recommendationConfig);
        when(recommendationConfig.getMaxResults()).thenReturn(20);
    }

    @Test
    void getSimilarProducts_ShouldReturnSimilarProducts() {
        // Arrange
        List<Product> similarProducts = Arrays.asList(
                TestDataFactory.createProduct(2L, "Bluetooth Headphones", Category.ELECTRONICS, new BigDecimal("89.99")),
                TestDataFactory.createProduct(3L, "Gaming Headset", Category.ELECTRONICS, new BigDecimal("129.99"))
        );

        when(productService.getProductById(1L)).thenReturn(testProduct);
        when(vectorStoreService.findSimilarProducts(any(Product.class), anyInt())).thenReturn(similarProducts);
        when(productService.getProductById(2L)).thenReturn(similarProducts.get(0));
        when(productService.getProductById(3L)).thenReturn(similarProducts.get(1));

        // Act
        List<Product> result = recommendationService.getSimilarProducts(1L, 10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(p -> p.getCategory() == Category.ELECTRONICS);
        verify(productService).getProductById(1L);
        verify(vectorStoreService).findSimilarProducts(any(Product.class), anyInt());
    }

    @Test
    void getSimilarProducts_ShouldRespectMaxResultsLimit() {
        // Arrange
        when(productService.getProductById(1L)).thenReturn(testProduct);
        when(vectorStoreService.findSimilarProducts(any(Product.class), eq(20))).thenReturn(Collections.emptyList());

        // Act
        recommendationService.getSimilarProducts(1L, 100); // Request more than max

        // Assert
        verify(vectorStoreService).findSimilarProducts(any(Product.class), eq(20)); // Should be limited to max
    }

    @Test
    void getRecommendationsByQuery_ShouldReturnMatchingProducts() {
        // Arrange
        List<Product> recommendations = Arrays.asList(
                TestDataFactory.createElectronicsProduct(),
                TestDataFactory.createProduct(4L, "Gaming Mouse", Category.ELECTRONICS, new BigDecimal("79.99"))
        );

        when(vectorStoreService.semanticSearch(eq("wireless gaming accessories"), anyInt()))
                .thenReturn(recommendations);

        // Act
        List<Product> result = recommendationService.getRecommendationsByQuery("wireless gaming accessories", 10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(vectorStoreService).semanticSearch(eq("wireless gaming accessories"), anyInt());
    }

    @Test
    void getPersonalizedRecommendations_ShouldEnrichPreferencesAndReturnResults() {
        // Arrange
        List<Product> recommendations = Arrays.asList(testProduct);
        when(vectorStoreService.semanticSearch(anyString(), anyInt())).thenReturn(recommendations);

        // Act
        List<Product> result = recommendationService.getPersonalizedRecommendations("I love gaming and technology", 5);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(vectorStoreService).semanticSearch(
                argThat(query -> query.contains("gaming and technology")),
                anyInt()
        );
    }

    @Test
    void getPersonalizedRecommendations_ShouldHandleNullPreferences() {
        // Arrange
        when(vectorStoreService.semanticSearch(anyString(), anyInt())).thenReturn(testProducts);

        // Act
        List<Product> result = recommendationService.getPersonalizedRecommendations(null, 5);

        // Assert
        assertThat(result).isNotNull();
        verify(vectorStoreService).semanticSearch(
                argThat(query -> query.contains("popular high-quality products")),
                anyInt()
        );
    }

    @Test
    void getRecommendationsFromHistory_ShouldReturnRecommendations() {
        // Arrange
        List<Long> viewedIds = Arrays.asList(1L, 2L, 3L);
        List<Product> similarToFirst = Arrays.asList(
                TestDataFactory.createProduct(4L, "Product 4", Category.ELECTRONICS, new BigDecimal("99.99")),
                TestDataFactory.createProduct(5L, "Product 5", Category.ELECTRONICS, new BigDecimal("89.99"))
        );

        when(productService.getProductById(1L)).thenReturn(testProduct);
        when(vectorStoreService.findSimilarProducts(any(Product.class), anyInt())).thenReturn(similarToFirst);
        when(productService.getProductById(4L)).thenReturn(similarToFirst.get(0));
        when(productService.getProductById(5L)).thenReturn(similarToFirst.get(1));

        // Act
        List<Product> result = recommendationService.getRecommendationsFromHistory(viewedIds, 5);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).allMatch(p -> !viewedIds.contains(p.getId())); // Should not include viewed products
        verify(productService, atLeastOnce()).getProductById(anyLong());
    }

    @Test
    void getRecommendationsFromHistory_ShouldReturnEmptyList_WhenHistoryIsEmpty() {
        // Act
        List<Product> result = recommendationService.getRecommendationsFromHistory(Collections.emptyList(), 5);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getRecommendationsFromHistory_ShouldReturnEmptyList_WhenHistoryIsNull() {
        // Act
        List<Product> result = recommendationService.getRecommendationsFromHistory(null, 5);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getComplementaryProducts_ShouldReturnComplementaryItems() {
        // Arrange
        Product headphones = TestDataFactory.createElectronicsProduct();
        List<Product> complementary = Arrays.asList(
                headphones,
                TestDataFactory.createProduct(2L, "Headphone Case", Category.ELECTRONICS, new BigDecimal("19.99")),
                TestDataFactory.createProduct(3L, "Audio Cable", Category.ELECTRONICS, new BigDecimal("9.99"))
        );

        when(productService.getProductById(1L)).thenReturn(headphones);
        when(vectorStoreService.semanticSearch(anyString(), anyInt())).thenReturn(complementary);

        // Act
        List<Product> result = recommendationService.getComplementaryProducts(1L, 5);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).doesNotContain(headphones); // Should not include the original product
        verify(productService).getProductById(1L);
        verify(vectorStoreService).semanticSearch(anyString(), anyInt());
    }

    @Test
    void getTrendingProducts_ShouldReturnTopRatedProducts() {
        // Arrange
        when(productService.getTopRatedProducts(0, 10)).thenReturn(new PageImpl<>(testProducts));

        // Act
        List<Product> result = recommendationService.getTrendingProducts(10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(5);
        verify(productService).getTopRatedProducts(0, 10);
    }

    @Test
    void getCategoryRecommendations_ShouldReturnCategorySpecificProducts() {
        // Arrange
        List<Product> electronicsProducts = Arrays.asList(
                TestDataFactory.createElectronicsProduct(),
                TestDataFactory.createProduct(4L, "Gaming Mouse", Category.ELECTRONICS, new BigDecimal("79.99"))
        );

        when(vectorStoreService.semanticSearch(anyString(), anyInt())).thenReturn(electronicsProducts);

        // Act
        List<Product> result = recommendationService.getCategoryRecommendations("Electronics", "gaming", 10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(vectorStoreService).semanticSearch(
                argThat(query -> query.contains("Electronics") && query.contains("gaming")),
                anyInt()
        );
    }

    @Test
    void getDiverseRecommendations_ShouldReturnProductsFromMultipleCategories() {
        // Arrange
        List<Product> diverseProducts = Arrays.asList(
                TestDataFactory.createElectronicsProduct(),
                TestDataFactory.createClothingProduct(),
                TestDataFactory.createBookProduct(),
                TestDataFactory.createProduct(4L, "Gaming Mouse", Category.ELECTRONICS, new BigDecimal("79.99")),
                TestDataFactory.createProduct(5L, "Running Shoes", Category.SPORTS_OUTDOORS, new BigDecimal("119.99"))
        );

        when(vectorStoreService.semanticSearch(anyString(), anyInt())).thenReturn(diverseProducts);

        // Act
        List<Product> result = recommendationService.getDiverseRecommendations("variety of interests", 5);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.size()).isGreaterThan(0);
        // Should have products from different categories
        long uniqueCategories = result.stream()
                .map(Product::getCategory)
                .distinct()
                .count();
        assertThat(uniqueCategories).isGreaterThan(1);
        verify(vectorStoreService).semanticSearch(anyString(), anyInt());
    }

    @Test
    void getBudgetAlternatives_ShouldReturnCheaperProducts() {
        // Arrange
        Product expensiveProduct = TestDataFactory.createProduct(1L, "Premium Headphones", Category.ELECTRONICS, new BigDecimal("199.99"));
        List<Product> similarProducts = Arrays.asList(
                expensiveProduct,
                TestDataFactory.createProduct(2L, "Mid-range Headphones", Category.ELECTRONICS, new BigDecimal("99.99")),
                TestDataFactory.createProduct(3L, "Budget Headphones", Category.ELECTRONICS, new BigDecimal("49.99")),
                TestDataFactory.createProduct(4L, "Luxury Headphones", Category.ELECTRONICS, new BigDecimal("299.99"))
        );

        when(productService.getProductById(1L)).thenReturn(expensiveProduct);
        when(vectorStoreService.findSimilarProducts(any(Product.class), anyInt())).thenReturn(similarProducts);
        when(productService.getProductById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return similarProducts.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
        });

        // Act
        List<Product> result = recommendationService.getBudgetAlternatives(1L, 5);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).allMatch(p -> p.getPrice().compareTo(expensiveProduct.getPrice()) < 0);
        verify(productService).getProductById(1L);
    }

    @Test
    void getPremiumAlternatives_ShouldReturnMoreExpensiveProducts() {
        // Arrange
        Product budgetProduct = TestDataFactory.createProduct(1L, "Budget Headphones", Category.ELECTRONICS, new BigDecimal("49.99"));
        List<Product> similarProducts = Arrays.asList(
                budgetProduct,
                TestDataFactory.createProduct(2L, "Mid-range Headphones", Category.ELECTRONICS, new BigDecimal("99.99")),
                TestDataFactory.createProduct(3L, "Premium Headphones", Category.ELECTRONICS, new BigDecimal("199.99")),
                TestDataFactory.createProduct(4L, "Basic Headphones", Category.ELECTRONICS, new BigDecimal("29.99"))
        );

        when(productService.getProductById(1L)).thenReturn(budgetProduct);
        when(vectorStoreService.findSimilarProducts(any(Product.class), anyInt())).thenReturn(similarProducts);
        when(productService.getProductById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return similarProducts.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
        });

        // Act
        List<Product> result = recommendationService.getPremiumAlternatives(1L, 5);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).allMatch(p -> p.getPrice().compareTo(budgetProduct.getPrice()) > 0);
        verify(productService).getProductById(1L);
    }

    @Test
    void getBudgetAlternatives_ShouldReturnEmptyList_WhenNoCheaperOptionsExist() {
        // Arrange
        Product cheapestProduct = TestDataFactory.createProduct(1L, "Cheapest Headphones", Category.ELECTRONICS, new BigDecimal("19.99"));
        List<Product> similarProducts = Arrays.asList(
                TestDataFactory.createProduct(2L, "Mid-range Headphones", Category.ELECTRONICS, new BigDecimal("99.99")),
                TestDataFactory.createProduct(3L, "Premium Headphones", Category.ELECTRONICS, new BigDecimal("199.99"))
        );

        when(productService.getProductById(1L)).thenReturn(cheapestProduct);
        when(vectorStoreService.findSimilarProducts(any(Product.class), anyInt())).thenReturn(similarProducts);
        when(productService.getProductById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return similarProducts.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
        });

        // Act
        List<Product> result = recommendationService.getBudgetAlternatives(1L, 5);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getPremiumAlternatives_ShouldReturnEmptyList_WhenNoMoreExpensiveOptionsExist() {
        // Arrange
        Product mostExpensiveProduct = TestDataFactory.createProduct(1L, "Luxury Headphones", Category.ELECTRONICS, new BigDecimal("999.99"));
        List<Product> similarProducts = Arrays.asList(
                TestDataFactory.createProduct(2L, "Budget Headphones", Category.ELECTRONICS, new BigDecimal("49.99")),
                TestDataFactory.createProduct(3L, "Mid-range Headphones", Category.ELECTRONICS, new BigDecimal("99.99"))
        );

        when(productService.getProductById(1L)).thenReturn(mostExpensiveProduct);
        when(vectorStoreService.findSimilarProducts(any(Product.class), anyInt())).thenReturn(similarProducts);
        when(productService.getProductById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return similarProducts.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
        });

        // Act
        List<Product> result = recommendationService.getPremiumAlternatives(1L, 5);

        // Assert
        assertThat(result).isEmpty();
    }
}
