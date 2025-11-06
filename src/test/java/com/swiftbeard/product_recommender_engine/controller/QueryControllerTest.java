package com.swiftbeard.product_recommender_engine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftbeard.product_recommender_engine.TestDataFactory;
import com.swiftbeard.product_recommender_engine.dto.QueryRequest;
import com.swiftbeard.product_recommender_engine.service.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QueryController.class)
class QueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RagService ragService;

    @Test
    void answerQuery_ShouldReturnAnswer() throws Exception {
        // Arrange
        QueryRequest request = TestDataFactory.createQueryRequest("What are the best headphones?");
        String answer = "Based on our products, the Wireless Headphones are highly recommended.";
        when(ragService.answerQuery(anyString())).thenReturn(answer);

        // Act & Assert
        mockMvc.perform(post("/api/queries/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query", is(request.getQuery())))
                .andExpect(jsonPath("$.answer", is(answer)));

        verify(ragService).answerQuery(request.getQuery());
    }

    @Test
    void getRecommendationWithExplanation_ShouldReturnDetailedRecommendation() throws Exception {
        // Arrange
        QueryRequest request = TestDataFactory.createQueryRequest("I need headphones for work");
        String recommendation = "I recommend the Wireless Headphones because they have noise cancellation.";
        when(ragService.getRecommendationWithExplanation(anyString(), anyString()))
                .thenReturn(recommendation);

        // Act & Assert
        mockMvc.perform(post("/api/queries/recommend-with-explanation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query", is(request.getQuery())))
                .andExpect(jsonPath("$.recommendation", is(recommendation)));

        verify(ragService).getRecommendationWithExplanation(anyString(), anyString());
    }

    @Test
    void compareProducts_ShouldReturnComparison() throws Exception {
        // Arrange
        List<Long> productIds = Arrays.asList(1L, 2L, 3L);
        String comparison = "Product 1 is better for gaming, while Product 2 is better for music.";
        when(ragService.compareProducts(anyList())).thenReturn(comparison);

        // Act & Assert
        mockMvc.perform(post("/api/queries/compare-products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comparison", is(comparison)));

        verify(ragService).compareProducts(anyList());
    }

    @Test
    void answerProductFaq_ShouldReturnAnswer() throws Exception {
        // Arrange
        String question = "Does it have warranty?";
        String answer = "Yes, it comes with a 1-year warranty.";
        when(ragService.answerProductFaq(eq(1L), anyString())).thenReturn(answer);

        // Act & Assert
        mockMvc.perform(post("/api/queries/product-faq/1")
                        .param("question", question))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question", is(question)))
                .andExpect(jsonPath("$.answer", is(answer)));

        verify(ragService).answerProductFaq(1L, question);
    }

    @Test
    void getPersonalizedSuggestions_ShouldReturnSuggestions() throws Exception {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("userProfile", "tech enthusiast");
        requestBody.put("occasion", "birthday gift");

        String suggestions = "For a tech enthusiast's birthday, I suggest the Gaming Mouse or Wireless Headphones.";
        when(ragService.getPersonalizedSuggestions(anyString(), anyString())).thenReturn(suggestions);

        // Act & Assert
        mockMvc.perform(post("/api/queries/personalized-suggestions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions", is(suggestions)));

        verify(ragService).getPersonalizedSuggestions(anyString(), anyString());
    }

    @Test
    void healthCheck_ShouldReturnStatus() throws Exception {
        // Arrange
        when(ragService.isHealthy()).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/queries/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.healthy", is(true)));

        verify(ragService).isHealthy();
    }

    @Test
    void healthCheck_ShouldReturnUnhealthyWhenServiceDown() throws Exception {
        // Arrange
        when(ragService.isHealthy()).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/queries/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DOWN")))
                .andExpect(jsonPath("$.healthy", is(false)));

        verify(ragService).isHealthy();
    }
}
