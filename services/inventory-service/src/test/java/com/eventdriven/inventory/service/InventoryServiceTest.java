package com.eventdriven.inventory.service;

import com.eventdriven.events.inventory.InventoryReservedEvent;
import com.eventdriven.events.order.OrderCreatedEvent;
import com.eventdriven.inventory.dto.*;
import com.eventdriven.inventory.entity.Inventory;
import com.eventdriven.inventory.exception.InsufficientReservedStockException;
import com.eventdriven.inventory.exception.InsufficientStockException;
import com.eventdriven.inventory.exception.InventoryAlreadyExistsException;
import com.eventdriven.inventory.exception.InventoryNotFoundException;
import com.eventdriven.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private UUID productId;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        inventory = new Inventory();
        inventory.setId(UUID.randomUUID());
        inventory.setProductId(productId);
        inventory.setAvailableQuantity(10);
        inventory.setReservedQuantity(5);
        inventory.setVersion(0L);
        inventory.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        inventory.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Nested
    @DisplayName("createInventory tests")
    class CreateInventoryTests {

        @Test
        void createInventory_success() {
            CreateInventoryRequest request = new CreateInventoryRequest(productId, 20);
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

            InventoryResponse response = inventoryService.createInventory(request);

            assertNotNull(response);
            assertEquals(productId, response.productId());
            assertEquals(20, response.availableQuantity());
            assertEquals(0, response.reservedQuantity());
            verify(inventoryRepository).save(any(Inventory.class));
        }

        @Test
        void createInventory_alreadyExists_throwsException() {
            CreateInventoryRequest request = new CreateInventoryRequest(productId, 20);
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

            assertThrows(InventoryAlreadyExistsException.class, () -> inventoryService.createInventory(request));
            verify(inventoryRepository, never()).save(any(Inventory.class));
        }
    }

    @Nested
    @DisplayName("getInventoryByProductId tests")
    class GetInventoryTests {

        @Test
        void getInventoryByProductId_found() {
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

            InventoryResponse response = inventoryService.getInventoryByProductId(productId);

            assertNotNull(response);
            assertEquals(productId, response.productId());
            assertEquals(10, response.availableQuantity());
            assertEquals(5, response.reservedQuantity());
        }

        @Test
        void getInventoryByProductId_notFound_throwsException() {
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

            assertThrows(InventoryNotFoundException.class, () -> inventoryService.getInventoryByProductId(productId));
        }
    }

    @Nested
    @DisplayName("reserveInventory tests")
    class ReserveInventoryTests {

        @Test
        void reserveInventory_success() {
            ReserveInventoryRequest request = new ReserveInventoryRequest(productId, 4);
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

            InventoryResponse response = inventoryService.reserveInventory(request);

            assertNotNull(response);
            assertEquals(6, response.availableQuantity()); // 10 - 4
            assertEquals(9, response.reservedQuantity());  // 5 + 4
            verify(inventoryRepository).save(inventory);
        }

        @Test
        void reserveInventory_insufficientStock_throwsException() {
            ReserveInventoryRequest request = new ReserveInventoryRequest(productId, 15);
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

            assertThrows(InsufficientStockException.class, () -> inventoryService.reserveInventory(request));
            verify(inventoryRepository, never()).save(any(Inventory.class));
        }
    }

    @Nested
    @DisplayName("releaseInventory tests")
    class ReleaseInventoryTests {

        @Test
        void releaseInventory_success() {
            ReleaseInventoryRequest request = new ReleaseInventoryRequest(productId, 3);
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

            InventoryResponse response = inventoryService.releaseInventory(request);

            assertNotNull(response);
            assertEquals(13, response.availableQuantity()); // 10 + 3
            assertEquals(2, response.reservedQuantity());   // 5 - 3
            verify(inventoryRepository).save(inventory);
        }

        @Test
        void releaseInventory_exceedsReservedQuantity_throwsException() {
            ReleaseInventoryRequest request = new ReleaseInventoryRequest(productId, 10); // currently reserved is 5
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

            assertThrows(InsufficientReservedStockException.class, () -> inventoryService.releaseInventory(request));
            verify(inventoryRepository, never()).save(any(Inventory.class));
        }
    }

    @Nested
    @DisplayName("confirmInventory tests")
    class ConfirmInventoryTests {

        @Test
        void confirmInventory_success() {
            ConfirmInventoryRequest request = new ConfirmInventoryRequest(productId, 3);
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

            InventoryResponse response = inventoryService.confirmInventory(request);

            assertNotNull(response);
            assertEquals(10, response.availableQuantity()); // unchanged
            assertEquals(2, response.reservedQuantity());   // 5 - 3
            verify(inventoryRepository).save(inventory);
        }

        @Test
        void confirmInventory_exceedsReservedQuantity_throwsException() {
            ConfirmInventoryRequest request = new ConfirmInventoryRequest(productId, 10); // currently reserved is 5
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

            assertThrows(InsufficientReservedStockException.class, () -> inventoryService.confirmInventory(request));
            verify(inventoryRepository, never()).save(any(Inventory.class));
        }
    }

    @Nested
    @DisplayName("addStock and updateStock tests")
    class AdminStockManagementTests {

        @Test
        void addStock_success() {
            AddStockRequest request = new AddStockRequest(15);
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

            InventoryResponse response = inventoryService.addStock(productId, request);

            assertNotNull(response);
            assertEquals(25, response.availableQuantity()); // 10 + 15
            assertEquals(5, response.reservedQuantity());   // unchanged
            verify(inventoryRepository).save(inventory);
        }

        @Test
        void updateStock_success() {
            UpdateStockRequest request = new UpdateStockRequest(50);
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

            InventoryResponse response = inventoryService.updateStock(productId, request);

            assertNotNull(response);
            assertEquals(50, response.availableQuantity());
            assertEquals(5, response.reservedQuantity());   // unchanged
            verify(inventoryRepository).save(inventory);
        }
    }

    @Nested
    @DisplayName("reserveOrderInventory tests")
    class ReserveOrderInventoryTests {

        @Test
        void reserveOrderInventory_success() {
            UUID orderId = UUID.randomUUID();
            UUID prodId2 = UUID.randomUUID();

            Inventory inventory2 = new Inventory();
            inventory2.setId(UUID.randomUUID());
            inventory2.setProductId(prodId2);
            inventory2.setAvailableQuantity(20);
            inventory2.setReservedQuantity(0);

            OrderCreatedEvent.OrderItem item1 = OrderCreatedEvent.OrderItem.builder()
                    .productId(productId)
                    .quantity(3)
                    .build();

            OrderCreatedEvent.OrderItem item2 = OrderCreatedEvent.OrderItem.builder()
                    .productId(prodId2)
                    .quantity(5)
                    .build();

            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.findByProductId(prodId2)).thenReturn(Optional.of(inventory2));
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

            List<InventoryReservedEvent.ReservedItem> reserved = inventoryService.reserveOrderInventory(
                    orderId,
                    List.of(item1, item2)
            );

            assertNotNull(reserved);
            assertEquals(2, reserved.size());
            assertEquals(7, inventory.getAvailableQuantity()); // 10 - 3
            assertEquals(8, inventory.getReservedQuantity());  // 5 + 3
            assertEquals(15, inventory2.getAvailableQuantity()); // 20 - 5
            assertEquals(5, inventory2.getReservedQuantity());  // 0 + 5

            verify(inventoryRepository).save(inventory);
            verify(inventoryRepository).save(inventory2);
        }

        @Test
        void reserveOrderInventory_insufficientStock_throwsException() {
            UUID orderId = UUID.randomUUID();
            OrderCreatedEvent.OrderItem item = OrderCreatedEvent.OrderItem.builder()
                    .productId(productId)
                    .quantity(50) // only 10 available
                    .build();

            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

            assertThrows(InsufficientStockException.class, () ->
                    inventoryService.reserveOrderInventory(orderId, List.of(item))
            );

            verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        void reserveOrderInventory_productNotFound_throwsException() {
            UUID orderId = UUID.randomUUID();
            UUID missingProductId = UUID.randomUUID();
            OrderCreatedEvent.OrderItem item = OrderCreatedEvent.OrderItem.builder()
                    .productId(missingProductId)
                    .quantity(2)
                    .build();

            when(inventoryRepository.findByProductId(missingProductId)).thenReturn(Optional.empty());

            assertThrows(InventoryNotFoundException.class, () ->
                    inventoryService.reserveOrderInventory(orderId, List.of(item))
            );

            verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        void reserveOrderInventory_emptyOrNull_throwsException() {
            UUID orderId = UUID.randomUUID();

            assertThrows(IllegalArgumentException.class, () ->
                    inventoryService.reserveOrderInventory(orderId, null)
            );

            assertThrows(IllegalArgumentException.class, () ->
                    inventoryService.reserveOrderInventory(orderId, List.of())
            );

            assertThrows(IllegalArgumentException.class, () ->
                    inventoryService.reserveOrderInventory(null, List.of(
                            OrderCreatedEvent.OrderItem.builder().productId(productId).quantity(1).build()
                    ))
            );
        }
    }
}
