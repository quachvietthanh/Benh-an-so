package com.benhsoan.port.inbound.role;

import java.util.List;
import com.benhsoan.port.dto.result.role.RolePermissionsResult;

public interface GetSystemRolesUseCase { List<RolePermissionsResult> getSystemRoles(); }
