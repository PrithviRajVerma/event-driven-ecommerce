package com.eventdriven.inventory.repository;

import com.eventdriven.inventory.domain.OrderReservation;
import com.eventdriven.inventory.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderReservationRepository extends JpaRepository<OrderReservation, UUID> {

    List<OrderReservation> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);

    List<OrderReservation> findByOrderId(UUID orderId);
}
