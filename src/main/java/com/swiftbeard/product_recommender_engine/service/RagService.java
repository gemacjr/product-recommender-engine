package com.swiftbeard.product_recommender_engine.service;

import com.swiftbeard.product_recommender_engine.config.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval Augmented Generation) Service for answering customer queries
 * Uses vector store to retrieve relevant product context and generates answers
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RagService {

    private final VectorStoreService vectorStoreService;
    private final ChatModel chatModel;
    private final ApplicationProperties applicationProperties;

    /**
     * Answer a customer query using RAG pattern
     */
    public String answerQuery(String query) {
        log.debug("Answering query with RAG: {}", query);

        // Step 1: Retrieve relevant context from vector store
        int contextWindowSize = applicationProperties.getRag().getContextWindowSize();
        List<Document> relevantDocs = vectorStoreService.getRelevantContext(query, contextWindowSize);

        if (relevantDocs.isEmpty()) {
            log.info("No relevant context found for query");
            return "I couldn't find any products matching your query. Please try rephrasing or being more specific.";
        }

        // Step 2: Build context from retrieved documents
        String context = buildContext(relevantDocs);

        // Step 3: Generate answer using LLM with context
        String answer = generateAnswer(query, context);

        log.info("Successfully answered query with RAG");
        return answer;
    }

    /**
     * Get product recommendations with explanation
     */
    public String getRecommendationWithExplanation(String userQuery, String userPreferences) {
        log.debug("Getting recommendation with explanation for: {}", userQuery);

        // Retrieve relevant products
        int contextWindowSize = applicationProperties.getRag().getContextWindowSize();
        List<Document> relevantDocs = vectorStoreService.getRelevantContext(
                userQuery + " " + (userPreferences != null ? userPreferences : ""),
                contextWindowSize
        );

        if (relevantDocs.isEmpty()) {
            return "I couldn't find suitable products for your request. Please provide more details.";
        }

        String context = buildContext(relevantDocs);

        String promptTemplate = """
                You are a helpful shopping assistant. Based on the customer's query and preferences,
                recommend suitable products and explain why they're good matches.

                Customer Query: %s

                User Preferences: %s

                Available Products:
                %s

                Provide a helpful recommendation with:
                1. Product names and key features
                2. Why each product matches the customer's needs
                3. Comparison if multiple products are suggested
                4. Any additional advice

                Keep the response conversational and helpful.
                """;

        String prompt = String.format(
                promptTemplate,
                userQuery,
                userPreferences != null ? userPreferences : "Not specified",
                context
        );

        String recommendation = chatModel.call(prompt);

        log.info("Generated recommendation with explanation");
        return recommendation;
    }

    /**
     * Compare multiple products
     */
    public String compareProducts(List<Long> productIds) {
        log.debug("Comparing products: {}", productIds);

        if (productIds == null || productIds.isEmpty()) {
            return "Please provide product IDs to compare.";
        }

        // Create a query to find these specific products
        String query = "product comparison " + String.join(" ", productIds.stream()
                .map(String::valueOf)
                .collect(Collectors.toList()));

        List<Document> productDocs = vectorStoreService.getRelevantContext(query, productIds.size() * 2);

        if (productDocs.isEmpty()) {
            return "Could not find the specified products for comparison.";
        }

        String context = buildContext(productDocs);

        String promptTemplate = """
                You are a product comparison expert. Compare the following products and provide
                a detailed analysis.

                Products to Compare:
                %s

                Provide a comprehensive comparison including:
                1. Key similarities and differences
                2. Pros and cons of each product
                3. Best use cases for each
                4. Value for money analysis
                5. Your recommendation based on different user needs

                Format the comparison in a clear, easy-to-read manner.
                """;

        String prompt = String.format(promptTemplate, context);
        String comparison = chatModel.call(prompt);

        log.info("Generated product comparison");
        return comparison;
    }

    /**
     * Answer FAQ about a specific product
     */
    public String answerProductFaq(Long productId, String question) {
        log.debug("Answering FAQ for product {}: {}", productId, question);

        // Get product context
        List<Document> productDocs = vectorStoreService.getRelevantContext(
                "product " + productId + " " + question,
                2
        );

        if (productDocs.isEmpty()) {
            return "I couldn't find information about this product. Please check the product ID.";
        }

        String context = buildContext(productDocs);

        String promptTemplate = """
                You are a knowledgeable product specialist. Answer the customer's question about
                the product based on the available information.

                Product Information:
                %s

                Customer Question: %s

                Provide a clear, accurate answer. If the information is not available in the product
                details, politely say so and suggest contacting customer support for specific details.
                """;

        String prompt = String.format(promptTemplate, context, question);
        String answer = chatModel.call(prompt);

        log.info("Answered product FAQ");
        return answer;
    }

    /**
     * Get personalized shopping suggestions
     */
    public String getPersonalizedSuggestions(String userProfile, String occasion) {
        log.debug("Getting personalized suggestions for occasion: {}", occasion);

        String query = String.format("%s %s shopping suggestions", userProfile, occasion);

        List<Document> relevantDocs = vectorStoreService.getRelevantContext(
                query,
                applicationProperties.getRag().getContextWindowSize() * 2
        );

        if (relevantDocs.isEmpty()) {
            return "I couldn't find suitable products. Please try different criteria.";
        }

        String context = buildContext(relevantDocs);

        String promptTemplate = """
                You are a personal shopping advisor. Based on the user profile and occasion,
                suggest suitable products.

                User Profile: %s

                Occasion: %s

                Available Products:
                %s

                Provide personalized shopping suggestions with:
                1. Product recommendations tailored to the user and occasion
                2. Why each product is suitable
                3. Styling or usage tips if applicable
                4. Budget considerations

                Be enthusiastic and helpful!
                """;

        String prompt = String.format(promptTemplate, userProfile, occasion, context);
        String suggestions = chatModel.call(prompt);

        log.info("Generated personalized shopping suggestions");
        return suggestions;
    }

    /**
     * Build context string from documents
     */
    private String buildContext(List<Document> documents) {
        StringBuilder contextBuilder = new StringBuilder();

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            contextBuilder.append(String.format("Product %d:\n", i + 1));
            contextBuilder.append(doc.getContent());
            contextBuilder.append("\n");

            // Add metadata if available
            if (doc.getMetadata() != null && !doc.getMetadata().isEmpty()) {
                contextBuilder.append("Additional Info: ");
                contextBuilder.append("Price: $").append(doc.getMetadata().get("price"));
                contextBuilder.append(", Rating: ").append(doc.getMetadata().get("rating"));
                contextBuilder.append(" stars");
                contextBuilder.append("\n");
            }
            contextBuilder.append("\n");
        }

        return contextBuilder.toString();
    }

    /**
     * Generate answer using LLM with context
     */
    private String generateAnswer(String query, String context) {
        String systemPrompt = """
                You are a helpful e-commerce assistant. Answer customer questions about products
                based on the provided context. Be accurate, helpful, and conversational.

                If the context doesn't contain enough information to answer the question,
                politely say so and suggest how the customer can get more information.

                Always base your answers on the provided product information.
                """;

        String userPrompt = String.format("""
                Context (Product Information):
                %s

                Customer Question: %s

                Please provide a helpful answer based on the product information above.
                """, context, query);

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(userPrompt));

        Prompt prompt = new Prompt(messages);
        return chatModel.call(prompt).getResult().getOutput().getContent();
    }

    /**
     * Check if RAG service is healthy
     */
    public boolean isHealthy() {
        try {
            vectorStoreService.isHealthy();
            return true;
        } catch (Exception e) {
            log.error("RAG service health check failed", e);
            return false;
        }
    }
}
