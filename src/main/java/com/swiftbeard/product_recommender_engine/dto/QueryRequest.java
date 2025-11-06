package com.swiftbeard.product_recommender_engine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for customer queries
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequest {

    @NotBlank(message = "Query is required")
    @Size(min = 3, max = 500, message = "Query must be between 3 and 500 characters")
    private String query;

    private String userPreferences;

    private String context;
}
