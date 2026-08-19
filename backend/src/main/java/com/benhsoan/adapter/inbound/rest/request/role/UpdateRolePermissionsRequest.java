package com.benhsoan.adapter.inbound.rest.request.role;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateRolePermissionsRequest(
        @NotNull List<@NotBlank String> permissionCodes
) {}
