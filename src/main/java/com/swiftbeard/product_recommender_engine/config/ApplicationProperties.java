package com.swiftbeard.product_recommender_engine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Application-wide configuration properties
 */
@Configuration
@ConfigurationProperties(prefix = "application")
@Data
public class ApplicationProperties {

    private ProductConfig product = new ProductConfig();
    private RecommendationConfig recommendation = new RecommendationConfig();
    private RagConfig rag = new RagConfig();

    @Data
    public static class ProductConfig {
        private int defaultLimit = 10;
        private int maxLimit = 100;
    }

    @Data
    public static class RecommendationConfig {
        private double similarityThreshold = 0.7;
        private int maxResults = 20;
    }

    @Data
    public static class RagConfig {
        private int contextWindowSize = 5;
        private double temperature = 0.3;
    }
}
