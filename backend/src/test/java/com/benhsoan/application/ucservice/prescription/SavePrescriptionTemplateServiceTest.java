package com.benhsoan.application.ucservice.prescription;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.PrescriptionTemplate;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.prescription.SavePrescriptionTemplateCommand;
import com.benhsoan.port.dto.result.PrescriptionTemplateResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionTemplateRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class SavePrescriptionTemplateServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-30T01:00:00Z");
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID PRESCRIPTION_ID = UUID.randomUUID();
    private static final UUID DIAGNOSIS_ID = UUID.randomUUID();

    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private PrescriptionTemplateRepository templateRepository;
    @Mock private DiagnosisCatalogRepository diagnosisCatalogRepository;
    @Mock private PrescriptionTemplateResultMapper resultMapper;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ClockPort clockPort;

    private SavePrescriptionTemplateService service;

    @BeforeEach
    void setUp() {
        service = new SavePrescriptionTemplateService(
                prescriptionRepository, templateRepository, diagnosisCatalogRepository,
                resultMapper, currentUserPort, auditLogRepository, clockPort, new ObjectMapper());
    }

    @Test
    void doctorSavesCompletedPrescriptionAsTemplate() {
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_ID);
        when(clockPort.now()).thenReturn(NOW);

        Prescription prescription = mock(Prescription.class);
        when(prescription.getId()).thenReturn(PRESCRIPTION_ID);
        when(prescription.isPendingDispense()).thenReturn(true);
        when(prescription.getPrescribedBy()).thenReturn(DOCTOR_ID);
        PrescriptionItem item = prescriptionItem();
        when(prescription.getItems()).thenReturn(List.of(item));
        when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(prescription));

        DiagnosisCatalog diagnosis = mock(DiagnosisCatalog.class);
        when(diagnosis.getId()).thenReturn(DIAGNOSIS_ID);
        when(diagnosis.getCode()).thenReturn("J06.9");
        when(diagnosisCatalogRepository.findByCode("J06.9")).thenReturn(Optional.of(diagnosis));

        PrescriptionTemplate saved = mock(PrescriptionTemplate.class);
        when(saved.getId()).thenReturn(UUID.randomUUID());
        when(saved.getItems()).thenReturn(List.of());
        when(templateRepository.save(any(PrescriptionTemplate.class))).thenReturn(saved);
        when(resultMapper.toResult(saved)).thenReturn(mock(PrescriptionTemplateResult.class));

        service.save(new SavePrescriptionTemplateCommand(PRESCRIPTION_ID, "J06.9"));

        verify(templateRepository).save(any(PrescriptionTemplate.class));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void nonDoctorIsRejected() {
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.save(new SavePrescriptionTemplateCommand(PRESCRIPTION_ID, "J06.9")));

        verify(templateRepository, never()).save(any());
    }

    @Test
    void notCompletedPrescriptionIsRejected() {
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_ID);

        Prescription prescription = mock(Prescription.class);
        when(prescription.isPendingDispense()).thenReturn(false);
        when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(prescription));

        assertThrows(ValidationException.class,
                () -> service.save(new SavePrescriptionTemplateCommand(PRESCRIPTION_ID, "J06.9")));
    }

    @Test
    void foreignDoctorPrescriptionIsRejected() {
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_ID);

        Prescription prescription = mock(Prescription.class);
        when(prescription.isPendingDispense()).thenReturn(true);
        when(prescription.getPrescribedBy()).thenReturn(UUID.randomUUID());
        when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(prescription));

        assertThrows(AccessDeniedException.class,
                () -> service.save(new SavePrescriptionTemplateCommand(PRESCRIPTION_ID, "J06.9")));
    }

    @Test
    void missingDiagnosisCodeIsRejected() {
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_ID);

        Prescription prescription = mock(Prescription.class);
        when(prescription.isPendingDispense()).thenReturn(true);
        when(prescription.getPrescribedBy()).thenReturn(DOCTOR_ID);
        when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(prescription));
        when(diagnosisCatalogRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> service.save(new SavePrescriptionTemplateCommand(PRESCRIPTION_ID, "UNKNOWN")));
    }

    private PrescriptionItem prescriptionItem() {
        PrescriptionItem item = mock(PrescriptionItem.class);
        when(item.getMedicineId()).thenReturn(UUID.randomUUID());
        when(item.getDosage()).thenReturn("1 viên");
        when(item.getFrequency()).thenReturn(2);
        when(item.getRoute()).thenReturn(AdministrationRoute.ORAL);
        when(item.getDurationDays()).thenReturn(7);
        when(item.getQuantity()).thenReturn(14);
        when(item.getInstructions()).thenReturn(null);
        return item;
    }
}
