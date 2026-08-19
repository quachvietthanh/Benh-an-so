package com.benhsoan.adapter.inbound.rest.response.role;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RolePermissionsResponse(UUID id, String name, String description, boolean system,
                                      List<PermissionResponse> permissions, Instant updatedAt) {}
