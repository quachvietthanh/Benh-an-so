package com.benhsoan.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@ExtendWith(MockitoExtension.class)
class RequirePermissionAspectTest {
    @Mock AuditLogRepository auditLogRepository;
    @Mock CurrentUserPort currentUserPort;
    @Mock ProceedingJoinPoint joinPoint;
    @Mock RequirePermission requirement;
    @Captor ArgumentCaptor<AuditLog> auditCaptor;

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); RequestContextHolder.resetRequestAttributes(); }

    @Test void permitsMatchingPermissionAuthority() throws Throwable {
        authenticate("PERMISSION_ROLE_READ");
        when(requirement.value()).thenReturn(new String[] { "ROLE_READ" });
        when(requirement.operator()).thenReturn(RequirePermission.Operator.ANY);
        when(joinPoint.proceed()).thenReturn("ok");
        RequirePermissionAspect aspect = aspect();
        assertEquals("ok", aspect.requirePermission(joinPoint, requirement));
    }

    @Test void deniesAndAuditsMissingPermission() {
        UUID actorId = UUID.randomUUID();
        authenticate("PERMISSION_PATIENT_READ");
        when(requirement.value()).thenReturn(new String[] { "ROLE_READ" });
        when(requirement.operator()).thenReturn(RequirePermission.Operator.ANY);
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/roles");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThrows(AccessDeniedException.class, () -> aspect().requirePermission(joinPoint, requirement));

        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertEquals(ActionType.ACCESS_DENIED, audit.getActionType());
        assertEquals(ResourceType.PERMISSION, audit.getResourceType());
        assertEquals(actorId, audit.getUserId());
        org.junit.jupiter.api.Assertions.assertTrue(audit.getDetail().contains("ROLE_READ"));
        org.junit.jupiter.api.Assertions.assertTrue(audit.getDetail().contains("GET"));
        org.junit.jupiter.api.Assertions.assertTrue(audit.getDetail().contains("/roles"));
    }

    private RequirePermissionAspect aspect() {
        return new RequirePermissionAspect(new PermissionEvaluator(), auditLogRepository, currentUserPort);
    }

    private void authenticate(String authority) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority(authority))));
    }
}
