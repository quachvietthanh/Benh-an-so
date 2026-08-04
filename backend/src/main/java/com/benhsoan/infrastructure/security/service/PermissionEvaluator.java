package com.benhsoan.infrastructure.security.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.enums.Permission;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PermissionEvaluator {

    private static final String ROLE_PREFIX = "ROLE_";

    private final RoleRepository roleRepository;

    public Set<Permission> getCurrentUserPermissions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Set.of();
        }

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .map(roleRepository::findByName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Role::getPermissions)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    public List<String> getCurrentUserRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return List.of();
        }

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .collect(Collectors.toList());
    }

    public boolean hasPermission(Permission permission) {
        return getCurrentUserPermissions().contains(permission);
    }

    public boolean hasAnyPermission(Permission... permissions) {
        Set<Permission> userPermissions = getCurrentUserPermissions();
        return Arrays.stream(permissions).anyMatch(userPermissions::contains);
    }

    public boolean hasAllPermissions(Permission... permissions) {
        Set<Permission> userPermissions = getCurrentUserPermissions();
        return userPermissions.containsAll(Set.of(permissions));
    }

    public boolean hasRole(String role) {
        return getCurrentUserRoles().contains(role);
    }

    public boolean hasAnyRole(String... roles) {
        List<String> userRoles = getCurrentUserRoles();
        return Arrays.stream(roles).anyMatch(userRoles::contains);
    }
}
