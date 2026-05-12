package com.example.ecommerce.backend.order.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload used to checkout a cart into a confirmed order.
 *
 * <p>The user identifier is intentionally excluded. Until authentication is
 * introduced, the controller supplies the temporary current user identifier.</p>
 *
 * @author Pial Kanti Samadder
 */
public record CreateOrderRequest(
        @NotNull(message = "Cart ID is required")
        Long cartId,
        String couponCode
) {
}
