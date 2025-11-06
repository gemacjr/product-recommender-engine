package com.swiftbeard.product_recommender_engine.model;

/**
 * Product categories for classification and filtering
 */
public enum Category {
    ELECTRONICS("Electronics", "Electronic devices and gadgets"),
    CLOTHING("Clothing", "Apparel and fashion items"),
    HOME_GARDEN("Home & Garden", "Home improvement and garden supplies"),
    SPORTS_OUTDOORS("Sports & Outdoors", "Sports equipment and outdoor gear"),
    BOOKS("Books", "Books and reading materials"),
    TOYS_GAMES("Toys & Games", "Toys and gaming products"),
    BEAUTY_PERSONAL_CARE("Beauty & Personal Care", "Beauty and personal care products"),
    FOOD_BEVERAGES("Food & Beverages", "Food and beverage items"),
    AUTOMOTIVE("Automotive", "Car parts and accessories"),
    HEALTH_WELLNESS("Health & Wellness", "Health and wellness products");

    private final String displayName;
    private final String description;

    Category(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
