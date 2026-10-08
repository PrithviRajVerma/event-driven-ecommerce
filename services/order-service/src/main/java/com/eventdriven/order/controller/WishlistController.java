package com.eventdriven.order.controller;

import com.eventdriven.order.dto.AddToWishlistRequest;
import com.eventdriven.order.dto.MoveWishlistItemToCartRequest;
import com.eventdriven.order.dto.WishlistResponse;
import com.eventdriven.order.exception.UnauthorizedCartAccessException;
import com.eventdriven.order.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<WishlistResponse> getWishlist(Authentication authentication) {
        UUID customerId = getRequiredCustomerId(authentication);
        WishlistResponse response = wishlistService.getWishlist(customerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items")
    public ResponseEntity<WishlistResponse> addItem(
            Authentication authentication,
            @Valid @RequestBody AddToWishlistRequest request
    ) {
        UUID customerId = getRequiredCustomerId(authentication);
        WishlistResponse response = wishlistService.addItem(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<WishlistResponse> removeItem(
            Authentication authentication,
            @PathVariable UUID productId
    ) {
        UUID customerId = getRequiredCustomerId(authentication);
        WishlistResponse response = wishlistService.removeItem(customerId, productId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items/{productId}/move-to-cart")
    public ResponseEntity<Void> moveToCart(
            Authentication authentication,
            @PathVariable UUID productId,
            @Valid @RequestBody MoveWishlistItemToCartRequest request
    ) {
        UUID customerId = getRequiredCustomerId(authentication);
        wishlistService.moveToCart(customerId, productId, request);
        return ResponseEntity.ok().build();
    }

    private UUID getRequiredCustomerId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UUID userId) {
            return userId;
        }
        throw new UnauthorizedCartAccessException("Authentication required to access wishlist");
    }
}
