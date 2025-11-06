package com.swiftbeard.product_recommender_engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for URL-based query responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlQueryResponse {

    private String url;
    private String query;
    private String answer;
    private String scrapedContentPreview; // Preview of scraped content for debugging
}
