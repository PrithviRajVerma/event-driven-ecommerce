package com.eventdriven.order.service;

import com.eventdriven.order.dto.AddToCartRequest;
import com.eventdriven.order.dto.CartResponse;
import com.eventdriven.order.dto.UpdateCartItemRequest;
import com.eventdriven.order.entity.Cart;
import com.eventdriven.order.entity.CartItem;
import com.eventdriven.order.entity.Wishlist;
import com.eventdriven.order.exception.CartItemNotFoundException;
import com.eventdriven.order.exception.InvalidCartOperationException;
import com.eventdriven.order.exception.UnauthorizedCartAccessException;
import com.eventdriven.order.redis.GuestCartRedisService;
import com.eventdriven.order.redis.model.GuestCart;
import com.eventdriven.order.redis.model.GuestCartItem;
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
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private GuestCartRedisService guestCartRedisService;

    @Mock
    private WishlistRepository wishlistRepository;

    @InjectMocks
    private CartService cartService;

    private UUID customerId;
    private String guestCartId;
    private UUID productId;
    private Cart cart;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        guestCartId = "guest-session-123";
        productId = UUID.randomUUID();

        cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setCustomerId(customerId);
        cart.setCreatedAt(OffsetDateTime.now());
        cart.setUpdatedAt(OffsetDateTime.now());

        cartItem = new CartItem();
        cartItem.setId(UUID.randomUUID());
        cartItem.setProductId(productId);
        cartItem.setQuantity(2);
        cartItem.setUnitPrice(new BigDecimal("29.99"));
        cartItem.setCreatedAt(OffsetDateTime.now());
        cartItem.setUpdatedAt(OffsetDateTime.now());
        cart.addItem(cartItem);
    }

    @Nested
    @DisplayName("getCart tests")
    class GetCartTests {

        @Test
        void getCart_authenticated_existingCart() {
            when(cartRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.of(cart));

            CartResponse response = cartService.getCart(customerId, null);

            assertNotNull(response);
            assertEquals(customerId, response.getCustomerId());
            assertEquals(1, response.getItems().size());
            assertEquals(2, response.getTotalItems());
            assertEquals(new BigDecimal("59.98"), response.getTotalAmount());
        }

        @Test
        void getCart_authenticated_emptyCart() {
            when(cartRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.empty());

            CartResponse response = cartService.getCart(customerId, null);

            assertNotNull(response);
            assertEquals(customerId, response.getCustomerId());
            assertTrue(response.getItems().isEmpty());
            assertEquals(0, response.getTotalItems());
            assertEquals(BigDecimal.ZERO, response.getTotalAmount());
        }

        @Test
        void getCart_guest_existingCartInRedis() {
            GuestCart guestCart = GuestCart.builder()
                    .guestCartId(guestCartId)
                    .items(new ArrayList<>(java.util.List.of(
                            GuestCartItem.builder()
                                    .productId(productId)
                                    .quantity(3)
                                    .unitPrice(new BigDecimal("10.00"))
                                    .build()
                    )))
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();

            when(guestCartRedisService.getCart(guestCartId)).thenReturn(Optional.of(guestCart));

            CartResponse response = cartService.getCart(null, guestCartId);

            assertNotNull(response);
            assertNull(response.getCustomerId());
            assertEquals(guestCartId, response.getCartId());
            assertEquals(1, response.getItems().size());
            assertEquals(3, response.getTotalItems());
            assertEquals(new BigDecimal("30.00"), response.getTotalAmount());
        }

        @Test
        void getCart_neitherAuthNorGuest_throwsException() {
            assertThrows(InvalidCartOperationException.class, () ->
                    cartService.getCart(null, null)
            );
        }
    }

    @Nested
    @DisplayName("addItem tests")
    class AddItemTests {

        @Test
        void addItem_authenticated_newItem() {
            UUID newProductId = UUID.randomUUID();
            AddToCartRequest request = new AddToCartRequest(newProductId, 1, new BigDecimal("15.50"));

            when(cartRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.of(cart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CartResponse response = cartService.addItem(customerId, null, request);

            assertNotNull(response);
            assertEquals(2, response.getItems().size());
            assertEquals(3, response.getTotalItems());
            verify(cartRepository).save(cart);
        }

        @Test
        void addItem_authenticated_incrementExistingItem() {
            AddToCartRequest request = new AddToCartRequest(productId, 2, new BigDecimal("29.99"));

            when(cartRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.of(cart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CartResponse response = cartService.addItem(customerId, null, request);

            assertNotNull(response);
            assertEquals(1, response.getItems().size());
            assertEquals(4, response.getTotalItems());
            verify(cartRepository).save(cart);
        }

        @Test
        void addItem_guest_savesToRedis() {
            AddToCartRequest request = new AddToCartRequest(productId, 1, new BigDecimal("10.00"));
            when(guestCartRedisService.getCart(guestCartId)).thenReturn(Optional.empty());

            CartResponse response = cartService.addItem(null, guestCartId, request);

            assertNotNull(response);
            assertEquals(1, response.getItems().size());
            assertEquals(1, response.getTotalItems());
            verify(guestCartRedisService).saveCart(any(GuestCart.class));
        }
    }

    @Nested
    @DisplayName("updateItemQuantity tests")
    class UpdateItemQuantityTests {

        @Test
        void updateItemQuantity_authenticated_success() {
            UpdateCartItemRequest request = new UpdateCartItemRequest(5);
            when(cartRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.of(cart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CartResponse response = cartService.updateItemQuantity(customerId, null, productId, request);

            assertNotNull(response);
            assertEquals(5, response.getTotalItems());
            verify(cartRepository).save(cart);
        }

        @Test
        void updateItemQuantity_authenticated_quantityZero_removesItem() {
            UpdateCartItemRequest request = new UpdateCartItemRequest(0);
            when(cartRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.of(cart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CartResponse response = cartService.updateItemQuantity(customerId, null, productId, request);

            assertNotNull(response);
            assertTrue(response.getItems().isEmpty());
            assertEquals(0, response.getTotalItems());
        }

        @Test
        void updateItemQuantity_authenticated_itemNotFound_throwsException() {
            UpdateCartItemRequest request = new UpdateCartItemRequest(3);
            when(cartRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.of(cart));

            UUID randomId = UUID.randomUUID();
            assertThrows(CartItemNotFoundException.class, () ->
                    cartService.updateItemQuantity(customerId, null, randomId, request)
            );
        }
    }

    @Nested
    @DisplayName("removeItem tests")
    class RemoveItemTests {

        @Test
        void removeItem_authenticated_success() {
            when(cartRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.of(cart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CartResponse response = cartService.removeItem(customerId, null, productId);

            assertNotNull(response);
            assertTrue(response.getItems().isEmpty());
            verify(cartRepository).save(cart);
        }

        @Test
        void removeItem_guest_success() {
            GuestCart guestCart = GuestCart.builder()
                    .guestCartId(guestCartId)
                    .items(new ArrayList<>(java.util.List.of(
                            GuestCartItem.builder().productId(productId).quantity(1).unitPrice(new BigDecimal("10.00")).build()
                    )))
                    .build();

            when(guestCartRedisService.getCart(guestCartId)).thenReturn(Optional.of(guestCart));

            CartResponse response = cartService.removeItem(null, guestCartId, productId);

            assertNotNull(response);
            assertTrue(response.getItems().isEmpty());
            verify(guestCartRedisService).saveCart(guestCart);
        }
    }

    @Nested
    @DisplayName("clearCart tests")
    class ClearCartTests {

        @Test
        void clearCart_authenticated_clearsItems() {
            when(cartRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.of(cart));

            cartService.clearCart(customerId, null);

            assertTrue(cart.getItems().isEmpty());
            verify(cartRepository).save(cart);
        }

        @Test
        void clearCart_guest_deletesFromRedis() {
            cartService.clearCart(null, guestCartId);

            verify(guestCartRedisService).deleteCart(guestCartId);
        }
    }

    @Nested
    @DisplayName("mergeGuestCart tests")
    class MergeGuestCartTests {

        @Test
        void mergeGuestCart_mergesItemsAndDeletesRedisCart() {
            UUID guestProduct = UUID.randomUUID();
            GuestCart guestCart = GuestCart.builder()
                    .guestCartId(guestCartId)
                    .items(new ArrayList<>(java.util.List.of(
                            GuestCartItem.builder()
                                    .productId(productId) // existing product -> quantity merges
                                    .quantity(3)
                                    .unitPrice(new BigDecimal("29.99"))
                                    .build(),
                            GuestCartItem.builder()
                                    .productId(guestProduct) // new product
                                    .quantity(1)
                                    .unitPrice(new BigDecimal("49.99"))
                                    .build()
                    )))
                    .build();

            when(guestCartRedisService.getCart(guestCartId)).thenReturn(Optional.of(guestCart));
            when(cartRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.of(cart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CartResponse response = cartService.mergeGuestCart(customerId, guestCartId);

            assertNotNull(response);
            assertEquals(2, response.getItems().size());
            assertEquals(6, response.getTotalItems()); // 2 + 3 + 1 = 6
            verify(guestCartRedisService).deleteCart(guestCartId);
        }

        @Test
        void mergeGuestCart_unauthenticated_throwsException() {
            assertThrows(UnauthorizedCartAccessException.class, () ->
                    cartService.mergeGuestCart(null, guestCartId)
            );
        }
    }

    @Nested
    @DisplayName("moveToWishlist tests")
    class MoveToWishlistTests {

        @Test
        void moveToWishlist_success() {
            when(cartRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.of(cart));
            when(wishlistRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.empty());
            when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(invocation -> invocation.getArgument(0));

            cartService.moveToWishlist(customerId, productId);

            assertTrue(cart.getItems().isEmpty());
            verify(wishlistRepository).save(any(Wishlist.class));
            verify(cartRepository).save(cart);
        }

        @Test
        void moveToWishlist_unauthenticated_throwsException() {
            assertThrows(UnauthorizedCartAccessException.class, () ->
                    cartService.moveToWishlist(null, productId)
            );
        }
    }
}
