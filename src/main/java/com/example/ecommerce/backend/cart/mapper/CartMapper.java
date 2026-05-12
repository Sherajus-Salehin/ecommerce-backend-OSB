package com.example.ecommerce.backend.cart.mapper;

import com.example.ecommerce.backend.cart.dto.response.CartItemResponse;
import com.example.ecommerce.backend.cart.dto.response.CartResponse;
import com.example.ecommerce.backend.cart.entity.Cart;
import com.example.ecommerce.backend.cart.entity.CartItem;
import com.example.ecommerce.backend.product.dto.response.ProductSuggestionResponse;
import com.example.ecommerce.backend.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for converting cart entities to API responses.
 *
 * @author Pial Kanti Samadder
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CartMapper {
    /**
     * Converts a cart entity to its API response representation.
     *
     * @param cart cart entity
     * @return cart response DTO
     */
    default CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems()
                .stream()
                .map(this::toItemResponse)
                .toList();

        return new CartResponse(
                cart.getId(),
                cart.getUserId(),
                items,
                calculateTotalQuantity(cart),
                calculateSubtotal(cart),
                cart.getCreatedAt(),
                cart.getModifiedAt(),
                cart.getCreatedBy(),
                cart.getModifiedBy()
        );
    }

    /**
     * Builds an empty cart response for users without a persisted cart yet.
     *
     * @param userId owner user identifier
     * @return empty cart response
     */
    default CartResponse toEmptyResponse(Long userId) {
        return new CartResponse(null, userId, List.of(), 0, 0.0, null, null, null, null);
    }

    private CartItemResponse toItemResponse(CartItem item) {
        return new CartItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getSku(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getQuantity() * item.getUnitPrice()
        );
    }

    private Integer calculateTotalQuantity(Cart cart) {
        return cart.getItems()
                .stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    private Double calculateSubtotal(Cart cart) {
        return cart.getItems()
                .stream()
                .mapToDouble(item -> item.getQuantity() * item.getUnitPrice())
                .sum();
    }

    default ProductSuggestionResponse toProductSuggestionResponse(Product product) {
        return new ProductSuggestionResponse(
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getImageUrl());
    }
}
