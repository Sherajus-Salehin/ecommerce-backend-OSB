package com.example.ecommerce.backend.product.dto.response;

import java.time.LocalDateTime;

public record ProductSuggestionResponse(
        String name,
        String description,
        Double price,
        Long categoryId,
        String imageUrl,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        Long createdBy,
        Long modifiedBy
) {

}
