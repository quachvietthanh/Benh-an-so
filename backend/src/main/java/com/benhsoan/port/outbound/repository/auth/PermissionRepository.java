package com.benhsoan.port.outbound.repository.auth;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.benhsoan.domain.auth.Permission;

public interface PermissionRepository {
    Optional<Permission> findByCode(String code);
    List<Permission> findAllByCodes(Collection<String> codes);
    List<Permission> findAllActive();
}
