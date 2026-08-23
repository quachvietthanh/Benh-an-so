package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordMissingAuthorizationException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotSignedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordUnauthorizedRecipientException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.command.medicalrecord.IssueMedicalRecordCopyCommand;
import com.benhsoan.port.dto.command.medicalrecord.MedicalRecordCopyRecipientType;
import com.benhsoan.port.dto.result.MedicalRecordCopyResult;
import com.benhsoan.port.outbound.pdf.MedicalRecordCopyPdfRenderer;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class IssueMedicalRecordCopyServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-21T00:00:00Z");
    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID VISIT_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID ISSUER = UUID.randomUUID();
    private static final UUID DOCTOR_ROLE = UUID.randomUUID();

    private static final String PATIENT_NAME = "Nguyen Van A";
    private static final String PATIENT_IDENTITY = "012345678";

    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    @Mock
    private MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;
    @Mock
    private VisitRepository visitRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ClinicConfigurationRepository clinicConfigurationRepository;
    @Mock
    private MedicalRecordCopyPdfRenderer pdfRenderer;
    @Mock
    private CurrentUserPort currentUserPort;
    @Mock
    private ClockPort clockPort;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private MedicalRecordAccessAuditService accessAuditService;

    private IssueMedicalRecordCopyService service() {
        return new IssueMedicalRecordCopyService(
                medicalRecordRepository, medicalRecordDiagnosisRepository, visitRepository,
                patientRepository, userRepository, clinicConfigurationRepository,
                pdfRenderer, currentUserPort, clockPort, auditLogRepository,
                new MedicalRecordCopyAuditWriter(auditLogRepository), accessAuditService);
    }

    private IssueMedicalRecordCopyCommand command() {
        return new IssueMedicalRecordCopyCommand(
                RECORD_ID, MedicalRecordCopyRecipientType.PATIENT,
                PATIENT_NAME, PATIENT_IDENTITY, "Xin ban sao ho so", null);
    }

    private IssueMedicalRecordCopyCommand representativeCommand(String authorizationDocumentNumber) {
        return new IssueMedicalRecordCopyCommand(
                RECORD_ID, MedicalRecordCopyRecipientType.AUTHORIZED_REPRESENTATIVE,
                "Tran Van B", "0987654321", "Uy quyen lay ban sao ho so", authorizationDocumentNumber);
    }

    private MedicalRecord record(MedicalRecordStatus status) {
        return MedicalRecord.restore(RECORD_ID, VISIT_ID, "c", "s", "h", "p", "cp", "tp", "di", "co",
                status, NOW, DOCTOR_ID, DOCTOR_ID, NOW, null, null);
    }

    private void stubSignedContext() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ISSUER);
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record(MedicalRecordStatus.LOCKED)));

        Visit visit = Visit.restore(VISIT_ID, "V001", PATIENT_ID, DOCTOR_ID, null, null, VisitType.WALK_IN,
                VisitStatus.COMPLETED, NOW.minusSeconds(3600), NOW.minusSeconds(1800), NOW,
                "Checkup", null, DOCTOR_ID, NOW.minusSeconds(3600), NOW);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        Patient patient = Patient.restore(PATIENT_ID, "BN001", PATIENT_NAME, LocalDate.of(1990, 1, 1),
                Gender.MALE, "0900000000", null, null, PATIENT_IDENTITY, null, BloodType.UNKNOWN, null, null,
                true, NOW, null, null, DOCTOR_ID);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));

        User doctor = User.restore(DOCTOR_ID, "doctor1", "hash", "Dr. Nguyen", "doctor1@benhsoan.com",
                "0901000001", DOCTOR_ROLE, true, null, NOW);
        lenient().when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));

        ClinicConfiguration clinic = ClinicConfiguration.create("Phong kham Benh So An", "Thai Nguyen",
                "0345678910", LocalTime.of(8, 0), LocalTime.of(17, 0), NOW);
        lenient().when(clinicConfigurationRepository.find()).thenReturn(Optional.of(clinic));
        lenient().when(medicalRecordDiagnosisRepository.findByMedicalRecordId(RECORD_ID)).thenReturn(List.of());
    }

    @Test
    void issuesPdfCopyForSignedRecordAndWritesAudits() {
        stubSignedContext();
        byte[] pdf = "%PDF-1.4 copy".getBytes();
        when(pdfRenderer.render(any())).thenReturn(pdf);

        MedicalRecordCopyResult result = service().issue(command());

        assertEquals("application/pdf", result.contentType());
        assertArrayEquals(pdf, result.content());
        assertTrue(result.fileName().contains("BN001"));

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertEquals(ActionType.EXPORT, audit.getActionType());
        assertEquals(ResourceType.MEDICAL_RECORD, audit.getResourceType());
        assertEquals(RECORD_ID, audit.getResourceId());
        assertTrue(audit.getDetail().contains("PATIENT"));
        assertTrue(audit.getDetail().contains("Nguyen Van A"));

        verify(accessAuditService).recordRecordAccess(PATIENT_ID, VISIT_ID, RECORD_ID, ISSUER,
                MedicalRecordAccessAction.EXPORT, "Medical record copy issued", NOW);
    }

    @Test
    void issuesPdfCopyForAuthorizedRepresentativeWithDocument() {
        stubSignedContext();
        byte[] pdf = "%PDF-1.4 copy".getBytes();
        when(pdfRenderer.render(any())).thenReturn(pdf);

        MedicalRecordCopyResult result = service().issue(representativeCommand("UQ-2026-0001"));

        assertEquals("application/pdf", result.contentType());
        assertArrayEquals(pdf, result.content());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertEquals(ActionType.EXPORT, auditCaptor.getValue().getActionType());
        assertTrue(auditCaptor.getValue().getDetail().contains("AUTHORIZED_REPRESENTATIVE"));
    }

    @Test
    void rejectsPatientWithMismatchedIdentityAndWritesDenialAudit() {
        stubSignedContext();

        IssueMedicalRecordCopyCommand mismatched = new IssueMedicalRecordCopyCommand(
                RECORD_ID, MedicalRecordCopyRecipientType.PATIENT,
                "Tran Van X", "999999999", "Xin ban sao", null);

        assertThrows(MedicalRecordUnauthorizedRecipientException.class,
                () -> service().issue(mismatched));

        verify(pdfRenderer, never()).render(any());
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertEquals(ActionType.ACCESS_DENIED, audit.getActionType());
        assertEquals(ResourceType.MEDICAL_RECORD, audit.getResourceType());
        assertEquals(RECORD_ID, audit.getResourceId());
        assertTrue(audit.getDetail().contains("denialReason"));
    }

    @Test
    void rejectsAuthorizedRepresentativeWithoutDocumentAndWritesDenialAudit() {
        stubSignedContext();

        assertThrows(MedicalRecordMissingAuthorizationException.class,
                () -> service().issue(representativeCommand(null)));

        verify(pdfRenderer, never()).render(any());
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertEquals(ActionType.ACCESS_DENIED, auditCaptor.getValue().getActionType());
        assertEquals(ResourceType.MEDICAL_RECORD, auditCaptor.getValue().getResourceType());
        assertTrue(auditCaptor.getValue().getDetail().contains("AUTHORIZED_REPRESENTATIVE"));
    }

    @Test
    void rejectsUnsignedDraftRecord() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
        when(medicalRecordRepository.findById(RECORD_ID))
                .thenReturn(Optional.of(record(MedicalRecordStatus.DRAFT)));

        assertThrows(MedicalRecordNotSignedException.class, () -> service().issue(command()));

        verify(pdfRenderer, never()).render(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void rejectsNonManagerRole() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);
        when(medicalRecordRepository.findById(RECORD_ID))
                .thenReturn(Optional.of(record(MedicalRecordStatus.LOCKED)));

        assertThrows(AccessDeniedException.class, () -> service().issue(command()));

        verify(pdfRenderer, never()).render(any());
    }
}
