package com.benhsoan.port.dto.result.role;

import java.util.UUID;

public record PermissionResult(UUID id, String code, String name, String module, String description, boolean active) {}
