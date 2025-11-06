package com.swiftbeard.product_recommender_engine.service;

import com.swiftbeard.product_recommender_engine.config.ApplicationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private ChatModel chatModel;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private ApplicationProperties.RagConfig ragConfig;

    @InjectMocks
    private RagService ragService;

    private List<Document> testDocuments;
    private ChatResponse mockChatResponse;
    private Generation mockGeneration;

    @BeforeEach
    void setUp() {
        // Setup application properties mock
        when(applicationProperties.getRag()).thenReturn(ragConfig);
        when(ragConfig.getContextWindowSize()).thenReturn(5);
        when(ragConfig.getTemperature()).thenReturn(0.3);

        // Setup test documents
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

        // Setup mock ChatModel response
        mockGeneration = mock(Generation.class);
        AssistantMessage mockOutput = mock(AssistantMessage.class);
        when(mockOutput.getText()).thenReturn("This is a test AI response");
        when(mockGeneration.getOutput()).thenReturn(mockOutput);

        mockChatResponse = mock(ChatResponse.class);
        when(mockChatResponse.getResult()).thenReturn(mockGeneration);

        when(chatModel.call(any(Prompt.class))).thenReturn(mockChatResponse);
        when(chatModel.call(anyString())).thenReturn("This is a test AI response");
    }

    @Test
    void answerQuery_ShouldReturnAIGeneratedAnswer_WhenContextExists() {
        // Arrange
        when(vectorStoreService.getRelevantContext(anyString(), anyInt())).thenReturn(testDocuments);

        // Act
        String result = ragService.answerQuery("What are the best wireless headphones?");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        verify(vectorStoreService).getRelevantContext(anyString(), eq(5));
        verify(chatModel).call(any(Prompt.class));
    }

    @Test
    void answerQuery_ShouldReturnDefaultMessage_WhenNoContextFound() {
        // Arrange
        when(vectorStoreService.getRelevantContext(anyString(), anyInt())).thenReturn(Collections.emptyList());

        // Act
        String result = ragService.answerQuery("What products do you have?");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).contains("couldn't find any products");
        verify(vectorStoreService).getRelevantContext(anyString(), eq(5));
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void getRecommendationWithExplanation_ShouldReturnRecommendation_WhenProductsFound() {
        // Arrange
        when(vectorStoreService.getRelevantContext(anyString(), anyInt())).thenReturn(testDocuments);

        // Act
        String result = ragService.getRecommendationWithExplanation(
                "I need headphones for work",
                "noise cancellation, comfortable"
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        verify(vectorStoreService).getRelevantContext(anyString(), eq(10)); // contextWindowSize * 2
        verify(chatModel).call(anyString());
    }

    @Test
    void getRecommendationWithExplanation_ShouldHandleNullPreferences() {
        // Arrange
        when(vectorStoreService.getRelevantContext(anyString(), anyInt())).thenReturn(testDocuments);

        // Act
        String result = ragService.getRecommendationWithExplanation("I need headphones", null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        verify(vectorStoreService).getRelevantContext(anyString(), eq(10));
        verify(chatModel).call(anyString());
    }

    @Test
    void getRecommendationWithExplanation_ShouldReturnDefaultMessage_WhenNoProductsFound() {
        // Arrange
        when(vectorStoreService.getRelevantContext(anyString(), anyInt())).thenReturn(Collections.emptyList());

        // Act
        String result = ragService.getRecommendationWithExplanation("obscure product query", "preferences");

        // Assert
        assertThat(result).contains("couldn't find suitable products");
        verify(vectorStoreService).getRelevantContext(anyString(), anyInt());
        verify(chatModel, never()).call(anyString());
    }

    @Test
    void compareProducts_ShouldReturnComparison_WhenProductsFound() {
        // Arrange
        List<Long> productIds = Arrays.asList(1L, 2L);
        when(vectorStoreService.getRelevantContext(anyString(), anyInt())).thenReturn(testDocuments);

        // Act
        String result = ragService.compareProducts(productIds);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        verify(vectorStoreService).getRelevantContext(anyString(), eq(4)); // productIds.size() * 2
        verify(chatModel).call(anyString());
    }

    @Test
    void compareProducts_ShouldReturnMessage_WhenProductIdsNull() {
        // Act
        String result = ragService.compareProducts(null);

        // Assert
        assertThat(result).contains("provide product IDs to compare");
        verify(vectorStoreService, never()).getRelevantContext(anyString(), anyInt());
        verify(chatModel, never()).call(anyString());
    }

    @Test
    void compareProducts_ShouldReturnMessage_WhenProductIdsEmpty() {
        // Act
        String result = ragService.compareProducts(Collections.emptyList());

        // Assert
        assertThat(result).contains("provide product IDs to compare");
        verify(vectorStoreService, never()).getRelevantContext(anyString(), anyInt());
    }

    @Test
    void compareProducts_ShouldReturnMessage_WhenProductsNotFound() {
        // Arrange
        List<Long> productIds = Arrays.asList(1L, 2L);
        when(vectorStoreService.getRelevantContext(anyString(), anyInt())).thenReturn(Collections.emptyList());

        // Act
        String result = ragService.compareProducts(productIds);

        // Assert
        assertThat(result).contains("Could not find the specified products");
        verify(vectorStoreService).getRelevantContext(anyString(), anyInt());
        verify(chatModel, never()).call(anyString());
    }

    @Test
    void answerProductFaq_ShouldReturnAnswer_WhenProductFound() {
        // Arrange
        when(vectorStoreService.getRelevantContext(anyString(), eq(2))).thenReturn(testDocuments);

        // Act
        String result = ragService.answerProductFaq(1L, "Does it have noise cancellation?");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        verify(vectorStoreService).getRelevantContext(anyString(), eq(2));
        verify(chatModel).call(anyString());
    }

    @Test
    void answerProductFaq_ShouldReturnMessage_WhenProductNotFound() {
        // Arrange
        when(vectorStoreService.getRelevantContext(anyString(), anyInt())).thenReturn(Collections.emptyList());

        // Act
        String result = ragService.answerProductFaq(999L, "What is this?");

        // Assert
        assertThat(result).contains("couldn't find information about this product");
        verify(vectorStoreService).getRelevantContext(anyString(), eq(2));
        verify(chatModel, never()).call(anyString());
    }

    @Test
    void getPersonalizedSuggestions_ShouldReturnSuggestions_WhenProductsFound() {
        // Arrange
        when(vectorStoreService.getRelevantContext(anyString(), anyInt())).thenReturn(testDocuments);

        // Act
        String result = ragService.getPersonalizedSuggestions(
                "tech enthusiast, loves gaming",
                "birthday gift"
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        verify(vectorStoreService).getRelevantContext(anyString(), eq(10)); // contextWindowSize * 2
        verify(chatModel).call(anyString());
    }

    @Test
    void getPersonalizedSuggestions_ShouldReturnMessage_WhenNoProductsFound() {
        // Arrange
        when(vectorStoreService.getRelevantContext(anyString(), anyInt())).thenReturn(Collections.emptyList());

        // Act
        String result = ragService.getPersonalizedSuggestions("user profile", "occasion");

        // Assert
        assertThat(result).contains("couldn't find suitable products");
        verify(vectorStoreService).getRelevantContext(anyString(), eq(10));
        verify(chatModel, never()).call(anyString());
    }

    @Test
    void isHealthy_ShouldReturnTrue_WhenVectorStoreIsHealthy() {
        // Arrange
        when(vectorStoreService.isHealthy()).thenReturn(true);

        // Act
        boolean result = ragService.isHealthy();

        // Assert
        assertThat(result).isTrue();
        verify(vectorStoreService).isHealthy();
    }

    @Test
    void isHealthy_ShouldReturnFalse_WhenVectorStoreThrowsException() {
        // Arrange
        doThrow(new RuntimeException("Vector store error")).when(vectorStoreService).isHealthy();

        // Act
        boolean result = ragService.isHealthy();

        // Assert
        assertThat(result).isFalse();
        verify(vectorStoreService).isHealthy();
    }
}
