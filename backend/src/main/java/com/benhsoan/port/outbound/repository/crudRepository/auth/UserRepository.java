package com.benhsoan.port.outbound.repository.crudRepository.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.auth.User;
import com.benhsoan.port.outbound.repository.BaseRepository;

public interface UserRepository extends BaseRepository<User, UUID> {
    List<User> findAll();

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findAllById(List<UUID> ids);

    List<User> findAllActiveByRoleId(UUID roleId);
}
