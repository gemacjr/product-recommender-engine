package com.swiftbeard.product_recommender_engine.dto;

import com.swiftbeard.product_recommender_engine.model.Category;
import com.swiftbeard.product_recommender_engine.model.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for product responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private Category category;
    private BigDecimal price;
    private String brand;
    private String sku;
    private List<String> tags;
    private List<String> features;
    private Integer stockQuantity;
    private BigDecimal rating;
    private Integer reviewCount;
    private String imageUrl;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponse fromEntity(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .price(product.getPrice())
                .brand(product.getBrand())
                .sku(product.getSku())
                .tags(product.getTags())
                .features(product.getFeatures())
                .stockQuantity(product.getStockQuantity())
                .rating(product.getRating())
                .reviewCount(product.getReviewCount())
                .imageUrl(product.getImageUrl())
                .active(product.getActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
