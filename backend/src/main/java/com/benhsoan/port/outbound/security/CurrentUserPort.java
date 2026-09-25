package com.benhsoan.port.outbound.security;

import java.util.Set;
import java.util.UUID;

public interface CurrentUserPort {

    UUID getCurrentUserId();

    default UUID getCurrentSessionId() {
        return null;
    }

    Set<String> getCurrentUserRoles();

    boolean hasRole(String role);

    Set<String> getCurrentUserPermissions();

    boolean hasPermission(String permission);
}