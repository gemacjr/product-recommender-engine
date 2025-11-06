package com.swiftbeard.product_recommender_engine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftbeard.product_recommender_engine.TestDataFactory;
import com.swiftbeard.product_recommender_engine.dto.RecommendationRequest;
import com.swiftbeard.product_recommender_engine.model.Product;
import com.swiftbeard.product_recommender_engine.service.RecommendationService;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecommendationController.class)
@DisplayName("RecommendationController Unit Tests")
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RecommendationService recommendationService;

    @Test
    @DisplayName("Should return similar products")
    void getSimilarProducts_ShouldReturnSimilarProducts() throws Exception {
        List<Product> similar = Arrays.asList(TestDataFactory.createElectronicsProduct());
        when(recommendationService.getSimilarProducts(1L, 10)).thenReturn(similar);

        mockMvc.perform(get("/api/recommendations/similar/1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(recommendationService).getSimilarProducts(1L, 10);
    }

    @Test
    @DisplayName("Should return recommendations by query")
    void getRecommendationsByQuery_ShouldReturnRecommendations() throws Exception {
        List<Product> recommendations = TestDataFactory.createProductList();
        when(recommendationService.getRecommendationsByQuery("wireless headphones", 10)).thenReturn(recommendations);

        mockMvc.perform(get("/api/recommendations/search")
                        .param("query", "wireless headphones")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));

        verify(recommendationService).getRecommendationsByQuery("wireless headphones", 10);
    }

    @Test
    @DisplayName("Should return personalized recommendations")
    void getPersonalizedRecommendations_ShouldReturnPersonalizedProducts() throws Exception {
        RecommendationRequest request = TestDataFactory.createRecommendationRequest();
        List<Product> recommendations = TestDataFactory.createProductList();
        when(recommendationService.getPersonalizedRecommendations(anyString(), anyInt()))
                .thenReturn(recommendations);

        mockMvc.perform(post("/api/recommendations/personalized")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));

        verify(recommendationService).getPersonalizedRecommendations(anyString(), anyInt());
    }

    @Test
    @DisplayName("Should return recommendations from browsing history")
    void getRecommendationsFromHistory_ShouldReturnRecommendations() throws Exception {
        RecommendationRequest request = RecommendationRequest.builder()
                .viewedProductIds(Arrays.asList(1L, 2L, 3L))
                .limit(10)
                .build();
        List<Product> recommendations = Arrays.asList(TestDataFactory.createElectronicsProduct());
        when(recommendationService.getRecommendationsFromHistory(anyList(), anyInt()))
                .thenReturn(recommendations);

        mockMvc.perform(post("/api/recommendations/from-history")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(recommendationService).getRecommendationsFromHistory(anyList(), eq(10));
    }

    @Test
    @DisplayName("Should return complementary products")
    void getComplementaryProducts_ShouldReturnComplementaryItems() throws Exception {
        List<Product> complementary = Arrays.asList(TestDataFactory.createElectronicsProduct());
        when(recommendationService.getComplementaryProducts(1L, 10)).thenReturn(complementary);

        mockMvc.perform(get("/api/recommendations/complementary/1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(recommendationService).getComplementaryProducts(1L, 10);
    }

    @Test
    @DisplayName("Should return trending products")
    void getTrendingProducts_ShouldReturnTrendingProducts() throws Exception {
        List<Product> trending = TestDataFactory.createProductList();
        when(recommendationService.getTrendingProducts(10)).thenReturn(trending);

        mockMvc.perform(get("/api/recommendations/trending")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));

        verify(recommendationService).getTrendingProducts(10);
    }

    @Test
    @DisplayName("Should return diverse recommendations")
    void getDiverseRecommendations_ShouldReturnDiverseProducts() throws Exception {
        List<Product> diverse = TestDataFactory.createProductList();
        when(recommendationService.getDiverseRecommendations(anyString(), anyInt())).thenReturn(diverse);

        mockMvc.perform(get("/api/recommendations/diverse")
                        .param("userInterests", "outdoor activities")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));

        verify(recommendationService).getDiverseRecommendations("outdoor activities", 10);
    }

    @Test
    @DisplayName("Should return budget alternatives")
    void getBudgetAlternatives_ShouldReturnBudgetAlternatives() throws Exception {
        List<Product> alternatives = Arrays.asList(TestDataFactory.createElectronicsProduct());
        when(recommendationService.getBudgetAlternatives(1L, 5)).thenReturn(alternatives);

        mockMvc.perform(get("/api/recommendations/budget-alternatives/1")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(recommendationService).getBudgetAlternatives(1L, 5);
    }

    @Test
    @DisplayName("Should return premium alternatives")
    void getPremiumAlternatives_ShouldReturnPremiumAlternatives() throws Exception {
        List<Product> alternatives = Arrays.asList(TestDataFactory.createElectronicsProduct());
        when(recommendationService.getPremiumAlternatives(1L, 5)).thenReturn(alternatives);

        mockMvc.perform(get("/api/recommendations/premium-alternatives/1")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(recommendationService).getPremiumAlternatives(1L, 5);
    }

    @Test
    @DisplayName("Should return category recommendations")
    void getCategoryRecommendations_ShouldReturnCategoryProducts() throws Exception {
        List<Product> categoryProducts = Arrays.asList(TestDataFactory.createElectronicsProduct());
        when(recommendationService.getCategoryRecommendations(anyString(), anyString(), anyInt()))
                .thenReturn(categoryProducts);

        mockMvc.perform(get("/api/recommendations/category/Electronics")
                        .param("userContext", "gaming")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(recommendationService).getCategoryRecommendations(eq("Electronics"), eq("gaming"), eq(10));
    }
}

