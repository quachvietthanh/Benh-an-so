package com.benhsoan.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.benhsoan.infrastructure.authSecurity.CurrentUserPrincipal;
import com.benhsoan.infrastructure.security.service.CurrentUserAdapter;

class CurrentUserAdapterTest {

    private final CurrentUserAdapter adapter = new CurrentUserAdapter();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsOnlyRolesWhenAuthenticationAlsoContainsPermissions() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUserPrincipal(UUID.randomUUID(), "doctor"),
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_DOCTOR"),
                        new SimpleGrantedAuthority("PERMISSION_PRESCRIPTION_PRINT")
                )
        ));

        assertThat(adapter.getCurrentUserRoles()).containsExactly("DOCTOR");
        assertThat(adapter.hasRole("DOCTOR")).isTrue();
    }

    @Test
    void returnsEmptyRolesWhenThereIsNoAuthentication() {
        assertThat(adapter.getCurrentUserRoles()).isEmpty();
    }
}
