package com.swiftbeard.product_recommender_engine.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for URL-based query requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlQueryRequest {

    @NotBlank(message = "URL is required")
    private String url;

    @NotBlank(message = "Query is required")
    private String query;
}
