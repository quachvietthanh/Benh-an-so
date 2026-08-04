package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordAmendment;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAmendmentRequiresCompletedVisitException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.command.medicalrecord.AmendMedicalRecordCommand;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordAmendmentRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class AmendMedicalRecordServiceTest {

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private MedicalRecordAmendmentRepository amendmentRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private MedicalRecordAuthorizationService authorizationService;
    @Mock private MedicalRecordAccessAuditService accessAuditService;
    @Mock private ClockPort clockPort;
    @Spy private MedicalRecordResultMapper resultMapper = new MedicalRecordResultMapper();
    @InjectMocks private AmendMedicalRecordService service;

    @Test
    void amendsLockedRecordAndWritesAmendAudit() {
        UUID recordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T02:00:00Z");
        MedicalRecord record = MedicalRecord.create(visitId, "Headache", null, null, null, null, null, null,
                "Stable", userId, now);
        record.lock(userId, now);
        record = MedicalRecord.restore(recordId, record.getVisitId(), record.getChiefComplaint(), record.getSymptoms(),
                record.getMedicalHistory(), record.getPhysicalExamination(), record.getClinicalProgress(), record.getTreatmentPlan(),
                record.getDoctorInstructions(), record.getConclusion(), record.getStatus(), record.getLockedAt(), record.getLockedBy(),
                record.getCreatedBy(), record.getCreatedAt(), record.getUpdatedBy(), record.getUpdatedAt());
        Visit visit = Visit.restore(visitId, "VIS-001", patientId, UUID.randomUUID(), null, null,
                VisitType.WALK_IN, VisitStatus.COMPLETED, now, now, now, "Consultation", null, userId, now, now);
        when(authorizationService.requireWriteAccess()).thenReturn(userId);
        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(clockPort.now()).thenReturn(now);
        when(amendmentRepository.save(any(MedicalRecordAmendment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.amend(recordId, new AmendMedicalRecordCommand("Correction", "Clarification"));

        assertEquals(recordId, result.medicalRecordId());
        verify(accessAuditService).recordRecordAccess(patientId, visitId, recordId, userId,
                MedicalRecordAccessAction.AMEND, "Medical record amended", now);
    }

    @Test
    void rejectsAmendmentForCancelledVisit() {
        UUID recordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T02:00:00Z");
        MedicalRecord draft = MedicalRecord.create(visitId, "Headache", null, null, null, null, null, null,
                "Stable", userId, now);
        draft.lock(userId, now);
        MedicalRecord record = MedicalRecord.restore(recordId, draft.getVisitId(), draft.getChiefComplaint(),
                draft.getSymptoms(), draft.getMedicalHistory(), draft.getPhysicalExamination(),
                draft.getClinicalProgress(), draft.getTreatmentPlan(), draft.getDoctorInstructions(),
                draft.getConclusion(), draft.getStatus(), draft.getLockedAt(), draft.getLockedBy(),
                draft.getCreatedBy(), draft.getCreatedAt(), draft.getUpdatedBy(), draft.getUpdatedAt());
        Visit visit = Visit.restore(visitId, "VIS-001", UUID.randomUUID(), UUID.randomUUID(), null, null,
                VisitType.WALK_IN, VisitStatus.CANCELLED, now, null, null, "Consultation", null, userId, now, now);
        when(authorizationService.requireWriteAccess()).thenReturn(userId);
        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        assertThrows(MedicalRecordAmendmentRequiresCompletedVisitException.class,
                () -> service.amend(recordId, new AmendMedicalRecordCommand("Correction", "Clarification")));

        verifyNoInteractions(amendmentRepository, accessAuditService);
    }
}
