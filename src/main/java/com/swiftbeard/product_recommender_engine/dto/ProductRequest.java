package com.swiftbeard.product_recommender_engine.dto;

import com.swiftbeard.product_recommender_engine.model.Category;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for creating/updating products
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 200, message = "Product name must be between 3 and 200 characters")
    private String name;

    @NotBlank(message = "Product description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    @NotNull(message = "Category is required")
    private Category category;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    private String brand;

    private String sku;

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Builder.Default
    private List<String> features = new ArrayList<>();

    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Builder.Default
    private Integer stockQuantity = 0;

    @DecimalMin(value = "0.0", message = "Rating must be at least 0")
    @DecimalMax(value = "5.0", message = "Rating cannot exceed 5")
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @Min(value = 0, message = "Review count cannot be negative")
    @Builder.Default
    private Integer reviewCount = 0;

    private String imageUrl;
}
