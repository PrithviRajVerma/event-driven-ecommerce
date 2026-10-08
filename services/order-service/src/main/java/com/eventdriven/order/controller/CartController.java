package com.eventdriven.order.controller;

import com.eventdriven.order.dto.*;
import com.eventdriven.order.exception.UnauthorizedCartAccessException;
import com.eventdriven.order.service.CartService;
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
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Cart-Id", required = false) String guestCartId
    ) {
        UUID customerId = extractCustomerId(authentication);
        CartResponse response = cartService.getCart(customerId, guestCartId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Cart-Id", required = false) String guestCartId,
            @Valid @RequestBody AddToCartRequest request
    ) {
        UUID customerId = extractCustomerId(authentication);
        CartResponse response = cartService.addItem(customerId, guestCartId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateItem(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Cart-Id", required = false) String guestCartId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        UUID customerId = extractCustomerId(authentication);
        CartResponse response = cartService.updateItemQuantity(customerId, guestCartId, productId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Cart-Id", required = false) String guestCartId,
            @PathVariable UUID productId
    ) {
        UUID customerId = extractCustomerId(authentication);
        CartResponse response = cartService.removeItem(customerId, guestCartId, productId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            Authentication authentication,
            @RequestHeader(value = "X-Guest-Cart-Id", required = false) String guestCartId
    ) {
        UUID customerId = extractCustomerId(authentication);
        cartService.clearCart(customerId, guestCartId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/merge")
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
