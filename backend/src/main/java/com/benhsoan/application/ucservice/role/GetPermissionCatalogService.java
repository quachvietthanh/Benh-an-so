package com.benhsoan.application.ucservice.role;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.result.role.PermissionResult;
import com.benhsoan.port.inbound.role.GetPermissionCatalogUseCase;
import com.benhsoan.port.outbound.repository.auth.PermissionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPermissionCatalogService implements GetPermissionCatalogUseCase {
    private final PermissionRepository permissionRepository;
    private final RolePermissionsResultMapper mapper;

    @Override public List<PermissionResult> getPermissionCatalog() {
        return permissionRepository.findAllActive().stream().map(mapper::permission).toList();
    }
}
