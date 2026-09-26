package com.benhsoan.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class MedicalRecordTemplatePermissionAuditTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private RequirePermission requirement;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void deniesTemplateManagementAndAuditsTheRequiredPermission() {
        UUID actorId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "doctor", null, List.of(new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_READ"))));
        when(requirement.value()).thenReturn(new String[] { "MEDICAL_RECORD_TEMPLATE_MANAGE" });
        when(requirement.operator()).thenReturn(RequirePermission.Operator.ANY);
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("POST", "/system/medical-record-templates")));

        RequirePermissionAspect aspect = new RequirePermissionAspect(new PermissionEvaluator(), auditLogRepository, currentUserPort);
        assertThrows(AccessDeniedException.class, () -> aspect.requirePermission(joinPoint, requirement));

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertTrue(audit.getDetail().contains("MEDICAL_RECORD_TEMPLATE_MANAGE"));
        assertTrue(audit.getDetail().contains("/system/medical-record-templates"));
        org.junit.jupiter.api.Assertions.assertEquals(ActionType.ACCESS_DENIED, audit.getActionType());
        org.junit.jupiter.api.Assertions.assertEquals(ResourceType.PERMISSION, audit.getResourceType());
    }
}
