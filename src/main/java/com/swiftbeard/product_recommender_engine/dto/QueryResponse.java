package com.swiftbeard.product_recommender_engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for query responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponse {

    private String query;
    private String answer;
    private Integer contextDocumentsUsed;
}
