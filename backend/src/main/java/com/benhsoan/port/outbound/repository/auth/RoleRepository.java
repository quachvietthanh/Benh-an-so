package com.benhsoan.port.outbound.repository.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.auth.Role;
public interface RoleRepository {

    Optional<Role> findById(UUID id);

    Role save(Role role);

    Optional<Role> findByName(String name);

    List<Role> findAll();

    boolean existsByName(String name);
}
