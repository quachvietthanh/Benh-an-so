package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.role.UpdateRolePermissionsRequest;
import com.benhsoan.adapter.inbound.rest.response.role.PermissionResponse;
import com.benhsoan.adapter.inbound.rest.response.role.RolePermissionsResponse;
import com.benhsoan.port.dto.command.role.UpdateRolePermissionsCommand;
import com.benhsoan.port.dto.result.role.PermissionResult;
import com.benhsoan.port.dto.result.role.RolePermissionsResult;

@Component
public class RoleRestMapper {
    public UpdateRolePermissionsCommand toCommand(UUID roleId, UpdateRolePermissionsRequest request) {
        return new UpdateRolePermissionsCommand(roleId, request.permissionCodes());
    }

    public PermissionResponse toResponse(PermissionResult result) {
        return new PermissionResponse(result.id(), result.code(), result.name(), result.module(),
                result.description(), result.active());
    }

    public List<PermissionResponse> toPermissionResponse(List<PermissionResult> results) {
        return results.stream().map(this::toResponse).toList();
    }

    public RolePermissionsResponse toResponse(RolePermissionsResult result) {
        return new RolePermissionsResponse(result.id(), result.name(), result.description(), result.system(),
                toPermissionResponse(result.permissions()), result.updatedAt());
    }

    public List<RolePermissionsResponse> toRoleResponse(List<RolePermissionsResult> results) {
        return results.stream().map(this::toResponse).toList();
    }
}
