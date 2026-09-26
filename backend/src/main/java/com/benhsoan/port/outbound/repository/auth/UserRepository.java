package com.benhsoan.port.outbound.repository.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.auth.User;

public interface UserRepository {
    Optional<User> findById(UUID id);

    Optional<User> findByIdForUpdate(UUID id);

    User save(User user);

    List<User> findAll();

    Optional<User> findByUsername(String username);

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    List<User> findAllById(List<UUID> ids);

    List<User> findAllActiveByRoleId(UUID roleId);

    long countActiveByRoleId(UUID roleId);
}
