package com.example.ecommerce.backend.product.repository;

import com.example.ecommerce.backend.product.entity.Category;
import com.example.ecommerce.backend.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for product catalog persistence operations.
 *
 * @author Pial Kanti Samadder
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    /**
     * Checks whether a product already exists for the supplied SKU.
     *
     * @param sku product stock keeping unit
     * @return {@code true} when the SKU is already used
     */
    boolean existsBySku(String sku);

    @Query("""
            SELECT p FROM Product p
            WHERE p.category=:category AND
            p.price BETWEEN :tkl AND :tkh
            AND p.isActive=true AND p.id != :productId
            """)
    List<Product> findByCategoryAndPrice(@Param("category") Category category,
                                         @Param("tkl") Double tkl,
                                         @Param("tkh") Double tkh,
                                         @Param("productId") Long productId);
}
