package com.benhsoan.application.ucservice.vitalsign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@ExtendWith(MockitoExtension.class)
class VitalSignAuthorizationServiceTest {

    @Mock
    private CurrentUserPort currentUserPort;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private VitalSignAuthorizationAuditService authorizationAuditService;

    private VitalSignAuthorizationService authorizationService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final UUID VISIT_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        authorizationService = new VitalSignAuthorizationService(
                currentUserPort,
                visitRepository,
                authorizationAuditService
        );
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

    @Test
    @DisplayName("requireVisitReadAccess cho phép ADMIN đọc dữ liệu lượt khám của bất kỳ bác sĩ nào")
    void requireVisitReadAccessAllowsAdmin() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);

        UUID actorId = authorizationService.requireVisitReadAccess(OTHER_USER_ID, VISIT_ID);

        assertEquals(USER_ID, actorId);
        verify(authorizationAuditService, never()).recordVisitAccessDenied(any(), any(), any());
    }

    @Test
    @DisplayName("requireVisitReadAccess cho phép bác sĩ phụ trách đọc dữ liệu lượt khám của mình")
    void requireVisitReadAccessAllowsAssignedDoctor() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);

        UUID actorId = authorizationService.requireVisitReadAccess(USER_ID, VISIT_ID);

        assertEquals(USER_ID, actorId);
        verify(authorizationAuditService, never()).recordVisitAccessDenied(any(), any(), any());
    }

    @Test
    @DisplayName("requireVisitReadAccess ném 403 và ghi log ACCESS_DENIED khi Doctor A đọc lượt khám của Doctor B")
    void requireVisitReadAccessDeniesDoctorMismatch() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);

        assertThrows(MedicalRecordAccessDeniedException.class,
                () -> authorizationService.requireVisitReadAccess(OTHER_USER_ID, VISIT_ID));

        verify(authorizationAuditService).recordVisitAccessDenied(
                eq(USER_ID),
                eq(VISIT_ID),
                eq("Bác sĩ chỉ có quyền xem chỉ số sinh tồn của lượt khám do mình phụ trách.")
        );
    }

    @Test
    @DisplayName("requirePatientHistoryReadAccess cho phép ADMIN đọc lịch sử bất kỳ bệnh nhân nào")
    void requirePatientHistoryReadAccessAllowsAdmin() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);

        UUID actorId = authorizationService.requirePatientHistoryReadAccess(PATIENT_ID);

        assertEquals(USER_ID, actorId);
        verify(authorizationAuditService, never()).recordPatientAccessDenied(any(), any(), any());
    }

    @Test
    @DisplayName("requirePatientHistoryReadAccess cho phép bác sĩ khi đã/đang phụ trách ít nhất một lượt khám của bệnh nhân")
    void requirePatientHistoryReadAccessAllowsAttendingDoctor() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);

        Visit visit = org.mockito.Mockito.mock(Visit.class);
        when(visit.getDoctorId()).thenReturn(USER_ID);
        when(visitRepository.findByPatientIdOrderByVisitAtDesc(PATIENT_ID)).thenReturn(List.of(visit));

        UUID actorId = authorizationService.requirePatientHistoryReadAccess(PATIENT_ID);

        assertEquals(USER_ID, actorId);
        verify(authorizationAuditService, never()).recordPatientAccessDenied(any(), any(), any());
    }

    @Test
    @DisplayName("requirePatientHistoryReadAccess ném 403 và ghi log ACCESS_DENIED khi Doctor A xem lịch sử bệnh nhân chỉ thuộc Doctor B")
    void requirePatientHistoryReadAccessDeniesUnassignedDoctor() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);

        Visit visitOfDoctorB = org.mockito.Mockito.mock(Visit.class);
        when(visitOfDoctorB.getDoctorId()).thenReturn(OTHER_USER_ID);
        when(visitRepository.findByPatientIdOrderByVisitAtDesc(PATIENT_ID)).thenReturn(List.of(visitOfDoctorB));

        assertThrows(MedicalRecordAccessDeniedException.class,
                () -> authorizationService.requirePatientHistoryReadAccess(PATIENT_ID));

        verify(authorizationAuditService).recordPatientAccessDenied(
                eq(USER_ID),
                eq(PATIENT_ID),
                eq("Bác sĩ không có quyền xem lịch sử chỉ số sinh tồn của bệnh nhân không do mình phụ trách.")
        );
    }
}
