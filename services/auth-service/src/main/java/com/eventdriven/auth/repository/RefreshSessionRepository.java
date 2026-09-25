package com.eventdriven.auth.repository;

import com.eventdriven.auth.entity.RefreshSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {

    Optional<RefreshSession> findByTokenHash(String tokenHash);
    List<RefreshSession> findByUserId(UUID userId);

}
