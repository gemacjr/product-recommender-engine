package com.swiftbeard.product_recommender_engine.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Product entity representing items in the catalog
 */
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_category", columnList = "category"),
    @Index(name = "idx_product_name", columnList = "name"),
    @Index(name = "idx_product_price", columnList = "price")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 200, message = "Product name must be between 3 and 200 characters")
    @Column(nullable = false, length = 200)
    private String name;

    @NotBlank(message = "Product description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    @Column(nullable = false, length = 2000)
    private String description;

    @NotNull(message = "Category is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Category category;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 50)
    private String brand;

    @Column(length = 100)
    private String sku;

    @ElementCollection
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag", length = 50)
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_features", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "feature", length = 200)
    @Builder.Default
    private List<String> features = new ArrayList<>();

    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Column(nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @DecimalMin(value = "0.0", message = "Rating must be at least 0")
    @DecimalMax(value = "5.0", message = "Rating cannot exceed 5")
    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @Min(value = 0, message = "Review count cannot be negative")
    @Column(nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Generate a comprehensive text representation for embeddings
     */
    public String toEmbeddingText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Product: ").append(name).append(". ");
        sb.append("Description: ").append(description).append(". ");
        sb.append("Category: ").append(category.getDisplayName()).append(". ");

        if (brand != null && !brand.isEmpty()) {
            sb.append("Brand: ").append(brand).append(". ");
        }

        if (features != null && !features.isEmpty()) {
            sb.append("Features: ").append(String.join(", ", features)).append(". ");
        }

        if (tags != null && !tags.isEmpty()) {
            sb.append("Tags: ").append(String.join(", ", tags)).append(". ");
        }

        sb.append("Price: $").append(price).append(". ");

        if (rating != null && rating.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Rating: ").append(rating).append(" stars (").append(reviewCount).append(" reviews).");
        }

        return sb.toString();
    }
}
