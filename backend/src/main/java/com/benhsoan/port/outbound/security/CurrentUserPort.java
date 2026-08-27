package com.benhsoan.port.outbound.security;

import java.util.Set;
import java.util.UUID;

public interface CurrentUserPort {

    UUID getCurrentUserId();

    Set<String> getCurrentUserRoles();
    
    boolean hasRole(String role);

    Set<String> getCurrentUserPermissions();

    boolean hasPermission(String permission);
}