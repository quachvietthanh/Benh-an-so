package com.benhsoan.application.ucservice.vitalsign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@ExtendWith(MockitoExtension.class)
class VitalSignAuthorizationServiceTest {

    @Mock
    private CurrentUserPort currentUserPort;

    private VitalSignAuthorizationService authorizationService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        authorizationService = new VitalSignAuthorizationService(currentUserPort);
    }

    @Test
    @DisplayName("requireReadAccess trả về userId của người dùng hiện tại")
    void requireReadAccessReturnsCurrentUserId() {
        when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);
        assertEquals(USER_ID, authorizationService.requireReadAccess());
    }

    @Test
    @DisplayName("requireWriteAccess trả về userId của người dùng hiện tại")
    void requireWriteAccessReturnsCurrentUserId() {
        when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);
        assertEquals(USER_ID, authorizationService.requireWriteAccess());
    }

    @Test
    @DisplayName("requireVisitDoctorAccess cho phép khi currentUserId khớp với visitDoctorId")
    void requireVisitDoctorAccessPassesWhenDoctorMatches() {
        when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);
        assertDoesNotThrow(() -> authorizationService.requireVisitDoctorAccess(USER_ID));
    }

    @Test
    @DisplayName("requireVisitDoctorAccess ném MedicalRecordAccessDeniedException khi currentUserId không khớp visitDoctorId (kể cả ADMIN)")
    void requireVisitDoctorAccessThrowsWhenDoctorMismatch() {
        when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);
        assertThrows(MedicalRecordAccessDeniedException.class, () -> authorizationService.requireVisitDoctorAccess(OTHER_USER_ID));
    }
}
