package com.example.ecommerce.backend.cart.dto.response;

/**
 * Response payload representing one product line in a cart.
 *
 * <p>Product details are flattened to keep cart responses convenient for API
 * consumers while avoiding direct entity graph exposure.</p>
 *
 * @author Pial Kanti Samadder
 */
public record CartItemResponse(
        Long id,
        Long productId,
        String sku,
        String productName,
        Integer quantity,
        Double unitPrice,
        Double lineTotal
) {

}
