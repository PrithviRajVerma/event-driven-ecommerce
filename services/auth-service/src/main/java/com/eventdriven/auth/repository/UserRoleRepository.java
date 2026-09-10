package com.eventdriven.auth.repository;

import com.eventdriven.auth.entity.User;
import com.eventdriven.auth.entity.UserRole;
import com.eventdriven.auth.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByUserId(UUID userId);

}
