package com.benhsoan.adapter.inbound.rest.response.role;

import java.util.UUID;

public record PermissionResponse(UUID id, String code, String name, String module, String description, boolean active) {}
