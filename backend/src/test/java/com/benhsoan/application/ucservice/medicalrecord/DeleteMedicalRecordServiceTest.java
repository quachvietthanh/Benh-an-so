package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordRetentionException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class DeleteMedicalRecordServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-21T00:00:00Z");
    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID VISIT_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();

    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    @Mock
    private VisitRepository visitRepository;
    @Mock
    private MedicalRecordDeletionAuditWriter auditWriter;
    @Mock
    private CurrentUserPort currentUserPort;
    @Mock
    private ClockPort clockPort;
    @Mock
    private ClinicConfigurationRepository clinicConfigurationRepository;

    private DeleteMedicalRecordService service() {
        MedicalRecordRetentionPolicy policy = new MedicalRecordRetentionPolicy(clinicConfigurationRepository);
        return new DeleteMedicalRecordService(
                medicalRecordRepository, visitRepository, auditWriter,
                currentUserPort, clockPort, policy);
    }

    private MedicalRecord record() {
        return MedicalRecord.restore(RECORD_ID, VISIT_ID, "c", "s", "h", "p", "cp", "tp", "di", "co",
                MedicalRecordStatus.LOCKED, NOW, DOCTOR_ID, DOCTOR_ID, NOW, null, null);
    }

    private Visit visitCompletedAt(Instant completedAt) {
        return Visit.restore(VISIT_ID, "V001", PATIENT_ID, DOCTOR_ID, null, null, VisitType.WALK_IN,
                VisitStatus.COMPLETED,
                completedAt.minusSeconds(3600),
                completedAt.minusSeconds(1800),
                completedAt,
                "Checkup", null, DOCTOR_ID, completedAt.minusSeconds(3600), completedAt);
    }

    @Test
    void blocksDeletionWithinRetentionAndWritesDenialAudit() {
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR);
        when(clinicConfigurationRepository.find()).thenReturn(Optional.empty());
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record()));
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visitCompletedAt(NOW)));

        assertThrows(MedicalRecordRetentionException.class, () -> service().delete(RECORD_ID));

        verify(medicalRecordRepository, never()).deleteById(RECORD_ID);
        verify(auditWriter).writeDenied(ACTOR, RECORD_ID, NOW);
    }

    @Test
    void deletesRecordPastRetentionAndWritesDeleteAudit() {
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR);
        when(clinicConfigurationRepository.find()).thenReturn(Optional.empty());
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record()));
        when(visitRepository.findById(VISIT_ID))
                .thenReturn(Optional.of(visitCompletedAt(NOW.minusSeconds(11L * 365 * 24 * 3600))));

        service().delete(RECORD_ID);

        verify(medicalRecordRepository).deleteById(RECORD_ID);
        verify(auditWriter).writeDeleted(ACTOR, RECORD_ID, NOW);
    }
}
