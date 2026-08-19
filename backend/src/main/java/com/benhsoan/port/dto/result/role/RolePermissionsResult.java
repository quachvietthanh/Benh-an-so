package com.benhsoan.port.dto.result.role;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RolePermissionsResult(UUID id, String name, String description, boolean system,
                                    List<PermissionResult> permissions, Instant updatedAt) {}
