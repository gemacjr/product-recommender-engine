package com.swiftbeard.product_recommender_engine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftbeard.product_recommender_engine.TestDataFactory;
import com.swiftbeard.product_recommender_engine.dto.RecommendationRequest;
import com.swiftbeard.product_recommender_engine.model.Product;
import com.swiftbeard.product_recommender_engine.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecommendationController.class)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RecommendationService recommendationService;

    @Test
    void getSimilarProducts_ShouldReturnSimilarProducts() throws Exception {
        // Arrange
        List<Product> similarProducts = Arrays.asList(
                TestDataFactory.createElectronicsProduct(),
                TestDataFactory.createClothingProduct()
        );
        when(recommendationService.getSimilarProducts(1L, 10)).thenReturn(similarProducts);

        // Act & Assert
        mockMvc.perform(get("/api/recommendations/similar/1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));

        verify(recommendationService).getSimilarProducts(1L, 10);
    }

    @Test
    void getRecommendationsByQuery_ShouldReturnRecommendations() throws Exception {
        // Arrange
        List<Product> recommendations = Arrays.asList(TestDataFactory.createElectronicsProduct());
        when(recommendationService.getRecommendationsByQuery("wireless headphones", 10))
                .thenReturn(recommendations);

        // Act & Assert
        mockMvc.perform(get("/api/recommendations/search")
                        .param("query", "wireless headphones")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(recommendationService).getRecommendationsByQuery("wireless headphones", 10);
    }

    @Test
    void getPersonalizedRecommendations_ShouldReturnPersonalizedProducts() throws Exception {
        // Arrange
        RecommendationRequest request = TestDataFactory.createRecommendationRequest();
        List<Product> recommendations = TestDataFactory.createProductList();
        when(recommendationService.getPersonalizedRecommendations(anyString(), anyInt()))
                .thenReturn(recommendations);

        // Act & Assert
        mockMvc.perform(post("/api/recommendations/personalized")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));

        verify(recommendationService).getPersonalizedRecommendations(anyString(), anyInt());
    }

    @Test
    void getRecommendationsFromHistory_ShouldReturnRecommendations() throws Exception {
        // Arrange
        List<Long> viewedIds = Arrays.asList(1L, 2L, 3L);
        List<Product> recommendations = Arrays.asList(TestDataFactory.createElectronicsProduct());
        when(recommendationService.getRecommendationsFromHistory(anyList(), anyInt()))
                .thenReturn(recommendations);

        // Act & Assert
        mockMvc.perform(post("/api/recommendations/from-history")
                        .param("limit", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(viewedIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(recommendationService).getRecommendationsFromHistory(anyList(), eq(10));
    }

    @Test
    void getComplementaryProducts_ShouldReturnComplementaryItems() throws Exception {
        // Arrange
        List<Product> complementary = Arrays.asList(TestDataFactory.createElectronicsProduct());
        when(recommendationService.getComplementaryProducts(1L, 10)).thenReturn(complementary);

        // Act & Assert
        mockMvc.perform(get("/api/recommendations/complementary/1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(recommendationService).getComplementaryProducts(1L, 10);
    }

    @Test
    void getTrendingProducts_ShouldReturnTrendingProducts() throws Exception {
        // Arrange
        List<Product> trending = TestDataFactory.createProductList();
        when(recommendationService.getTrendingProducts(10)).thenReturn(trending);

        // Act & Assert
        mockMvc.perform(get("/api/recommendations/trending")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));

        verify(recommendationService).getTrendingProducts(10);
    }

    @Test
    void getDiverseRecommendations_ShouldReturnDiverseProducts() throws Exception {
        // Arrange
        List<Product> diverse = TestDataFactory.createProductList();
        when(recommendationService.getDiverseRecommendations(anyString(), anyInt()))
                .thenReturn(diverse);

        // Act & Assert
        mockMvc.perform(get("/api/recommendations/diverse")
                        .param("interests", "gaming and books")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));

        verify(recommendationService).getDiverseRecommendations(anyString(), eq(10));
    }

    @Test
    void getBudgetAlternatives_ShouldReturnCheaperOptions() throws Exception {
        // Arrange
        List<Product> budgetOptions = Arrays.asList(TestDataFactory.createElectronicsProduct());
        when(recommendationService.getBudgetAlternatives(1L, 10)).thenReturn(budgetOptions);

        // Act & Assert
        mockMvc.perform(get("/api/recommendations/budget-alternatives/1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(recommendationService).getBudgetAlternatives(1L, 10);
    }

    @Test
    void getPremiumAlternatives_ShouldReturnPremiumOptions() throws Exception {
        // Arrange
        List<Product> premiumOptions = Arrays.asList(TestDataFactory.createElectronicsProduct());
        when(recommendationService.getPremiumAlternatives(1L, 10)).thenReturn(premiumOptions);

        // Act & Assert
        mockMvc.perform(get("/api/recommendations/premium-alternatives/1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(recommendationService).getPremiumAlternatives(1L, 10);
    }

    @Test
    void getCategoryRecommendations_ShouldReturnCategoryProducts() throws Exception {
        // Arrange
        List<Product> categoryProducts = Arrays.asList(TestDataFactory.createElectronicsProduct());
        when(recommendationService.getCategoryRecommendations(anyString(), anyString(), anyInt()))
                .thenReturn(categoryProducts);

        // Act & Assert
        mockMvc.perform(get("/api/recommendations/category")
                        .param("category", "Electronics")
                        .param("context", "gaming")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(recommendationService).getCategoryRecommendations(eq("Electronics"), eq("gaming"), eq(10));
    }
}
