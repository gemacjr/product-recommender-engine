package com.swiftbeard.product_recommender_engine.service;

import com.swiftbeard.product_recommender_engine.config.ApplicationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RagService Unit Tests")
class RagServiceTest {

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private ChatModel chatModel;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private ApplicationProperties.RagConfig ragConfig;

    @Mock
    private WebScrapingService webScrapingService;

    @InjectMocks
    private RagService ragService;

    private List<Document> testDocuments;
    private ChatResponse mockChatResponse;

    @BeforeEach
    void setUp() {
        when(applicationProperties.getRag()).thenReturn(ragConfig);
        when(ragConfig.getContextWindowSize()).thenReturn(5);

        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("productId", "1");
        metadata1.put("name", "Wireless Headphones");
        metadata1.put("price", "99.99");
        metadata1.put("rating", "4.5");

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("productId", "2");
        metadata2.put("name", "Gaming Mouse");
        metadata2.put("price", "79.99");
        metadata2.put("rating", "4.7");

        testDocuments = Arrays.asList(
                new Document("1", "Wireless Headphones with noise cancellation", metadata1),
                new Document("2", "Gaming Mouse with RGB lighting", metadata2)
        );

        AssistantMessage assistantMessage = mock(AssistantMessage.class);
        when(assistantMessage.getText()).thenReturn("This is a test AI response");
        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(assistantMessage);
        mockChatResponse = mock(ChatResponse.class);
        when(mockChatResponse.getResult()).thenReturn(generation);

        when(chatModel.call(any(Prompt.class))).thenReturn(mockChatResponse);
        when(chatModel.call(anyString())).thenReturn("This is a test AI response");
    }

    @Test
    @DisplayName("Should return AI-generated answer when context exists")
    void answerQuery_ShouldReturnAIGeneratedAnswer_WhenContextExists() {
        when(vectorStoreService.getRelevantContext(anyString(), anyInt())).thenReturn(testDocuments);

        String result = ragService.answerQuery("What are the best wireless headphones?");

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        verify(vectorStoreService).getRelevantContext(anyString(), eq(5));
        verify(chatModel).call(any(Prompt.class));
    }

