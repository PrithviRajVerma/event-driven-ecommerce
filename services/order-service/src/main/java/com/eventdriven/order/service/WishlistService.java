package com.eventdriven.order.service;

import com.eventdriven.order.dto.AddToWishlistRequest;
import com.eventdriven.order.dto.MoveWishlistItemToCartRequest;
import com.eventdriven.order.dto.WishlistItemResponse;
import com.eventdriven.order.dto.WishlistResponse;
import com.eventdriven.order.entity.Cart;
import com.eventdriven.order.entity.CartItem;
import com.eventdriven.order.entity.Wishlist;
import com.eventdriven.order.entity.WishlistItem;
import com.eventdriven.order.exception.UnauthorizedCartAccessException;
import com.eventdriven.order.exception.WishlistItemNotFoundException;
import com.eventdriven.order.repository.CartRepository;
import com.eventdriven.order.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final CartRepository cartRepository;

    @Transactional(readOnly = true)
    public WishlistResponse getWishlist(UUID customerId) {
        validateCustomer(customerId);
        return wishlistRepository.findWithItemsByCustomerId(customerId)
                .map(this::mapToWishlistResponse)
                .orElseGet(() -> emptyWishlistResponse(customerId));
    }

    @Transactional
    public WishlistResponse addItem(UUID customerId, AddToWishlistRequest request) {
        validateCustomer(customerId);

        Wishlist wishlist = wishlistRepository.findWithItemsByCustomerId(customerId)
                .orElseGet(() -> {
                    Wishlist newWishlist = new Wishlist();
                    newWishlist.setCustomerId(customerId);
                    newWishlist.setCreatedAt(OffsetDateTime.now());
                    newWishlist.setUpdatedAt(OffsetDateTime.now());
                    return wishlistRepository.save(newWishlist);
                });

        Optional<WishlistItem> existingItem = wishlist.findItemByProductId(request.getProductId());
        if (existingItem.isEmpty()) {
            WishlistItem item = new WishlistItem();
            item.setProductId(request.getProductId());
            item.setCreatedAt(OffsetDateTime.now());
            wishlist.addItem(item);
            wishlist.setUpdatedAt(OffsetDateTime.now());
            wishlist = wishlistRepository.save(wishlist);
        }

        return mapToWishlistResponse(wishlist);
    }

    @Transactional
    public WishlistResponse removeItem(UUID customerId, UUID productId) {
        validateCustomer(customerId);

        Wishlist wishlist = wishlistRepository.findWithItemsByCustomerId(customerId)
                .orElseThrow(() -> new WishlistItemNotFoundException(
                        "Wishlist item with product ID " + productId + " not found"
                ));

        WishlistItem item = wishlist.findItemByProductId(productId)
                .orElseThrow(() -> new WishlistItemNotFoundException(
                        "Wishlist item with product ID " + productId + " not found"
                ));

        wishlist.removeItem(item);
        wishlist.setUpdatedAt(OffsetDateTime.now());
        Wishlist saved = wishlistRepository.save(wishlist);

        return mapToWishlistResponse(saved);
    }

    @Transactional
    public void moveToCart(UUID customerId, UUID productId, MoveWishlistItemToCartRequest request) {
        validateCustomer(customerId);

        Wishlist wishlist = wishlistRepository.findWithItemsByCustomerId(customerId)
                .orElseThrow(() -> new WishlistItemNotFoundException(
                        "Wishlist item with product ID " + productId + " not found"
                ));

        WishlistItem item = wishlist.findItemByProductId(productId)
                .orElseThrow(() -> new WishlistItemNotFoundException(
                        "Wishlist item with product ID " + productId + " not found"
                ));

        // Add to cart
        Cart cart = cartRepository.findWithItemsByCustomerId(customerId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setCustomerId(customerId);
                    newCart.setCreatedAt(OffsetDateTime.now());
                    newCart.setUpdatedAt(OffsetDateTime.now());
                    return cartRepository.save(newCart);
                });

        Optional<CartItem> existingCartItem = cart.findItemByProductId(productId);
        if (existingCartItem.isPresent()) {
            CartItem cartItem = existingCartItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
            cartItem.setUnitPrice(request.getUnitPrice());
            cartItem.setUpdatedAt(OffsetDateTime.now());
        } else {
            CartItem newCartItem = new CartItem();
            newCartItem.setProductId(productId);
            newCartItem.setQuantity(request.getQuantity());
            newCartItem.setUnitPrice(request.getUnitPrice());
            newCartItem.setCreatedAt(OffsetDateTime.now());
            newCartItem.setUpdatedAt(OffsetDateTime.now());
            cart.addItem(newCartItem);
        }

        cart.setUpdatedAt(OffsetDateTime.now());
        cartRepository.save(cart);

        // Remove from wishlist
        wishlist.removeItem(item);
        wishlist.setUpdatedAt(OffsetDateTime.now());
        wishlistRepository.save(wishlist);
        log.info("Moved product {} from wishlist to cart for customer {}", productId, customerId);
    }

    private WishlistResponse mapToWishlistResponse(Wishlist wishlist) {
        List<WishlistItemResponse> itemResponses = wishlist.getItems().stream()
                .map(item -> WishlistItemResponse.builder()
                        .productId(item.getProductId())
                        .addedAt(item.getCreatedAt())
                        .build())
                .toList();

        return WishlistResponse.builder()
                .customerId(wishlist.getCustomerId())
                .items(itemResponses)
                .totalItems(itemResponses.size())
                .updatedAt(wishlist.getUpdatedAt())
                .build();
    }

    private WishlistResponse emptyWishlistResponse(UUID customerId) {
        return WishlistResponse.builder()
                .customerId(customerId)
                .items(List.of())
                .totalItems(0)
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private void validateCustomer(UUID customerId) {
        if (customerId == null) {
            throw new UnauthorizedCartAccessException("You must be logged in to manage your wishlist");
        }
    }
}
