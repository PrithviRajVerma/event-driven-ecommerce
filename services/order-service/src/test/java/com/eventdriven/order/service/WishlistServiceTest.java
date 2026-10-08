package com.eventdriven.order.service;

import com.eventdriven.order.dto.AddToWishlistRequest;
import com.eventdriven.order.dto.MoveWishlistItemToCartRequest;
import com.eventdriven.order.dto.WishlistResponse;
import com.eventdriven.order.entity.Cart;
import com.eventdriven.order.entity.CartItem;
import com.eventdriven.order.entity.Wishlist;
import com.eventdriven.order.entity.WishlistItem;
import com.eventdriven.order.exception.UnauthorizedCartAccessException;
import com.eventdriven.order.exception.WishlistItemNotFoundException;
import com.eventdriven.order.repository.CartRepository;
import com.eventdriven.order.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private WishlistService wishlistService;

    private UUID customerId;
    private UUID productId;
    private Wishlist wishlist;
    private WishlistItem wishlistItem;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();

        wishlist = new Wishlist();
        wishlist.setId(UUID.randomUUID());
        wishlist.setCustomerId(customerId);
        wishlist.setCreatedAt(OffsetDateTime.now());
        wishlist.setUpdatedAt(OffsetDateTime.now());

        wishlistItem = new WishlistItem();
        wishlistItem.setId(UUID.randomUUID());
        wishlistItem.setProductId(productId);
        wishlistItem.setCreatedAt(OffsetDateTime.now());
        wishlist.addItem(wishlistItem);
    }

    @Nested
    @DisplayName("getWishlist tests")
    class GetWishlistTests {

        @Test
        void getWishlist_existingWishlist() {
            when(wishlistRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.of(wishlist));

            WishlistResponse response = wishlistService.getWishlist(customerId);

            assertNotNull(response);
            assertEquals(customerId, response.getCustomerId());
            assertEquals(1, response.getItems().size());
            assertEquals(1, response.getTotalItems());
        }

        @Test
        void getWishlist_emptyWishlist() {
            when(wishlistRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.empty());

            WishlistResponse response = wishlistService.getWishlist(customerId);

            assertNotNull(response);
            assertEquals(customerId, response.getCustomerId());
            assertTrue(response.getItems().isEmpty());
            assertEquals(0, response.getTotalItems());
        }

        @Test
        void getWishlist_unauthenticated_throwsException() {
            assertThrows(UnauthorizedCartAccessException.class, () ->
                    wishlistService.getWishlist(null)
            );
        }
    }

    @Nested
    @DisplayName("addItem tests")
    class AddItemTests {

        @Test
        void addItem_newItem_success() {
            UUID newProduct = UUID.randomUUID();
            AddToWishlistRequest request = new AddToWishlistRequest(newProduct);

            when(wishlistRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.of(wishlist));
            when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(invocation -> invocation.getArgument(0));

            WishlistResponse response = wishlistService.addItem(customerId, request);

            assertNotNull(response);
            assertEquals(2, response.getTotalItems());
            verify(wishlistRepository).save(wishlist);
        }

        @Test
        void addItem_alreadyPresent_isIdempotent() {
            AddToWishlistRequest request = new AddToWishlistRequest(productId);

            when(wishlistRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.of(wishlist));

            WishlistResponse response = wishlistService.addItem(customerId, request);

            assertNotNull(response);
            assertEquals(1, response.getTotalItems());
            verify(wishlistRepository, never()).save(any(Wishlist.class));
        }
    }

    @Nested
    @DisplayName("removeItem tests")
    class RemoveItemTests {

        @Test
        void removeItem_success() {
            when(wishlistRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.of(wishlist));
            when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(invocation -> invocation.getArgument(0));

            WishlistResponse response = wishlistService.removeItem(customerId, productId);

            assertNotNull(response);
            assertEquals(0, response.getTotalItems());
            verify(wishlistRepository).save(wishlist);
        }

        @Test
        void removeItem_itemNotFound_throwsException() {
            UUID otherProduct = UUID.randomUUID();
            when(wishlistRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.of(wishlist));

            assertThrows(WishlistItemNotFoundException.class, () ->
                    wishlistService.removeItem(customerId, otherProduct)
            );
        }
    }

    @Nested
    @DisplayName("moveToCart tests")
    class MoveToCartTests {

        @Test
        void moveToCart_success() {
            MoveWishlistItemToCartRequest request = new MoveWishlistItemToCartRequest(2, new BigDecimal("19.99"));

            Cart cart = new Cart();
            cart.setId(UUID.randomUUID());
            cart.setCustomerId(customerId);

            when(wishlistRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.of(wishlist));
            when(cartRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.of(cart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(invocation -> invocation.getArgument(0));

            wishlistService.moveToCart(customerId, productId, request);

            assertTrue(wishlist.getItems().isEmpty());
            assertEquals(1, cart.getItems().size());
            assertEquals(2, cart.getItems().get(0).getQuantity());
            verify(cartRepository).save(cart);
            verify(wishlistRepository).save(wishlist);
        }

        @Test
        void moveToCart_itemNotFound_throwsException() {
            UUID otherProduct = UUID.randomUUID();
            MoveWishlistItemToCartRequest request = new MoveWishlistItemToCartRequest(1, new BigDecimal("10.00"));

            when(wishlistRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.of(wishlist));

            assertThrows(WishlistItemNotFoundException.class, () ->
                    wishlistService.moveToCart(customerId, otherProduct, request)
            );
        }
    }
}
