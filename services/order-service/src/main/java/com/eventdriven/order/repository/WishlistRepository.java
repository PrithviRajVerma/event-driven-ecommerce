package com.eventdriven.order.repository;

import com.eventdriven.order.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {

    Optional<Wishlist> findByCustomerId(UUID customerId);

    @Query("SELECT w FROM Wishlist w LEFT JOIN FETCH w.items WHERE w.customerId = :customerId")
    Optional<Wishlist> findWithItemsByCustomerId(@Param("customerId") UUID customerId);

    void deleteByCustomerId(UUID customerId);
}
