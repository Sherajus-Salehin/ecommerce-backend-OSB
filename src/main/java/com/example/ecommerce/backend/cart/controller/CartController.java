package com.example.ecommerce.backend.cart.controller;

import com.example.ecommerce.backend.cart.dto.request.CartItemAddRequest;
import com.example.ecommerce.backend.cart.dto.response.CartItemResponse;
import com.example.ecommerce.backend.cart.dto.response.CartResponse;
import com.example.ecommerce.backend.cart.service.CartService;
import com.example.ecommerce.backend.common.constants.ApiEndpoints;
import com.example.ecommerce.backend.common.dto.response.ApiResponse;
import com.example.ecommerce.backend.product.dto.response.ProductSuggestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing the current user's shopping cart.
 *
 * <p>Exposes versioned cart endpoints under
 * {@link ApiEndpoints.Cart#BASE_CART} and wraps successful responses with the
 * common {@link ApiResponse} structure used by the API.</p>
 *
 * <p>The user identifier is temporarily hard-coded until authentication and a
 * user entity are introduced.</p>
 *
 * @author Pial Kanti Samadder
 */
@RestController
@RequestMapping(ApiEndpoints.Cart.BASE_CART)
@RequiredArgsConstructor
@Tag(
        name = "Cart",
        description = "Operations for managing the current user's shopping cart"
)
public class CartController {
    private static final Long CURRENT_USER_ID = 1L;

    private final CartService cartService;

    /**
     * Adds a product item to the current user's cart.
     *
     * @param request validated cart item add payload
     * @return response containing the updated cart
     * @throws jakarta.persistence.EntityNotFoundException when product or inventory does not exist
     * @throws com.example.ecommerce.backend.common.exception.ResourceConflictException when the product is inactive or stock is insufficient
     */
    @Operation(
            summary = "Add item to cart",
            description = "Adds an active product to the current user's cart after checking available inventory.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Cart item payload containing product identifier and quantity.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CartItemAddRequest.class),
                            examples = @ExampleObject(
                                    name = "Add item to cart",
                                    value = """
                                            {
                                              "productId": 1,
                                              "quantity": 2
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Item added to cart successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CartResponse.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Invalid cart item payload",
                            content = @Content
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "No product or inventory exists for the supplied product identifier",
                            content = @Content
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "409",
                            description = "Product is inactive or available stock is insufficient",
                            content = @Content
                    )
            }
    )
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Valid @RequestBody CartItemAddRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cartService.addItem(CURRENT_USER_ID, request)));
    }

    /**
     * Retrieves the current user's cart details.
     *
     * @return response containing the current cart, or an empty cart when none exists
     */
    @Operation(
            summary = "Get current cart",
            description = "Retrieves the current user's cart details including items, total quantity, and subtotal.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Cart retrieved successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CartResponse.class)
                            )
                    )
            }
    )
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        return ResponseEntity.ok(ApiResponse.success(cartService.getCart(CURRENT_USER_ID)));
    }
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<ProductSuggestionResponse>>> getSuggestions() {
        //ProductSuggestionResponse [] suggestions=new ProductSuggestionResponse[3];

        return ResponseEntity.ok(ApiResponse.success(cartService.getSuggestion(CURRENT_USER_ID)));
    }
}
