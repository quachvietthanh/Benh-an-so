package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@ExtendWith(MockitoExtension.class)
class MedicalRecordAuthorizationServiceTest {

    @Mock
    private CurrentUserPort currentUserPort;

    @Mock
    private PermissionEvaluator permissionEvaluator;

    @InjectMocks
    private MedicalRecordAuthorizationService service;

    @Test
    @DisplayName("audit log access is allowed when current user has AUDIT_READ")
    void allowsAuditReadPermission() {
        UUID userId = UUID.randomUUID();
        when(permissionEvaluator.hasPermission("AUDIT_READ")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);

        assertEquals(userId, service.requireAuditReadAccess());
    }

    @Test
    @DisplayName("audit log access is denied without AUDIT_READ")
    void deniesWithoutAuditReadPermission() {
        when(permissionEvaluator.hasPermission("AUDIT_READ")).thenReturn(false);

        assertThrows(MedicalRecordAccessDeniedException.class, () -> service.requireAuditReadAccess());
    }
}
