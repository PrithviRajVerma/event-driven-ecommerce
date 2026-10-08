package com.eventdriven.order.service;

import com.eventdriven.order.dto.*;
import com.eventdriven.order.entity.Cart;
import com.eventdriven.order.entity.CartItem;
import com.eventdriven.order.entity.Wishlist;
import com.eventdriven.order.entity.WishlistItem;
import com.eventdriven.order.exception.CartItemNotFoundException;
import com.eventdriven.order.exception.InvalidCartOperationException;
import com.eventdriven.order.exception.UnauthorizedCartAccessException;
import com.eventdriven.order.redis.GuestCartRedisService;
import com.eventdriven.order.redis.model.GuestCart;
import com.eventdriven.order.redis.model.GuestCartItem;
import com.eventdriven.order.repository.CartRepository;
import com.eventdriven.order.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final GuestCartRedisService guestCartRedisService;
    private final WishlistRepository wishlistRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID customerId, String guestCartId) {
        if (customerId != null) {
            return getAuthenticatedCart(customerId);
        } else if (isValidGuestId(guestCartId)) {
            return getGuestCart(guestCartId);
        } else {
            throw new InvalidCartOperationException(
                    "Either authenticated user credentials or a valid X-Guest-Cart-Id header must be provided"
            );
        }
    }

    @Transactional
    public CartResponse addItem(UUID customerId, String guestCartId, AddToCartRequest request) {
        if (customerId != null) {
            return addAuthenticatedItem(customerId, request);
        } else if (isValidGuestId(guestCartId)) {
            return addGuestItem(guestCartId, request);
        } else {
            throw new InvalidCartOperationException(
                    "Either authenticated user credentials or a valid X-Guest-Cart-Id header must be provided"
            );
        }
    }

    @Transactional
    public CartResponse updateItemQuantity(
            UUID customerId,
            String guestCartId,
            UUID productId,
            UpdateCartItemRequest request
    ) {
        if (request.getQuantity() == 0) {
            return removeItem(customerId, guestCartId, productId);
        }

        if (customerId != null) {
            return updateAuthenticatedItemQuantity(customerId, productId, request.getQuantity());
        } else if (isValidGuestId(guestCartId)) {
            return updateGuestItemQuantity(guestCartId, productId, request.getQuantity());
        } else {
            throw new InvalidCartOperationException(
                    "Either authenticated user credentials or a valid X-Guest-Cart-Id header must be provided"
            );
        }
    }

    @Transactional
    public CartResponse removeItem(UUID customerId, String guestCartId, UUID productId) {
        if (customerId != null) {
            return removeAuthenticatedItem(customerId, productId);
        } else if (isValidGuestId(guestCartId)) {
            return removeGuestItem(guestCartId, productId);
        } else {
            throw new InvalidCartOperationException(
                    "Either authenticated user credentials or a valid X-Guest-Cart-Id header must be provided"
            );
        }
    }

    @Transactional
    public void clearCart(UUID customerId, String guestCartId) {
        if (customerId != null) {
            cartRepository.findWithItemsByCustomerId(customerId).ifPresent(cart -> {
                cart.getItems().clear();
                cart.setUpdatedAt(OffsetDateTime.now());
                cartRepository.save(cart);
            });
        } else if (isValidGuestId(guestCartId)) {
            guestCartRedisService.deleteCart(guestCartId);
        } else {
            throw new InvalidCartOperationException(
                    "Either authenticated user credentials or a valid X-Guest-Cart-Id header must be provided"
            );
        }
    }

    @Transactional
    public CartResponse mergeGuestCart(UUID customerId, String guestCartId) {
        if (customerId == null) {
            throw new UnauthorizedCartAccessException("You must be logged in to merge a guest cart");
        }
        if (!isValidGuestId(guestCartId)) {
            throw new InvalidCartOperationException("Invalid guestCartId provided for merge");
        }

        Optional<GuestCart> guestCartOpt = guestCartRedisService.getCart(guestCartId);
        if (guestCartOpt.isEmpty() || guestCartOpt.get().getItems() == null || guestCartOpt.get().getItems().isEmpty()) {
            return getAuthenticatedCart(customerId);
        }

        GuestCart guestCart = guestCartOpt.get();
        Cart userCart = cartRepository.findWithItemsByCustomerId(customerId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setCustomerId(customerId);
                    newCart.setCreatedAt(OffsetDateTime.now());
                    newCart.setUpdatedAt(OffsetDateTime.now());
                    return cartRepository.save(newCart);
                });

        for (GuestCartItem guestItem : guestCart.getItems()) {
            Optional<CartItem> existingItemOpt = userCart.findItemByProductId(guestItem.getProductId());
            if (existingItemOpt.isPresent()) {
                CartItem item = existingItemOpt.get();
                item.setQuantity(item.getQuantity() + guestItem.getQuantity());
                item.setUnitPrice(guestItem.getUnitPrice());
                item.setUpdatedAt(OffsetDateTime.now());
            } else {
                CartItem newItem = new CartItem();
                newItem.setProductId(guestItem.getProductId());
                newItem.setQuantity(guestItem.getQuantity());
                newItem.setUnitPrice(guestItem.getUnitPrice());
                newItem.setCreatedAt(OffsetDateTime.now());
                newItem.setUpdatedAt(OffsetDateTime.now());
                userCart.addItem(newItem);
            }
        }

        userCart.setUpdatedAt(OffsetDateTime.now());
        cartRepository.save(userCart);

        // Delete guest cart from Redis after successful merge
        guestCartRedisService.deleteCart(guestCartId);
        log.info("Successfully merged guest cart {} into customer cart for {}", guestCartId, customerId);

        return mapToCartResponse(userCart);
    }

    @Transactional
    public void moveToWishlist(UUID customerId, UUID productId) {
        if (customerId == null) {
            throw new UnauthorizedCartAccessException("You must be logged in to move an item to wishlist");
        }

        Cart cart = cartRepository.findWithItemsByCustomerId(customerId)
                .orElseThrow(() -> new CartItemNotFoundException(
                        "Cart item with product ID " + productId + " not found"
                ));

        CartItem item = cart.findItemByProductId(productId)
                .orElseThrow(() -> new CartItemNotFoundException(
                        "Product " + productId + " not found in cart"
                ));

        // Add to wishlist
        Wishlist wishlist = wishlistRepository.findWithItemsByCustomerId(customerId)
                .orElseGet(() -> {
                    Wishlist newWishlist = new Wishlist();
                    newWishlist.setCustomerId(customerId);
                    newWishlist.setCreatedAt(OffsetDateTime.now());
                    newWishlist.setUpdatedAt(OffsetDateTime.now());
                    return newWishlist;
                });

        if (wishlist.findItemByProductId(productId).isEmpty()) {
            WishlistItem wishlistItem = new WishlistItem();
            wishlistItem.setProductId(productId);
            wishlistItem.setCreatedAt(OffsetDateTime.now());
            wishlist.addItem(wishlistItem);
            wishlist.setUpdatedAt(OffsetDateTime.now());
            wishlistRepository.save(wishlist);
        }

        // Remove from cart
        cart.removeItem(item);
        cart.setUpdatedAt(OffsetDateTime.now());
        cartRepository.save(cart);
        log.info("Moved product {} from cart to wishlist for customer {}", productId, customerId);
    }

    // --- Private Helper Methods: Authenticated User (PostgreSQL) ---

    private CartResponse getAuthenticatedCart(UUID customerId) {
        return cartRepository.findWithItemsByCustomerId(customerId)
                .map(this::mapToCartResponse)
                .orElseGet(() -> emptyCartResponse(null, customerId));
    }

    private CartResponse addAuthenticatedItem(UUID customerId, AddToCartRequest request) {
        Cart cart = cartRepository.findWithItemsByCustomerId(customerId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setCustomerId(customerId);
                    newCart.setCreatedAt(OffsetDateTime.now());
                    newCart.setUpdatedAt(OffsetDateTime.now());
                    return cartRepository.save(newCart);
                });

        Optional<CartItem> existingItem = cart.findItemByProductId(request.getProductId());
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item.setUnitPrice(request.getUnitPrice());
            item.setUpdatedAt(OffsetDateTime.now());
        } else {
            CartItem newItem = new CartItem();
            newItem.setProductId(request.getProductId());
            newItem.setQuantity(request.getQuantity());
            newItem.setUnitPrice(request.getUnitPrice());
            newItem.setCreatedAt(OffsetDateTime.now());
            newItem.setUpdatedAt(OffsetDateTime.now());
            cart.addItem(newItem);
        }

        cart.setUpdatedAt(OffsetDateTime.now());
        Cart saved = cartRepository.save(cart);
        return mapToCartResponse(saved);
    }

    private CartResponse updateAuthenticatedItemQuantity(UUID customerId, UUID productId, int quantity) {
        Cart cart = cartRepository.findWithItemsByCustomerId(customerId)
                .orElseThrow(() -> new CartItemNotFoundException("Item with product ID " + productId + " not found"));

        CartItem item = cart.findItemByProductId(productId)
                .orElseThrow(() -> new CartItemNotFoundException("Item with product ID " + productId + " not found"));

        item.setQuantity(quantity);
        item.setUpdatedAt(OffsetDateTime.now());
        cart.setUpdatedAt(OffsetDateTime.now());
        Cart saved = cartRepository.save(cart);
        return mapToCartResponse(saved);
    }

    private CartResponse removeAuthenticatedItem(UUID customerId, UUID productId) {
        Cart cart = cartRepository.findWithItemsByCustomerId(customerId)
                .orElseThrow(() -> new CartItemNotFoundException("Item with product ID " + productId + " not found"));

        CartItem item = cart.findItemByProductId(productId)
                .orElseThrow(() -> new CartItemNotFoundException("Item with product ID " + productId + " not found"));

        cart.removeItem(item);
        cart.setUpdatedAt(OffsetDateTime.now());
        Cart saved = cartRepository.save(cart);
        return mapToCartResponse(saved);
    }

    // --- Private Helper Methods: Guest User (Redis) ---

    private CartResponse getGuestCart(String guestCartId) {
        return guestCartRedisService.getCart(guestCartId)
                .map(this::mapGuestToCartResponse)
                .orElseGet(() -> emptyCartResponse(guestCartId, null));
    }

    private CartResponse addGuestItem(String guestCartId, AddToCartRequest request) {
        GuestCart cart = guestCartRedisService.getCart(guestCartId)
                .orElseGet(() -> GuestCart.builder()
                        .guestCartId(guestCartId)
                        .items(new ArrayList<>())
                        .createdAt(OffsetDateTime.now())
                        .updatedAt(OffsetDateTime.now())
                        .build());

        Optional<GuestCartItem> existingItem = cart.findItemByProductId(request.getProductId());
        if (existingItem.isPresent()) {
            GuestCartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item.setUnitPrice(request.getUnitPrice());
        } else {
            GuestCartItem newItem = GuestCartItem.builder()
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .unitPrice(request.getUnitPrice())
                    .build();
            cart.getItems().add(newItem);
        }

        cart.setUpdatedAt(OffsetDateTime.now());
        guestCartRedisService.saveCart(cart);
        return mapGuestToCartResponse(cart);
    }

    private CartResponse updateGuestItemQuantity(String guestCartId, UUID productId, int quantity) {
        GuestCart cart = guestCartRedisService.getCart(guestCartId)
                .orElseThrow(() -> new CartItemNotFoundException("Item with product ID " + productId + " not found"));

        GuestCartItem item = cart.findItemByProductId(productId)
                .orElseThrow(() -> new CartItemNotFoundException("Item with product ID " + productId + " not found"));

        item.setQuantity(quantity);
        cart.setUpdatedAt(OffsetDateTime.now());
        guestCartRedisService.saveCart(cart);
        return mapGuestToCartResponse(cart);
    }

    private CartResponse removeGuestItem(String guestCartId, UUID productId) {
        GuestCart cart = guestCartRedisService.getCart(guestCartId)
                .orElseThrow(() -> new CartItemNotFoundException("Item with product ID " + productId + " not found"));

        GuestCartItem item = cart.findItemByProductId(productId)
                .orElseThrow(() -> new CartItemNotFoundException("Item with product ID " + productId + " not found"));

        cart.getItems().remove(item);
        cart.setUpdatedAt(OffsetDateTime.now());
        guestCartRedisService.saveCart(cart);
        return mapGuestToCartResponse(cart);
    }

    // --- Private Helper Methods: Response Mappings ---

    private CartResponse mapToCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> CartItemResponse.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .toList();

        int totalItems = itemResponses.stream().mapToInt(CartItemResponse::getQuantity).sum();
        BigDecimal totalAmount = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId().toString())
                .customerId(cart.getCustomerId())
                .items(itemResponses)
                .totalItems(totalItems)
                .totalAmount(totalAmount)
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    private CartResponse mapGuestToCartResponse(GuestCart cart) {
        List<CartItemResponse> itemResponses = (cart.getItems() != null ? cart.getItems() : List.<GuestCartItem>of())
                .stream()
                .map(item -> CartItemResponse.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .toList();

        int totalItems = itemResponses.stream().mapToInt(CartItemResponse::getQuantity).sum();
        BigDecimal totalAmount = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getGuestCartId())
                .customerId(null)
                .items(itemResponses)
                .totalItems(totalItems)
                .totalAmount(totalAmount)
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    private CartResponse emptyCartResponse(String guestCartId, UUID customerId) {
        return CartResponse.builder()
                .cartId(guestCartId != null ? guestCartId : (customerId != null ? customerId.toString() : null))
                .customerId(customerId)
                .items(List.of())
                .totalItems(0)
                .totalAmount(BigDecimal.ZERO)
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private boolean isValidGuestId(String guestCartId) {
        return guestCartId != null && !guestCartId.isBlank();
    }
}
