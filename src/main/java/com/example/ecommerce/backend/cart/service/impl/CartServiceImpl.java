package com.example.ecommerce.backend.cart.service.impl;

import com.example.ecommerce.backend.cart.dto.request.CartItemAddRequest;
import com.example.ecommerce.backend.cart.dto.response.CartItemResponse;
import com.example.ecommerce.backend.cart.dto.response.CartResponse;
import com.example.ecommerce.backend.cart.entity.Cart;
import com.example.ecommerce.backend.cart.entity.CartItem;
import com.example.ecommerce.backend.cart.mapper.CartMapper;
import com.example.ecommerce.backend.cart.repository.CartRepository;
import com.example.ecommerce.backend.cart.service.CartService;
import com.example.ecommerce.backend.common.exception.ResourceConflictException;
import com.example.ecommerce.backend.inventory.entity.Inventory;
import com.example.ecommerce.backend.inventory.repository.InventoryRepository;
import com.example.ecommerce.backend.product.dto.response.ProductSuggestionResponse;
import com.example.ecommerce.backend.product.entity.Category;
import com.example.ecommerce.backend.product.entity.Product;
import com.example.ecommerce.backend.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Implementation of {@link CartService} for managing user shopping carts.
 *
 * <p>Handles cart lookup, first-cart creation, product validation, inventory
 * availability checks, item quantity merging, persistence, and response
 * mapping. Inventory is checked before cart mutation, but it is not reserved by
 * cart operations.</p>
 *
 * @author Pial Kanti Samadder
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CartMapper cartMapper;

    @Override
    @Transactional
    public CartResponse addItem(Long userId, CartItemAddRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + request.productId()));

        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new ResourceConflictException("Inactive product cannot be added to cart: " + request.productId());
        }

        Inventory inventory = inventoryRepository.findByProductId(request.productId())
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found for product: " + request.productId()));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createCart(userId));

        Optional<CartItem> existingItem = findItemByProductId(cart, request.productId());
        int requestedCartQuantity = request.quantity() + existingItem
                .map(CartItem::getQuantity)
                .orElse(0);

        validateAvailableQuantity(inventory, requestedCartQuantity);

        if (existingItem.isPresent()) {
            CartItem cartItem = existingItem.get();
            cartItem.setQuantity(requestedCartQuantity);
        } else {
            cart.getItems().add(CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.quantity())
                    .unitPrice(product.getPrice())
                    .build());
        }

        return cartMapper.toResponse(cartRepository.saveAndFlush(cart));
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(cartMapper::toResponse)
                .orElseGet(() -> cartMapper.toEmptyResponse(userId));
    }

    @Override
    @Transactional
    public void clearCart(Long userId, Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found: " + cartId));

        if (!cart.getUserId().equals(userId)) {
            throw new ResourceConflictException("Cart does not belong to current user: " + cartId);
        }

        cart.getItems().clear();
        cartRepository.saveAndFlush(cart);
    }

    @Override
    public List<ProductSuggestionResponse> getSuggestion(Long currentUserId) {
        List<ProductSuggestionResponse> suggestions = new ArrayList<>();
        List<Long> pids = getCart(currentUserId).items().stream().map(CartItemResponse::productId).toList();
        if (pids.isEmpty()) {
            return Collections.emptyList();
        }else {
            int suggestionCount = 0;
            LevenshteinDistance ld = new LevenshteinDistance();
            Set<String> checked=new HashSet<>();
            for(Long pid : pids) {
                Product p= productRepository.findById(pid).orElseThrow(() -> new EntityNotFoundException("Product not found: " + pid));
                Category c=p.getCategory();
                if(!checked.contains(c.getName()) && suggestionCount<3) {
                    checked.add(c.getName());
                    suggestionCount++;
                    Double tkl=Math.max(0,p.getPrice()-100);
                    Double tkh=p.getPrice()+100;
                    List<Product> narrowedDown=productRepository.findByCategoryAndPrice(c,tkl,tkh,pid);
                    String target=p.getName();
                    Product bestMatch = null;
                    int bestScore=Integer.MAX_VALUE;
                    for(Product test: narrowedDown) {
                        int match=ld.apply(target,test.getName());
                        if(match<bestScore) {
                            bestScore=match;
                            bestMatch=test;
                        }
                    }
                    if(bestMatch!=null) suggestions.add(cartMapper.toProductSuggestionResponse(bestMatch));
                }
            }
        return  suggestions;
        }
    }

    private Cart createCart(Long userId) {
        return Cart.builder()
                .userId(userId)
                .createdBy(userId)
                .modifiedBy(userId)
                .build();
    }

    private Optional<CartItem> findItemByProductId(Cart cart, Long productId) {
        return cart.getItems()
                .stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();
    }

    private void validateAvailableQuantity(Inventory inventory, int requestedCartQuantity) {
        int availableQuantity = inventory.getTotalQuantity() - inventory.getReservedQuantity();
        if (requestedCartQuantity > availableQuantity) {
            throw new ResourceConflictException("Insufficient available inventory for product: "
                                                + inventory.getProduct().getId());
        }
    }

}
