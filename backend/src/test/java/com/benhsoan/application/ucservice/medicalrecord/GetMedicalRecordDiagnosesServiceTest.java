package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;
import com.benhsoan.domain.medicalrecord.enums.DiagnosisType;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class GetMedicalRecordDiagnosesServiceTest {

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private MedicalRecordAuthorizationService authorizationService;
    @Mock private MedicalRecordAccessAuditService accessAuditService;
    @Mock private ClockPort clockPort;
    @Spy private MedicalRecordDiagnosisResultMapper resultMapper = new MedicalRecordDiagnosisResultMapper();
    @InjectMocks private GetMedicalRecordDiagnosesService service;

    @Test
    void returnsPersistedDiagnosesAndAuditsTheRead() {
        UUID actorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T02:00:00Z");
        MedicalRecord record = MedicalRecord.create(visitId, null, null, null, null, null, null, null, null, actorId, now);
        Visit visit = Visit.restore(visitId, "VIS-001", patientId, UUID.randomUUID(), null, null, VisitType.WALK_IN,
                VisitStatus.IN_PROGRESS, now, now, null, "Exam", null, actorId, now, null);
        UUID catalogId = UUID.randomUUID();
        MedicalRecordDiagnosis diagnosis = MedicalRecordDiagnosis.create(record.getId(), catalogId, "J06.9",
                "Upper respiratory infection", DiagnosisType.PRIMARY, null, actorId, now);
        when(authorizationService.requireReadAccess()).thenReturn(actorId);
        when(medicalRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(record.getId())).thenReturn(List.of(diagnosis));
        when(clockPort.now()).thenReturn(now);

        var result = service.getByMedicalRecordId(record.getId());

        assertEquals(List.of("J06.9"), result.stream().map(item -> item.diagnosisCode()).toList());
        assertEquals(catalogId, result.getFirst().diagnosisCatalogId());
        assertEquals(DiagnosisType.PRIMARY, result.getFirst().diagnosisType());
        verify(accessAuditService).recordRecordView(patientId, visitId, record.getId(), actorId, now);
    }

    @Test
    void returnsFilteredDiagnosesWhenTypeIsProvided() {
        UUID actorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID secondaryCatalogId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T02:00:00Z");
        MedicalRecord record = MedicalRecord.create(visitId, null, null, null, null, null, null, null, null, actorId, now);
        Visit visit = Visit.restore(visitId, "VIS-001", patientId, UUID.randomUUID(), null, null, VisitType.WALK_IN,
                VisitStatus.IN_PROGRESS, now, now, null, "Exam", null, actorId, now, null);
        MedicalRecordDiagnosis secondary = MedicalRecordDiagnosis.create(record.getId(), secondaryCatalogId, "R50.9",
                "Fever", DiagnosisType.SECONDARY, "Comorbidity", actorId, now);
        when(authorizationService.requireReadAccess()).thenReturn(actorId);
        when(medicalRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(medicalRecordDiagnosisRepository.findByMedicalRecordIdAndDiagnosisType(record.getId(), DiagnosisType.SECONDARY))
                .thenReturn(List.of(secondary));
        when(clockPort.now()).thenReturn(now);

        var result = service.getByMedicalRecordId(record.getId(), DiagnosisType.SECONDARY);

        assertEquals(1, result.size());
        assertEquals(secondaryCatalogId, result.getFirst().diagnosisCatalogId());
        assertEquals(DiagnosisType.SECONDARY, result.getFirst().diagnosisType());
        assertEquals("R50.9", result.getFirst().diagnosisCode());
        verify(medicalRecordDiagnosisRepository).findByMedicalRecordIdAndDiagnosisType(record.getId(), DiagnosisType.SECONDARY);
        verify(accessAuditService).recordRecordView(patientId, visitId, record.getId(), actorId, now);
    }

    @Test
    void returnsDeterministicallyOrderedDiagnosesWhenDiagnosedAtIsIdentical() {
        UUID actorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T02:00:00Z");
        MedicalRecord record = MedicalRecord.create(visitId, null, null, null, null, null, null, null, null, actorId, now);
        Visit visit = Visit.restore(visitId, "VIS-001", patientId, UUID.randomUUID(), null, null, VisitType.WALK_IN,
                VisitStatus.IN_PROGRESS, now, now, null, "Exam", null, actorId, now, null);

        UUID secondaryId1 = UUID.randomUUID();
        UUID secondaryId2 = UUID.randomUUID();
        MedicalRecordDiagnosis secondary1 = MedicalRecordDiagnosis.create(record.getId(), secondaryId1, "E11",
                "Diabetes", DiagnosisType.SECONDARY, null, actorId, now);
        MedicalRecordDiagnosis secondary2 = MedicalRecordDiagnosis.create(record.getId(), secondaryId2, "I10",
                "Hypertension", DiagnosisType.SECONDARY, null, actorId, now);

        when(authorizationService.requireReadAccess()).thenReturn(actorId);
        when(medicalRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(medicalRecordDiagnosisRepository.findByMedicalRecordIdAndDiagnosisType(record.getId(), DiagnosisType.SECONDARY))
                .thenReturn(List.of(secondary1, secondary2));
        when(clockPort.now()).thenReturn(now);

        var result = service.getByMedicalRecordId(record.getId(), DiagnosisType.SECONDARY);

        assertEquals(2, result.size());
        assertEquals("E11", result.get(0).diagnosisCode());
        assertEquals("I10", result.get(1).diagnosisCode());
    }
}
