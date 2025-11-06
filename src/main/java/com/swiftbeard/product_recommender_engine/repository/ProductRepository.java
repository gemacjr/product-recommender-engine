package com.swiftbeard.product_recommender_engine.repository;

import com.swiftbeard.product_recommender_engine.model.Category;
import com.swiftbeard.product_recommender_engine.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Product entity with custom query methods
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find all active products
     */
    Page<Product> findByActiveTrue(Pageable pageable);

    /**
     * Find products by category
     */
    Page<Product> findByCategoryAndActiveTrue(Category category, Pageable pageable);

    /**
     * Find products by name containing (case-insensitive)
     */
    Page<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);

    /**
     * Find products by brand
     */
    Page<Product> findByBrandIgnoreCaseAndActiveTrue(String brand, Pageable pageable);

    /**
     * Find products within price range
     */
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice AND p.active = true")
    Page<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice,
                                   @Param("maxPrice") BigDecimal maxPrice,
                                   Pageable pageable);

    /**
     * Find products by minimum rating
     */
    @Query("SELECT p FROM Product p WHERE p.rating >= :minRating AND p.active = true ORDER BY p.rating DESC")
    Page<Product> findByMinimumRating(@Param("minRating") BigDecimal minRating, Pageable pageable);

    /**
     * Find products by SKU
     */
    Optional<Product> findBySkuAndActiveTrue(String sku);

    /**
     * Find products by tag
     */
    @Query("SELECT DISTINCT p FROM Product p JOIN p.tags t WHERE t = :tag AND p.active = true")
    Page<Product> findByTag(@Param("tag") String tag, Pageable pageable);

    /**
     * Search products by keyword in name, description, or features
     */
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN p.features f WHERE " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(f) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND p.active = true")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Find top-rated products
     */
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.reviewCount > 0 ORDER BY p.rating DESC, p.reviewCount DESC")
    Page<Product> findTopRated(Pageable pageable);

    /**
     * Find products by IDs
     */
    List<Product> findByIdInAndActiveTrue(List<Long> ids);

    /**
     * Count products by category
     */
    long countByCategoryAndActiveTrue(Category category);

    /**
     * Find all distinct brands
     */
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL AND p.active = true ORDER BY p.brand")
    List<String> findDistinctBrands();

    /**
     * Find all distinct tags
     */
    @Query("SELECT DISTINCT t FROM Product p JOIN p.tags t WHERE p.active = true ORDER BY t")
    List<String> findDistinctTags();
}
