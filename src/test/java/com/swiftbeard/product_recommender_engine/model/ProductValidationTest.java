package com.swiftbeard.product_recommender_engine.model;

import com.swiftbeard.product_recommender_engine.TestDataFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProductValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validProduct_ShouldPassValidation() {
        // Arrange
        Product product = TestDataFactory.createProductWithoutId("Valid Product", Category.ELECTRONICS, new BigDecimal("99.99"));

        // Act
        Set<ConstraintViolation<Product>> violations = validator.validate(product);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void productWithBlankName_ShouldFailValidation() {
        // Arrange
        Product product = TestDataFactory.createProductWithoutId("", Category.ELECTRONICS, new BigDecimal("99.99"));

        // Act
        Set<ConstraintViolation<Product>> violations = validator.validate(product);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    void productWithShortName_ShouldFailValidation() {
        // Arrange
        Product product = TestDataFactory.createProductWithoutId("AB", Category.ELECTRONICS, new BigDecimal("99.99"));

        // Act
        Set<ConstraintViolation<Product>> violations = validator.validate(product);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("name") &&
                        v.getMessage().contains("between 3 and 200")
        );
    }

    @Test
    void productWithShortDescription_ShouldFailValidation() {
        // Arrange
        Product product = TestDataFactory.createProductWithoutId("Valid Name", Category.ELECTRONICS, new BigDecimal("99.99"));
        product.setDescription("Short");

        // Act
        Set<ConstraintViolation<Product>> violations = validator.validate(product);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }

    @Test
    void productWithNullCategory_ShouldFailValidation() {
        // Arrange
        Product product = TestDataFactory.createProductWithoutId("Valid Name", null, new BigDecimal("99.99"));

        // Act
        Set<ConstraintViolation<Product>> violations = validator.validate(product);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("category") &&
                        v.getMessage().contains("required")
        );
    }

    @Test
    void productWithNullPrice_ShouldFailValidation() {
        // Arrange
        Product product = TestDataFactory.createProductWithoutId("Valid Name", Category.ELECTRONICS, null);

        // Act
        Set<ConstraintViolation<Product>> violations = validator.validate(product);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("price"));
    }

    @Test
    void productWithNegativePrice_ShouldFailValidation() {
        // Arrange
        Product product = TestDataFactory.createProductWithoutId("Valid Name", Category.ELECTRONICS, new BigDecimal("-10.00"));

        // Act
        Set<ConstraintViolation<Product>> violations = validator.validate(product);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("price") &&
                        v.getMessage().contains("greater than 0")
        );
    }

    @Test
    void productWithZeroPrice_ShouldFailValidation() {
        // Arrange
        Product product = TestDataFactory.createProductWithoutId("Valid Name", Category.ELECTRONICS, BigDecimal.ZERO);

        // Act
        Set<ConstraintViolation<Product>> violations = validator.validate(product);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("price"));
    }

    @Test
    void productWithNegativeStockQuantity_ShouldFailValidation() {
        // Arrange
        Product product = TestDataFactory.createProductWithoutId("Valid Name", Category.ELECTRONICS, new BigDecimal("99.99"));
        product.setStockQuantity(-5);

        // Act
        Set<ConstraintViolation<Product>> violations = validator.validate(product);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("stockQuantity") &&
                        v.getMessage().contains("cannot be negative")
        );
    }

    @Test
    void productWithRatingAboveMax_ShouldFailValidation() {
        // Arrange
        Product product = TestDataFactory.createProductWithoutId("Valid Name", Category.ELECTRONICS, new BigDecimal("99.99"));
        product.setRating(new BigDecimal("6.0"));

        // Act
        Set<ConstraintViolation<Product>> violations = validator.validate(product);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("rating") &&
                        v.getMessage().contains("cannot exceed 5")
        );
    }

    @Test
    void productWithNegativeRating_ShouldFailValidation() {
        // Arrange
        Product product = TestDataFactory.createProductWithoutId("Valid Name", Category.ELECTRONICS, new BigDecimal("99.99"));
        product.setRating(new BigDecimal("-1.0"));

        // Act
        Set<ConstraintViolation<Product>> violations = validator.validate(product);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("rating") &&
                        v.getMessage().contains("at least 0")
        );
    }

    @Test
    void productWithNegativeReviewCount_ShouldFailValidation() {
        // Arrange
        Product product = TestDataFactory.createProductWithoutId("Valid Name", Category.ELECTRONICS, new BigDecimal("99.99"));
        product.setReviewCount(-1);

        // Act
        Set<ConstraintViolation<Product>> violations = validator.validate(product);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("reviewCount") &&
                        v.getMessage().contains("cannot be negative")
        );
    }

    @Test
    void productToEmbeddingText_ShouldGenerateComprehensiveText() {
        // Arrange
        Product product = TestDataFactory.createElectronicsProduct();

        // Act
        String embeddingText = product.toEmbeddingText();

        // Assert
        assertThat(embeddingText).isNotNull();
        assertThat(embeddingText).contains("Product:");
        assertThat(embeddingText).contains(product.getName());
        assertThat(embeddingText).contains("Description:");
        assertThat(embeddingText).contains(product.getDescription());
        assertThat(embeddingText).contains("Category:");
        assertThat(embeddingText).contains("Price:");
    }

    @Test
    void productToEmbeddingText_ShouldIncludeOptionalFields_WhenPresent() {
        // Arrange
        Product product = TestDataFactory.createElectronicsProduct();

        // Act
        String embeddingText = product.toEmbeddingText();

        // Assert
        assertThat(embeddingText).contains("Brand:");
        assertThat(embeddingText).contains("Features:");
        assertThat(embeddingText).contains("Tags:");
        assertThat(embeddingText).contains("Rating:");
    }
}
