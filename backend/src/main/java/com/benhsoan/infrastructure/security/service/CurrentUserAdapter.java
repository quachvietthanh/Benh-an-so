package com.benhsoan.infrastructure.security.service;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.benhsoan.infrastructure.authSecurity.CurrentUserPrincipal;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@Component
public class CurrentUserAdapter implements CurrentUserPort {

    @Override
    public UUID getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal())) {

            throw new IllegalStateException(
                    "No authenticated user found."
            );
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CurrentUserPrincipal currentUser)) {
            throw new IllegalStateException(
                    "Invalid authentication principal."
            );
        }

        return currentUser.userId();
    }

    @Override
    public Set<String> getCurrentUserRoles() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal())) {

            return Collections.emptySet();
        }

        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean hasRole(String role) {
        return getCurrentUserRoles().contains(role);
    }
}
