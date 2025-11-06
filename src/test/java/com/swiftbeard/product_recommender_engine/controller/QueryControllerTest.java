package com.swiftbeard.product_recommender_engine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftbeard.product_recommender_engine.dto.QueryRequest;
import com.swiftbeard.product_recommender_engine.dto.UrlQueryRequest;
import com.swiftbeard.product_recommender_engine.service.RagService;
import com.swiftbeard.product_recommender_engine.service.WebScrapingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QueryController.class)
@DisplayName("QueryController Unit Tests")
class QueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RagService ragService;

    @MockBean
    private WebScrapingService webScrapingService;

    @Test
    @DisplayName("Should answer query successfully")
    void answerQuery_ShouldReturnAnswer() throws Exception {
        QueryRequest request = QueryRequest.builder()
                .query("What are the best headphones?")
                .userPreferences("noise cancellation")
                .build();

        when(ragService.answerQuery(anyString())).thenReturn("Based on your preferences, here are the best headphones...");

        mockMvc.perform(post("/api/queries/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query", is("What are the best headphones?")))
                .andExpect(jsonPath("$.answer", containsString("headphones")));

        verify(ragService).answerQuery("What are the best headphones?");
    }

    @Test
    @DisplayName("Should return recommendation with explanation")
    void getRecommendationWithExplanation_ShouldReturnDetailedRecommendation() throws Exception {
        QueryRequest request = QueryRequest.builder()
                .query("I need headphones for work")
                .userPreferences("comfortable, noise cancellation")
                .build();

        when(ragService.getRecommendationWithExplanation(anyString(), anyString()))
                .thenReturn("Based on your needs, I recommend...");

        mockMvc.perform(post("/api/queries/recommend-with-explanation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query", is("I need headphones for work")))
                .andExpect(jsonPath("$.recommendation", containsString("recommend")));

        verify(ragService).getRecommendationWithExplanation("I need headphones for work", "comfortable, noise cancellation");
    }

    @Test
    @DisplayName("Should compare products successfully")
    void compareProducts_ShouldReturnComparison() throws Exception {
        when(ragService.compareProducts(anyList())).thenReturn("Here is a detailed comparison of the products...");

        mockMvc.perform(post("/api/queries/compare-products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(1L, 2L, 3L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comparison", containsString("comparison")));

        verify(ragService).compareProducts(Arrays.asList(1L, 2L, 3L));
    }

    @Test
    @DisplayName("Should answer product FAQ")
    void answerProductFaq_ShouldReturnAnswer() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("question", "Is this suitable for running?");

        when(ragService.answerProductFaq(eq(1L), anyString()))
                .thenReturn("Yes, this product is suitable for running activities.");

        mockMvc.perform(post("/api/queries/product-faq/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is("1")))
                .andExpect(jsonPath("$.question", is("Is this suitable for running?")))
                .andExpect(jsonPath("$.answer", containsString("suitable")));

        verify(ragService).answerProductFaq(1L, "Is this suitable for running?");
    }

    @Test
    @DisplayName("Should return personalized suggestions")
    void getPersonalizedSuggestions_ShouldReturnSuggestions() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("userProfile", "fitness enthusiast, early 30s");
        requestBody.put("occasion", "marathon training");

        when(ragService.getPersonalizedSuggestions(anyString(), anyString()))
                .thenReturn("Here are personalized shopping suggestions for your marathon training...");

        mockMvc.perform(post("/api/queries/personalized-suggestions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userProfile", is("fitness enthusiast, early 30s")))
                .andExpect(jsonPath("$.occasion", is("marathon training")))
                .andExpect(jsonPath("$.suggestions", containsString("suggestions")));

        verify(ragService).getPersonalizedSuggestions("fitness enthusiast, early 30s", "marathon training");
    }

    @Test
    @DisplayName("Should answer URL query successfully")
    void answerUrlQuery_ShouldReturnAnswer() throws Exception {
        UrlQueryRequest request = UrlQueryRequest.builder()
                .url("https://example.com/shop")
                .query("is there a shirt in size XXL under 10 dollars")
                .build();

        String expectedAnswer = "Yes, we found a blue cotton shirt in size XXL for $8.99";
        String contentPreview = "Page Title: Example Shop\n\nProducts Found\n\nProduct 1:\nName: Blue Cotton Shirt...";

        when(ragService.answerUrlQuery(anyString(), anyString())).thenReturn(expectedAnswer);
        when(webScrapingService.scrapeWebsite(anyString())).thenReturn(contentPreview);
        when(webScrapingService.getContentPreview(anyString())).thenReturn(
                contentPreview.substring(0, Math.min(contentPreview.length(), 500)));

        mockMvc.perform(post("/api/queries/ask-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url", is(request.getUrl())))
                .andExpect(jsonPath("$.query", is(request.getQuery())))
                .andExpect(jsonPath("$.answer", is(expectedAnswer)));

        verify(ragService).answerUrlQuery(request.getUrl(), request.getQuery());
    }

    @Test
    @DisplayName("Should return error when URL query fails")
    void answerUrlQuery_ShouldReturnError_WhenFails() throws Exception {
        UrlQueryRequest request = UrlQueryRequest.builder()
                .url("https://example.com/shop")
                .query("query")
                .build();

        when(ragService.answerUrlQuery(anyString(), anyString()))
                .thenThrow(new Exception("Failed to scrape website"));

        mockMvc.perform(post("/api/queries/ask-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Failed")));

        verify(ragService).answerUrlQuery(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return health status")
    void healthCheck_ShouldReturnStatus() throws Exception {
        when(ragService.isHealthy()).thenReturn(true);

        mockMvc.perform(get("/api/queries/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.service", is("RAG Query Service")));

        verify(ragService).isHealthy();
    }

    @Test
    @DisplayName("Should return unhealthy status")
    void healthCheck_ShouldReturnUnhealthyWhenServiceDown() throws Exception {
        when(ragService.isHealthy()).thenReturn(false);

        mockMvc.perform(get("/api/queries/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DOWN")));

        verify(ragService).isHealthy();
    }
}

