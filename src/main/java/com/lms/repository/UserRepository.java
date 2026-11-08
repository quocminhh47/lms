package com.lms.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lms.entity.User;
import com.lms.entity.UserRole;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

	long countByRoleAndActive(UserRole role, boolean active);

	long countByActive(boolean active);

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	Optional<User> findByPhone(String phone);

	boolean existsByPhone(String phone);
}
