package com.example.ecommerce.backend.product.dto.response;

import java.time.LocalDateTime;

public record ProductSuggestionResponse(
        String name,
        String description,
        Double price,
        String imageUrl
) {

}