    @Test
    @DisplayName("Should return default message when no context found")
    void answerQuery_ShouldReturnDefaultMessage_WhenNoContextFound() {
        when(vectorStoreService.getRelevantContext(anyString(), anyInt())).thenReturn(Collections.emptyList());

        String result = ragService.answerQuery("What products do you have?");

        assertThat(result).isNotNull();
        assertThat(result).contains("couldn't find any products");
        verify(vectorStoreService).getRelevantContext(anyString(), eq(5));
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    @DisplayName("Should return recommendation with explanation when products found")
    void getRecommendationWithExplanation_ShouldReturnRecommendation_WhenProductsFound() {
        when(vectorStoreService.getRelevantContext(anyString(), anyInt())).thenReturn(testDocuments);

        String result = ragService.getRecommendationWithExplanation(
                "I need headphones for work",
                "noise cancellation, comfortable"
        );

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        verify(vectorStoreService).getRelevantContext(anyString(), eq(5));
        verify(chatModel).call(anyString());
    }

    @Test
    @DisplayName("Should handle null preferences in recommendation")
    void getRecommendationWithExplanation_ShouldHandleNullPreferences() {
        when(vectorStoreService.getRelevantContext(anyString(), anyInt())).thenReturn(testDocuments);

        String result = ragService.getRecommendationWithExplanation("I need headphones", null);

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        verify(vectorStoreService).getRelevantContext(anyString(), eq(5));
        verify(chatModel).call(anyString());
    }

    @Test
    @DisplayName("Should return default message when no products found for recommendation")
    void getRecommendationWithExplanation_ShouldReturnDefaultMessage_WhenNoProductsFound() {
        when(vectorStoreService.getRelevantContext(anyString(), anyInt())).thenReturn(Collections.emptyList());

        String result = ragService.getRecommendationWithExplanation("obscure product query", "preferences");

        assertThat(result).contains("couldn't find suitable products");
        verify(vectorStoreService).getRelevantContext(anyString(), anyInt());
        verify(chatModel, never()).call(anyString());
    }

    @Test
    @DisplayName("Should return comparison when products found")
    void compareProducts_ShouldReturnComparison_WhenProductsFound() {
        List<Long> productIds = Arrays.asList(1L, 2L);
        when(vectorStoreService.getRelevantContext(anyString(), anyInt())).thenReturn(testDocuments);

        String result = ragService.compareProducts(productIds);

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        verify(vectorStoreService).getRelevantContext(anyString(), eq(4));
        verify(chatModel).call(anyString());
    }

    @Test
    @DisplayName("Should return message when product IDs are null")
    void compareProducts_ShouldReturnMessage_WhenProductIdsNull() {
        String result = ragService.compareProducts(null);

        assertThat(result).contains("provide product IDs to compare");
        verify(vectorStoreService, never()).getRelevantContext(anyString(), anyInt());
        verify(chatModel, never()).call(anyString());
    }

    @Test
    @DisplayName("Should return message when product IDs are empty")
    void compareProducts_ShouldReturnMessage_WhenProductIdsEmpty() {
        String result = ragService.compareProducts(Collections.emptyList());

        assertThat(result).contains("provide product IDs to compare");
        verify(vectorStoreService, never()).getRelevantContext(anyString(), anyInt());
    }

    @Test
    @DisplayName("Should return message when products not found for comparison")
    void compareProducts_ShouldReturnMessage_WhenProductsNotFound() {
        List<Long> productIds = Arrays.asList(1L, 2L);
        when(vectorStoreService.getRelevantContext(anyString(), anyInt())).thenReturn(Collections.emptyList());

        String result = ragService.compareProducts(productIds);

        assertThat(result).contains("Could not find the specified products");
        verify(vectorStoreService).getRelevantContext(anyString(), anyInt());
        verify(chatModel, never()).call(anyString());
    }

    @Test
    @DisplayName("Should return answer when product FAQ found")
    void answerProductFaq_ShouldReturnAnswer_WhenProductFound() {
        when(vectorStoreService.getRelevantContext(anyString(), eq(2))).thenReturn(testDocuments);

        String result = ragService.answerProductFaq(1L, "Does it have noise cancellation?");

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        verify(vectorStoreService).getRelevantContext(anyString(), eq(2));
        verify(chatModel).call(anyString());
    }

    @Test
    @DisplayName("Should return message when product not found for FAQ")
    void answerProductFaq_ShouldReturnMessage_WhenProductNotFound() {
        when(vectorStoreService.getRelevantContext(anyString(), anyInt())).thenReturn(Collections.emptyList());

        String result = ragService.answerProductFaq(999L, "What is this?");

        assertThat(result).contains("couldn't find information about this product");
        verify(vectorStoreService).getRelevantContext(anyString(), eq(2));
        verify(chatModel, never()).call(anyString());
    }

    @Test
    @DisplayName("Should return personalized suggestions when products found")
    void getPersonalizedSuggestions_ShouldReturnSuggestions_WhenProductsFound() {
        when(vectorStoreService.getRelevantContext(anyString(), anyInt())).thenReturn(testDocuments);

        String result = ragService.getPersonalizedSuggestions(
                "tech enthusiast, loves gaming",
                "birthday gift"
        );

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        verify(vectorStoreService).getRelevantContext(anyString(), eq(10));
        verify(chatModel).call(anyString());
    }

    @Test
    @DisplayName("Should return message when no products found for suggestions")
    void getPersonalizedSuggestions_ShouldReturnMessage_WhenNoProductsFound() {
        when(vectorStoreService.getRelevantContext(anyString(), anyInt())).thenReturn(Collections.emptyList());

        String result = ragService.getPersonalizedSuggestions("user profile", "occasion");

        assertThat(result).contains("couldn't find suitable products");
        verify(vectorStoreService).getRelevantContext(anyString(), eq(10));
        verify(chatModel, never()).call(anyString());
    }

    @Test
    @DisplayName("Should return true when vector store is healthy")
    void isHealthy_ShouldReturnTrue_WhenVectorStoreIsHealthy() {
        when(vectorStoreService.isHealthy()).thenReturn(true);

        boolean result = ragService.isHealthy();

        assertThat(result).isTrue();
        verify(vectorStoreService).isHealthy();
    }

    @Test
    @DisplayName("Should return false when vector store throws exception")
    void isHealthy_ShouldReturnFalse_WhenVectorStoreThrowsException() {
        doThrow(new RuntimeException("Vector store error")).when(vectorStoreService).isHealthy();

        boolean result = ragService.isHealthy();

        assertThat(result).isFalse();
        verify(vectorStoreService).isHealthy();
    }

    @Test
    @DisplayName("Should answer URL query successfully")
    void answerUrlQuery_ShouldReturnAnswer_WhenScrapingSucceeds() throws Exception {
        String scrapedContent = "Page Title: Example Shop\n\nProducts Found\n\nProduct 1:\nName: Blue Cotton Shirt";
        when(webScrapingService.scrapeWebsite("https://example.com/shop")).thenReturn(scrapedContent);
        when(chatModel.call(any(Prompt.class))).thenReturn(mockChatResponse);

        String result = ragService.answerUrlQuery("https://example.com/shop", "is there a shirt in size XXL");

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        verify(webScrapingService).scrapeWebsite("https://example.com/shop");
        verify(chatModel).call(any(Prompt.class));
    }

    @Test
    @DisplayName("Should throw exception when scraping fails")
    void answerUrlQuery_ShouldThrowException_WhenScrapingFails() throws Exception {
        when(webScrapingService.scrapeWebsite(anyString())).thenThrow(new IOException("Connection failed"));

        assertThatThrownBy(() -> ragService.answerUrlQuery("https://example.com/shop", "query"))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Failed to fetch content");
    }

    @Test
    @DisplayName("Should return message when scraped content is empty")
    void answerUrlQuery_ShouldReturnMessage_WhenContentIsEmpty() throws Exception {
        when(webScrapingService.scrapeWebsite(anyString())).thenReturn("");

        String result = ragService.answerUrlQuery("https://example.com/shop", "query");

        assertThat(result).contains("couldn't retrieve any content");
        verify(webScrapingService).scrapeWebsite(anyString());
        verify(chatModel, never()).call(any(Prompt.class));
    }
}

