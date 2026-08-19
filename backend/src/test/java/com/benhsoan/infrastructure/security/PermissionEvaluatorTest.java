package com.benhsoan.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.benhsoan.infrastructure.security.service.PermissionEvaluator;

@DisplayName("PermissionEvaluator JWT authority tests")
class PermissionEvaluatorTest {

    private final PermissionEvaluator evaluator = new PermissionEvaluator();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void grantsPermissionFromTokenAuthority() {
        authenticate("PERMISSION_REPORT_EXPORT");

        assertTrue(evaluator.hasPermission("REPORT_EXPORT"));
        assertFalse(evaluator.hasPermission("REPORT_VIEW"));
    }

    @Test
    void evaluatesAnyAndAllPermissionsFromTokenAuthorities() {
        authenticate("PERMISSION_ROLE_READ", "PERMISSION_ROLE_UPDATE");

        assertTrue(evaluator.hasAnyPermission("ROLE_READ", "PERMISSION_READ"));
        assertTrue(evaluator.hasAllPermissions("ROLE_READ", "ROLE_UPDATE"));
        assertFalse(evaluator.hasAllPermissions("ROLE_READ", "PERMISSION_READ"));
    }

    @Test
    void deniesWhenUnauthenticatedOrOnlyRoleAuthorityIsPresent() {
        assertFalse(evaluator.hasPermission("ROLE_READ"));

        authenticate("ROLE_ADMIN");
        assertFalse(evaluator.hasPermission("ROLE_READ"));
    }

    private void authenticate(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "user", null, List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList()));
    }
}
