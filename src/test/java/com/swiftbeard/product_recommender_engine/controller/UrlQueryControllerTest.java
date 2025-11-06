package com.swiftbeard.product_recommender_engine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftbeard.product_recommender_engine.dto.UrlQueryRequest;
import com.swiftbeard.product_recommender_engine.service.RagService;
import com.swiftbeard.product_recommender_engine.service.WebScrapingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for URL query endpoint in QueryController
 */
@WebMvcTest(QueryController.class)
class UrlQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RagService ragService;

    @MockBean
    private WebScrapingService webScrapingService;

    @Test
    void testAskUrl_success() throws Exception {
        // Given
        UrlQueryRequest request = UrlQueryRequest.builder()
                .url("https://example.com/shop")
                .query("is there a shirt in size XXL under 10 dollars")
                .build();

        String expectedAnswer = "Yes, we found a blue cotton shirt in size XXL for $8.99";
        String contentPreview = "Page Title: Example Shop\n\nProducts Found\n\nProduct 1:\nName: Blue Cotton Shirt...";

        when(ragService.answerUrlQuery(anyString(), anyString())).thenReturn(expectedAnswer);
        when(webScrapingService.scrapeWebsite(anyString())).thenReturn(contentPreview);
        when(webScrapingService.getContentPreview(anyString())).thenReturn(contentPreview.substring(0, 100));

        // When/Then
        mockMvc.perform(post("/api/queries/ask-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(request.getUrl()))
                .andExpect(jsonPath("$.query").value(request.getQuery()))
                .andExpect(jsonPath("$.answer").value(expectedAnswer))
                .andExpect(jsonPath("$.scrapedContentPreview").exists());
    }

    @Test
    void testAskUrl_withMissingUrl() throws Exception {
        // Given
        UrlQueryRequest request = UrlQueryRequest.builder()
                .query("is there a shirt in size XXL")
                .build(); // Missing URL

        // When/Then
        mockMvc.perform(post("/api/queries/ask-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAskUrl_withMissingQuery() throws Exception {
        // Given
        UrlQueryRequest request = UrlQueryRequest.builder()
                .url("https://example.com/shop")
                .build(); // Missing query

        // When/Then
        mockMvc.perform(post("/api/queries/ask-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAskUrl_withServiceError() throws Exception {
        // Given
        UrlQueryRequest request = UrlQueryRequest.builder()
                .url("https://example.com/shop")
                .query("is there a shirt in size XXL")
                .build();

        when(ragService.answerUrlQuery(anyString(), anyString()))
                .thenThrow(new RuntimeException("Failed to scrape website"));

        // When/Then
        mockMvc.perform(post("/api/queries/ask-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testAskUrl_withEmptyStrings() throws Exception {
        // Given
        UrlQueryRequest request = UrlQueryRequest.builder()
                .url("")
                .query("")
                .build();

        // When/Then
        mockMvc.perform(post("/api/queries/ask-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
