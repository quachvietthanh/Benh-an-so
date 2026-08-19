package com.benhsoan.port.inbound.role;

import com.benhsoan.port.dto.command.role.UpdateRolePermissionsCommand;
import com.benhsoan.port.dto.result.role.RolePermissionsResult;

public interface UpdateRolePermissionsUseCase { RolePermissionsResult updateRolePermissions(UpdateRolePermissionsCommand command); }
