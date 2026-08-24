package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordAmendment;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.result.MedicalRecordVersionHistoryResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordAmendmentRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class GetMedicalRecordVersionHistoryServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-21T00:00:00Z");
    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID VISIT_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID CREATOR_ID = UUID.randomUUID();
    private static final UUID AMENDER_ONE_ID = UUID.randomUUID();
    private static final UUID AMENDER_TWO_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();

    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    @Mock
    private MedicalRecordAmendmentRepository amendmentRepository;
    @Mock
    private VisitRepository visitRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MedicalRecordAuthorizationService authorizationService;
    @Mock
    private MedicalRecordAccessAuditService accessAuditService;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private CurrentUserPort currentUserPort;
    @Mock
    private ClockPort clockPort;

    private GetMedicalRecordVersionHistoryService service() {
        return new GetMedicalRecordVersionHistoryService(
                medicalRecordRepository, amendmentRepository, visitRepository, userRepository,
                authorizationService, accessAuditService,
                new MedicalRecordVersionHistoryAuditWriter(auditLogRepository),
                currentUserPort, clockPort);
    }

    @Test
    void returnsVersionsInChronologicalOrderWhenRecordHasAmendments() {
        stubAuthorizedRecord();

        MedicalRecordAmendment newer = MedicalRecordAmendment.restore(
                UUID.randomUUID(), RECORD_ID, "Updated conclusion", "Corrected diagnosis", AMENDER_ONE_ID, NOW);
        MedicalRecordAmendment older = MedicalRecordAmendment.restore(
                UUID.randomUUID(), RECORD_ID, "Added symptoms", "Missed detail", AMENDER_TWO_ID, NOW.minusSeconds(60));

        when(amendmentRepository.findByMedicalRecordId(RECORD_ID)).thenReturn(List.of(newer, older));
        when(userRepository.findAllById(any())).thenReturn(List.of(
                user(CREATOR_ID, "Dr. Creator"), user(AMENDER_ONE_ID, "Dr. One"), user(AMENDER_TWO_ID, "Dr. Two")));

        MedicalRecordVersionHistoryResult result = service().getVersionHistory(RECORD_ID);

        assertFalse(result.originalOnly());
        assertEquals(1, result.originalVersion().versionNumber());
        assertEquals("Dr. Creator", result.originalVersion().modifiedBy());
        assertEquals(2, result.amendments().size());

        assertEquals(2, result.amendments().get(0).versionNumber());
        assertEquals("Dr. Two", result.amendments().get(0).modifiedBy());
        assertEquals("Added symptoms", result.amendments().get(0).content());
        assertEquals("Missed detail", result.amendments().get(0).reason());

        assertEquals(3, result.amendments().get(1).versionNumber());
        assertEquals("Dr. One", result.amendments().get(1).modifiedBy());
        assertEquals("Updated conclusion", result.amendments().get(1).content());

        verify(accessAuditService).recordRecordAccess(PATIENT_ID, VISIT_ID, RECORD_ID, CREATOR_ID,
                MedicalRecordAccessAction.VIEW, "Medical record version history viewed", NOW);
    }

    @Test
    void returnsOnlyOriginalVersionWhenRecordHasNoAmendments() {
        stubAuthorizedRecord();
        when(amendmentRepository.findByMedicalRecordId(RECORD_ID)).thenReturn(List.of());
        when(userRepository.findAllById(any())).thenReturn(List.of(user(CREATOR_ID, "Dr. Creator")));

        MedicalRecordVersionHistoryResult result = service().getVersionHistory(RECORD_ID);

        assertTrue(result.originalOnly());
        assertTrue(result.amendments().isEmpty());
        assertEquals(1, result.originalVersion().versionNumber());
        assertEquals("Dr. Creator", result.originalVersion().modifiedBy());
        assertNull(result.originalVersion().reason());
        assertNull(result.originalVersion().content());
    }

    @Test
    void rejectsUnauthorizedAccessAndWritesDenialAudit() {
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record()));
        when(authorizationService.requireReadAccess()).thenThrow(new MedicalRecordAccessDeniedException());
        when(currentUserPort.getCurrentUserId()).thenReturn(CREATOR_ID);
        when(clockPort.now()).thenReturn(NOW);

        assertThrows(MedicalRecordAccessDeniedException.class, () -> service().getVersionHistory(RECORD_ID));

        verify(accessAuditService, never()).recordRecordAccess(any(), any(), any(), any(), any(), any(), any());
        verify(auditLogRepository).save(org.mockito.ArgumentMatchers.argThat(audit ->
                audit.getActionType() == ActionType.ACCESS_DENIED
                        && audit.getResourceType() == ResourceType.MEDICAL_RECORD
                        && RECORD_ID.equals(audit.getResourceId())));
    }

    @Test
    void recordsAccessLogOnSuccessfulRetrieval() {
        stubAuthorizedRecord();
        when(amendmentRepository.findByMedicalRecordId(RECORD_ID)).thenReturn(List.of());
        when(userRepository.findAllById(any())).thenReturn(List.of(user(CREATOR_ID, "Dr. Creator")));

        service().getVersionHistory(RECORD_ID);

        verify(accessAuditService).recordRecordAccess(PATIENT_ID, VISIT_ID, RECORD_ID, CREATOR_ID,
                MedicalRecordAccessAction.VIEW, "Medical record version history viewed", NOW);
    }

    private void stubAuthorizedRecord() {
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record()));
        when(authorizationService.requireReadAccess()).thenReturn(CREATOR_ID);
        when(clockPort.now()).thenReturn(NOW);
        Visit visit = Visit.restore(VISIT_ID, "V001", PATIENT_ID, CREATOR_ID, null, null, VisitType.WALK_IN,
                VisitStatus.COMPLETED, NOW.minusSeconds(3600), NOW.minusSeconds(1800), NOW,
                "Checkup", null, CREATOR_ID, NOW.minusSeconds(3600), NOW);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));
    }

    private MedicalRecord record() {
        return MedicalRecord.restore(RECORD_ID, VISIT_ID, "c", "s", "h", "p", "cp", "tp", "di", "co",
                MedicalRecordStatus.LOCKED, NOW, CREATOR_ID, CREATOR_ID, NOW, null, null);
    }

    private User user(UUID id, String fullName) {
        return User.restore(id, "u-" + id, "hash", fullName, id + "@benhsoan.com", "0900000000", ROLE_ID,
                true, null, NOW);
    }
}
