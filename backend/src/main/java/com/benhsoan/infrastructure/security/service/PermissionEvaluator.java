package com.benhsoan.infrastructure.security.service;

import java.util.Arrays;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class PermissionEvaluator {

    public boolean hasPermission(String permissionCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("PERMISSION_" + permissionCode));
    }

    public boolean hasAnyPermission(String... permissionCodes) {
        return Arrays.stream(permissionCodes).anyMatch(this::hasPermission);
    }

    public boolean hasAllPermissions(String... permissionCodes) {
        return Arrays.stream(permissionCodes).allMatch(this::hasPermission);
    }

}
