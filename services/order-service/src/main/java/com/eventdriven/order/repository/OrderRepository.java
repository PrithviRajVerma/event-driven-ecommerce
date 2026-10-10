package com.eventdriven.order.repository;

import com.eventdriven.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id AND o.customerId = :customerId")
    Optional<Order> findWithItemsByIdAndCustomerId(@Param("id") UUID id, @Param("customerId") UUID customerId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findWithItemsById(@Param("id") UUID id);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.customerId = :customerId ORDER BY o.createdAt DESC")
    List<Order> findWithItemsByCustomerIdOrderByCreatedAtDesc(@Param("customerId") UUID customerId);

    List<Order> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
