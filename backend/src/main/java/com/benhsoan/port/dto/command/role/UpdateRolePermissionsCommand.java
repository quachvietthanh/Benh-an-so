package com.benhsoan.port.dto.command.role;

import java.util.List;
import java.util.UUID;

public record UpdateRolePermissionsCommand(UUID roleId, List<String> permissionCodes) {}
