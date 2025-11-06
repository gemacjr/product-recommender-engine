package com.swiftbeard.product_recommender_engine.service;

import com.swiftbeard.product_recommender_engine.TestDataFactory;
import com.swiftbeard.product_recommender_engine.config.ApplicationProperties;
import com.swiftbeard.product_recommender_engine.model.Category;
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
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RecommendationService Unit Tests")
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

        when(applicationProperties.getRecommendation()).thenReturn(recommendationConfig);
        when(recommendationConfig.getMaxResults()).thenReturn(20);
    }

    @Test
    @DisplayName("Should return similar products for a given product")
    void getSimilarProducts_ShouldReturnSimilarProducts() {
        List<Product> similarProducts = Arrays.asList(
                TestDataFactory.createProduct(2L, "Bluetooth Headphones", Category.ELECTRONICS, new BigDecimal("89.99")),
                TestDataFactory.createProduct(3L, "Gaming Headset", Category.ELECTRONICS, new BigDecimal("129.99"))
        );

        when(productService.getProductById(1L)).thenReturn(testProduct);
        when(vectorStoreService.findSimilarProducts(any(Product.class), eq(10))).thenReturn(similarProducts);

        List<Product> result = recommendationService.getSimilarProducts(1L, 10);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(productService).getProductById(1L);
        verify(vectorStoreService).findSimilarProducts(any(Product.class), eq(10));
    }

    @Test
    @DisplayName("Should respect max results limit")
    void getSimilarProducts_ShouldRespectMaxResultsLimit() {
        when(productService.getProductById(1L)).thenReturn(testProduct);
        when(vectorStoreService.findSimilarProducts(any(Product.class), eq(20))).thenReturn(Collections.emptyList());

        recommendationService.getSimilarProducts(1L, 100);

        verify(vectorStoreService).findSimilarProducts(any(Product.class), eq(20));
    }

    @Test
    @DisplayName("Should return recommendations by query")
    void getRecommendationsByQuery_ShouldReturnMatchingProducts() {
        List<Product> recommendations = Arrays.asList(
                TestDataFactory.createElectronicsProduct(),
                TestDataFactory.createProduct(4L, "Gaming Mouse", Category.ELECTRONICS, new BigDecimal("79.99"))
        );

        when(vectorStoreService.semanticSearch(eq("wireless gaming accessories"), eq(10)))
                .thenReturn(recommendations);

        List<Product> result = recommendationService.getRecommendationsByQuery("wireless gaming accessories", 10);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(vectorStoreService).semanticSearch(eq("wireless gaming accessories"), eq(10));
    }

    @Test
    @DisplayName("Should return personalized recommendations with enriched query")
    void getPersonalizedRecommendations_ShouldEnrichPreferencesAndReturnResults() {
        List<Product> recommendations = Arrays.asList(testProduct);
        when(vectorStoreService.semanticSearch(anyString(), anyInt())).thenReturn(recommendations);

        List<Product> result = recommendationService.getPersonalizedRecommendations("I love gaming and technology", 5);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(vectorStoreService).semanticSearch(
                argThat(query -> query.contains("gaming and technology")),
                anyInt()
        );
    }

    @Test
    @DisplayName("Should handle null preferences in personalized recommendations")
    void getPersonalizedRecommendations_ShouldHandleNullPreferences() {
        when(vectorStoreService.semanticSearch(anyString(), anyInt())).thenReturn(testProducts);

        List<Product> result = recommendationService.getPersonalizedRecommendations(null, 5);

        assertThat(result).isNotNull();
        verify(vectorStoreService).semanticSearch(
                argThat(query -> query.contains("popular high-quality products")),
                anyInt()
        );
    }

    @Test
    @DisplayName("Should return recommendations from browsing history")
    void getRecommendationsFromHistory_ShouldReturnRecommendations() {
        List<Long> viewedIds = Arrays.asList(1L, 2L, 3L);
        List<Product> similarToFirst = Arrays.asList(
                TestDataFactory.createProduct(4L, "Product 4", Category.ELECTRONICS, new BigDecimal("99.99")),
                TestDataFactory.createProduct(5L, "Product 5", Category.ELECTRONICS, new BigDecimal("89.99"))
        );

        when(productService.getProductById(1L)).thenReturn(testProduct);
        when(vectorStoreService.findSimilarProducts(any(Product.class), anyInt())).thenReturn(similarToFirst);

        List<Product> result = recommendationService.getRecommendationsFromHistory(viewedIds, 5);

        assertThat(result).isNotNull();
        assertThat(result).allMatch(p -> !viewedIds.contains(p.getId()));
        verify(productService, atLeastOnce()).getProductById(anyLong());
    }

    @Test
    @DisplayName("Should return empty list when browsing history is empty")
    void getRecommendationsFromHistory_ShouldReturnEmptyList_WhenHistoryIsEmpty() {
        List<Product> result = recommendationService.getRecommendationsFromHistory(Collections.emptyList(), 5);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when browsing history is null")
    void getRecommendationsFromHistory_ShouldReturnEmptyList_WhenHistoryIsNull() {
        List<Product> result = recommendationService.getRecommendationsFromHistory(null, 5);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return complementary products excluding original")
    void getComplementaryProducts_ShouldReturnComplementaryItems() {
        Product headphones = TestDataFactory.createElectronicsProduct();
        List<Product> complementary = Arrays.asList(
                headphones,
                TestDataFactory.createProduct(2L, "Headphone Case", Category.ELECTRONICS, new BigDecimal("19.99")),
                TestDataFactory.createProduct(3L, "Audio Cable", Category.ELECTRONICS, new BigDecimal("9.99"))
        );

        when(productService.getProductById(1L)).thenReturn(headphones);
        when(vectorStoreService.semanticSearch(anyString(), anyInt())).thenReturn(complementary);

        List<Product> result = recommendationService.getComplementaryProducts(1L, 5);

        assertThat(result).isNotNull();
        assertThat(result).doesNotContain(headphones);
        verify(productService).getProductById(1L);
        verify(vectorStoreService).semanticSearch(anyString(), anyInt());
    }

    @Test
    @DisplayName("Should return trending products")
    void getTrendingProducts_ShouldReturnTopRatedProducts() {
        when(productService.getTopRatedProducts(0, 10)).thenReturn(new PageImpl<>(testProducts));

        List<Product> result = recommendationService.getTrendingProducts(10);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(5);
        verify(productService).getTopRatedProducts(0, 10);
    }

    @Test
    @DisplayName("Should return category recommendations with user context")
    void getCategoryRecommendations_ShouldReturnCategorySpecificProducts() {
        List<Product> electronicsProducts = Arrays.asList(
                TestDataFactory.createElectronicsProduct(),
                TestDataFactory.createProduct(4L, "Gaming Mouse", Category.ELECTRONICS, new BigDecimal("79.99"))
        );

        when(vectorStoreService.semanticSearch(anyString(), anyInt())).thenReturn(electronicsProducts);

        List<Product> result = recommendationService.getCategoryRecommendations("Electronics", "gaming", 10);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(vectorStoreService).semanticSearch(
                argThat(query -> query.contains("Electronics") && query.contains("gaming")),
                anyInt()
        );
    }

    @Test
    @DisplayName("Should return diverse recommendations from multiple categories")
    void getDiverseRecommendations_ShouldReturnProductsFromMultipleCategories() {
        List<Product> diverseProducts = Arrays.asList(
                TestDataFactory.createElectronicsProduct(),
                TestDataFactory.createClothingProduct(),
                TestDataFactory.createBookProduct(),
                TestDataFactory.createProduct(4L, "Gaming Mouse", Category.ELECTRONICS, new BigDecimal("79.99")),
                TestDataFactory.createProduct(5L, "Running Shoes", Category.SPORTS_OUTDOORS, new BigDecimal("119.99"))
        );

        when(vectorStoreService.semanticSearch(anyString(), anyInt())).thenReturn(diverseProducts);

        List<Product> result = recommendationService.getDiverseRecommendations("variety of interests", 5);

        assertThat(result).isNotNull();
        assertThat(result.size()).isGreaterThan(0);
        long uniqueCategories = result.stream()
                .map(Product::getCategory)
                .distinct()
                .count();
        assertThat(uniqueCategories).isGreaterThan(1);
        verify(vectorStoreService).semanticSearch(anyString(), anyInt());
    }

    @Test
    @DisplayName("Should return budget alternatives cheaper than original")
    void getBudgetAlternatives_ShouldReturnCheaperProducts() {
        Product expensiveProduct = TestDataFactory.createProduct(1L, "Premium Headphones", Category.ELECTRONICS, new BigDecimal("199.99"));
        List<Product> similarProducts = Arrays.asList(
                expensiveProduct,
                TestDataFactory.createProduct(2L, "Mid-range Headphones", Category.ELECTRONICS, new BigDecimal("99.99")),
                TestDataFactory.createProduct(3L, "Budget Headphones", Category.ELECTRONICS, new BigDecimal("49.99")),
                TestDataFactory.createProduct(4L, "Luxury Headphones", Category.ELECTRONICS, new BigDecimal("299.99"))
        );

        when(productService.getProductById(1L)).thenReturn(expensiveProduct);
        when(vectorStoreService.findSimilarProducts(any(Product.class), eq(10))).thenReturn(similarProducts);

        List<Product> result = recommendationService.getBudgetAlternatives(1L, 5);

        assertThat(result).isNotNull();
        assertThat(result).allMatch(p -> p.getPrice().compareTo(expensiveProduct.getPrice()) < 0);
        verify(productService, atLeastOnce()).getProductById(1L);
    }

    @Test
    @DisplayName("Should return empty list when no cheaper alternatives exist")
    void getBudgetAlternatives_ShouldReturnEmptyList_WhenNoCheaperOptionsExist() {
        Product cheapestProduct = TestDataFactory.createProduct(1L, "Cheapest Headphones", Category.ELECTRONICS, new BigDecimal("19.99"));
        List<Product> similarProducts = Arrays.asList(
                TestDataFactory.createProduct(2L, "Mid-range Headphones", Category.ELECTRONICS, new BigDecimal("99.99")),
                TestDataFactory.createProduct(3L, "Premium Headphones", Category.ELECTRONICS, new BigDecimal("199.99"))
        );

        when(productService.getProductById(1L)).thenReturn(cheapestProduct);
        when(vectorStoreService.findSimilarProducts(any(Product.class), eq(10))).thenReturn(similarProducts);

        List<Product> result = recommendationService.getBudgetAlternatives(1L, 5);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return premium alternatives more expensive than original")
    void getPremiumAlternatives_ShouldReturnMoreExpensiveProducts() {
        Product budgetProduct = TestDataFactory.createProduct(1L, "Budget Headphones", Category.ELECTRONICS, new BigDecimal("49.99"));
        List<Product> similarProducts = Arrays.asList(
                budgetProduct,
                TestDataFactory.createProduct(2L, "Mid-range Headphones", Category.ELECTRONICS, new BigDecimal("99.99")),
                TestDataFactory.createProduct(3L, "Premium Headphones", Category.ELECTRONICS, new BigDecimal("199.99")),
                TestDataFactory.createProduct(4L, "Basic Headphones", Category.ELECTRONICS, new BigDecimal("29.99"))
        );

        when(productService.getProductById(1L)).thenReturn(budgetProduct);
        when(vectorStoreService.findSimilarProducts(any(Product.class), eq(10))).thenReturn(similarProducts);

        List<Product> result = recommendationService.getPremiumAlternatives(1L, 5);

        assertThat(result).isNotNull();
        assertThat(result).allMatch(p -> p.getPrice().compareTo(budgetProduct.getPrice()) > 0);
        verify(productService, atLeastOnce()).getProductById(1L);
    }

    @Test
    @DisplayName("Should return empty list when no more expensive alternatives exist")
    void getPremiumAlternatives_ShouldReturnEmptyList_WhenNoMoreExpensiveOptionsExist() {
        Product mostExpensiveProduct = TestDataFactory.createProduct(1L, "Luxury Headphones", Category.ELECTRONICS, new BigDecimal("999.99"));
        List<Product> similarProducts = Arrays.asList(
                TestDataFactory.createProduct(2L, "Budget Headphones", Category.ELECTRONICS, new BigDecimal("49.99")),
                TestDataFactory.createProduct(3L, "Mid-range Headphones", Category.ELECTRONICS, new BigDecimal("99.99"))
        );

        when(productService.getProductById(1L)).thenReturn(mostExpensiveProduct);
        when(vectorStoreService.findSimilarProducts(any(Product.class), eq(10))).thenReturn(similarProducts);

        List<Product> result = recommendationService.getPremiumAlternatives(1L, 5);

        assertThat(result).isEmpty();
    }
}

