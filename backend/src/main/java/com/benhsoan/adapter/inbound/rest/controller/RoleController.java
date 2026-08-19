package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.RoleRestMapper;
import com.benhsoan.adapter.inbound.rest.request.role.UpdateRolePermissionsRequest;
import com.benhsoan.adapter.inbound.rest.response.role.PermissionResponse;
import com.benhsoan.adapter.inbound.rest.response.role.RolePermissionsResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.inbound.role.GetPermissionCatalogUseCase;
import com.benhsoan.port.inbound.role.GetSystemRolesUseCase;
import com.benhsoan.port.inbound.role.UpdateRolePermissionsUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class RoleController {
    private final GetSystemRolesUseCase getSystemRolesUseCase;
    private final GetPermissionCatalogUseCase getPermissionCatalogUseCase;
    private final UpdateRolePermissionsUseCase updateRolePermissionsUseCase;
    private final RoleRestMapper mapper;

    @GetMapping("/roles")
    @RequirePermission("ROLE_READ")
    public List<RolePermissionsResponse> getSystemRoles() {
        return mapper.toRoleResponse(getSystemRolesUseCase.getSystemRoles());
    }

    @GetMapping("/permissions")
    @RequirePermission("PERMISSION_READ")
    public List<PermissionResponse> getPermissionCatalog() {
        return mapper.toPermissionResponse(getPermissionCatalogUseCase.getPermissionCatalog());
    }

    @PutMapping("/roles/{roleId}/permissions")
    @RequirePermission("ROLE_UPDATE")
    public RolePermissionsResponse updateRolePermissions(@PathVariable UUID roleId,
            @Valid @RequestBody UpdateRolePermissionsRequest request) {
        return mapper.toResponse(updateRolePermissionsUseCase.updateRolePermissions(mapper.toCommand(roleId, request)));
    }
}
