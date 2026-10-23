package com.trustvault.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trustvault.entity.AppUser;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    boolean existsByUsername(String username);

    Optional<AppUser> findByUsername(String username);
}