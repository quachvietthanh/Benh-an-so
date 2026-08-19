package com.benhsoan.application.ucservice.role;

import java.util.Comparator;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.auth.Permission;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.port.dto.result.role.PermissionResult;
import com.benhsoan.port.dto.result.role.RolePermissionsResult;

@Component
public class RolePermissionsResultMapper {
    public PermissionResult permission(Permission value) {
        return new PermissionResult(value.getId(), value.getCode(), value.getName(), value.getModule(),
                value.getDescription(), value.isActive());
    }

    public RolePermissionsResult role(Role role) {
        return new RolePermissionsResult(role.getId(), role.getName(), role.getDescription(), role.isSystem(),
                role.getPermissions().stream().map(this::permission).sorted(Comparator.comparing(PermissionResult::module)
                        .thenComparing(PermissionResult::code)).toList(), role.getUpdatedAt());
    }
}
