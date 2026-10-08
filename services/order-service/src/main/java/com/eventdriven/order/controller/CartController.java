package com.eventdriven.order.controller;

import com.eventdriven.order.dto.*;
import com.eventdriven.order.exception.UnauthorizedCartAccessException;
import com.eventdriven.order.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping Cart operations supporting both Redis guest sessions and PostgreSQL authenticated users")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get current cart", description = "Fetches the active cart for either an authenticated user or a guest using X-Guest-Cart-Id.")
    public ResponseEntity<CartResponse> getCart(
            Authentication authentication,
            @Parameter(description = "Guest cart session ID (required if unauthenticated)")
            @RequestHeader(value = "X-Guest-Cart-Id", required = false) String guestCartId
    ) {
        UUID customerId = extractCustomerId(authentication);
        CartResponse response = cartService.getCart(customerId, guestCartId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart", description = "Adds a product to the cart or increments its quantity if already present.")
    public ResponseEntity<CartResponse> addItem(
            Authentication authentication,
            @Parameter(description = "Guest cart session ID (required if unauthenticated)")
            @RequestHeader(value = "X-Guest-Cart-Id", required = false) String guestCartId,
            @Valid @RequestBody AddToCartRequest request
    ) {
        UUID customerId = extractCustomerId(authentication);
        CartResponse response = cartService.addItem(customerId, guestCartId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/items/{productId}")
    @Operation(summary = "Update cart item quantity", description = "Updates item quantity. Setting quantity to 0 removes the item.")
    public ResponseEntity<CartResponse> updateItem(
            Authentication authentication,
            @Parameter(description = "Guest cart session ID (required if unauthenticated)")
            @RequestHeader(value = "X-Guest-Cart-Id", required = false) String guestCartId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        UUID customerId = extractCustomerId(authentication);
        CartResponse response = cartService.updateItemQuantity(customerId, guestCartId, productId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove item from cart", description = "Removes a specific product from the cart.")
    public ResponseEntity<CartResponse> removeItem(
            Authentication authentication,
            @Parameter(description = "Guest cart session ID (required if unauthenticated)")
            @RequestHeader(value = "X-Guest-Cart-Id", required = false) String guestCartId,
            @PathVariable UUID productId
    ) {
        UUID customerId = extractCustomerId(authentication);
        CartResponse response = cartService.removeItem(customerId, guestCartId, productId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    @Operation(summary = "Clear cart", description = "Removes all items from the active cart.")
    public ResponseEntity<Void> clearCart(
            Authentication authentication,
            @Parameter(description = "Guest cart session ID (required if unauthenticated)")
            @RequestHeader(value = "X-Guest-Cart-Id", required = false) String guestCartId
    ) {
        UUID customerId = extractCustomerId(authentication);
        cartService.clearCart(customerId, guestCartId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge guest cart", description = "Merges items from a Redis guest cart into the authenticated user's permanent PostgreSQL cart upon login, then deletes the Redis key.")
    public ResponseEntity<CartResponse> mergeGuestCart(
            Authentication authentication,
            @Valid @RequestBody MergeCartRequest request
    ) {
        UUID customerId = extractCustomerId(authentication);
        if (customerId == null) {
            throw new UnauthorizedCartAccessException("Authentication required to merge cart");
        }
        CartResponse response = cartService.mergeGuestCart(customerId, request.getGuestCartId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items/{productId}/move-to-wishlist")
    @Operation(summary = "Move item from cart to wishlist", description = "Atomically removes a product from the user's cart and saves it to their wishlist.")
    public ResponseEntity<Void> moveToWishlist(
            Authentication authentication,
            @PathVariable UUID productId
    ) {
        UUID customerId = extractCustomerId(authentication);
        if (customerId == null) {
            throw new UnauthorizedCartAccessException("Authentication required to move item to wishlist");
        }
        cartService.moveToWishlist(customerId, productId);
        return ResponseEntity.ok().build();
    }

    private UUID extractCustomerId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UUID userId) {
            return userId;
        }
        return null;
    }
}
