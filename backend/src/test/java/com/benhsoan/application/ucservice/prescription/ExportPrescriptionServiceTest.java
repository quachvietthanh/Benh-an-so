package com.benhsoan.application.ucservice.prescription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.exception.PrescriptionNotPrintableException;
import com.benhsoan.domain.prescription.exception.PrescriptionNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.infrastructure.pdf.PdfBoxPrescriptionPdfRenderer;
import com.benhsoan.infrastructure.pdf.PdfRenderingException;
import com.benhsoan.port.outbound.pdf.PrescriptionPdfRenderer;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class ExportPrescriptionServiceTest {

    @Test
    void exportsPdfWithRequiredPrescriptionAndClinicData() throws Exception {
        UUID prescriptionId = UUID.randomUUID();
        PrescriptionRepository prescriptionRepository = Mockito.mock(PrescriptionRepository.class);
        PrescriptionReadAccessValidator accessValidator = Mockito.mock(PrescriptionReadAccessValidator.class);
        PrescriptionDisplayContextResolver contextResolver = Mockito.mock(PrescriptionDisplayContextResolver.class);
        ClinicConfigurationRepository clinicConfigurationRepository = Mockito.mock(ClinicConfigurationRepository.class);
        CurrentUserPort currentUserPort = Mockito.mock(CurrentUserPort.class);
        AuditLogRepository auditLogRepository = Mockito.mock(AuditLogRepository.class);
        ClockPort clockPort = Mockito.mock(ClockPort.class);
        PrescriptionPdfRenderer pdfRenderer = Mockito.mock(PrescriptionPdfRenderer.class);
        byte[] renderedPdf = "%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        when(pdfRenderer.render(Mockito.any())).thenReturn(renderedPdf);
        ExportPrescriptionService service = new ExportPrescriptionService(
                prescriptionRepository,
                accessValidator,
                contextResolver,
                clinicConfigurationRepository,
                pdfRenderer,
                currentUserPort,
                auditLogRepository,
                clockPort
        );
        Instant prescribedAt = Instant.parse("2026-08-20T03:00:00Z");
        UUID medicalRecordId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Prescription prescription = Prescription.create(
                prescriptionId, "RX-0001", medicalRecordId, null, doctorId, prescribedAt,
                List.of(PrescriptionItem.create(
                        UUID.randomUUID(), prescriptionId, UUID.randomUUID(), "Paracetamol Vi\u1ec7t",
                        "Paracetamol", "500 mg", "tablet", "1 tablet", 2,
                        AdministrationRoute.ORAL, 5, 10, "After meals", prescribedAt
                ), PrescriptionItem.create(
                        UUID.randomUUID(), prescriptionId, UUID.randomUUID(), "Amoxicillin",
                        "Amoxicillin", "250 mg", "capsule", "1 capsule", 3,
                        AdministrationRoute.ORAL, 7, 21, "Before meals", prescribedAt
                ), PrescriptionItem.create(
                        UUID.randomUUID(), prescriptionId, UUID.randomUUID(), "Vitamin C",
                        "Ascorbic acid", "100 mg", "tablet", "1 tablet", 1,
                        AdministrationRoute.ORAL, 10, 10, "After breakfast", prescribedAt
                ))
        );
        when(prescriptionRepository.findById(prescriptionId)).thenReturn(java.util.Optional.of(prescription));
        when(contextResolver.resolve(medicalRecordId, doctorId)).thenReturn(
                new PrescriptionDisplayContextResolver.PrescriptionDisplayContext(
                        UUID.randomUUID(), "VISIT-001", UUID.randomUUID(), "PAT-001",
                        "Nguy\u1ec5n V\u0103n A", "B\u00e1c s\u0129 B"));
        when(clinicConfigurationRepository.find()).thenReturn(java.util.Optional.of(ClinicConfiguration.create(
                "Ph\u00f2ng kh\u00e1m A", "1 Main Street", "0900000000",
                LocalTime.of(8, 0), LocalTime.of(17, 0), prescribedAt
        )));
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        UUID printedBy = UUID.randomUUID();
        Instant printedAt = Instant.parse("2026-08-20T04:00:00Z");
        when(currentUserPort.getCurrentUserId()).thenReturn(printedBy);
        when(currentUserPort.getCurrentUserRoles()).thenReturn(Set.of("DOCTOR"));
        when(clockPort.now()).thenReturn(printedAt, printedAt.plusSeconds(60));
        var result = service.export(prescriptionId);
        var secondResult = service.export(prescriptionId);

        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.fileName()).isEqualTo("prescription-RX-0001.pdf");
        assertThat(result.content()).isEqualTo(renderedPdf);
        assertThat(secondResult.content()).isEqualTo(renderedPdf);
        var printDocumentCaptor = org.mockito.ArgumentCaptor.forClass(
                com.benhsoan.port.dto.result.PrescriptionPrintDocument.class);
        Mockito.verify(pdfRenderer, Mockito.times(2)).render(printDocumentCaptor.capture());
        var printDocument = printDocumentCaptor.getAllValues().get(0);
        assertThat(printDocument.clinicName()).isEqualTo("Phòng khám A");
        assertThat(printDocument.clinicAddress()).isEqualTo("1 Main Street");
        assertThat(printDocument.clinicPhone()).isEqualTo("0900000000");
        assertThat(printDocument.prescriptionCode()).isEqualTo("RX-0001");
        assertThat(printDocument.patientCode()).isEqualTo("PAT-001");
        assertThat(printDocument.patientName()).isEqualTo("Nguyễn Văn A");
        assertThat(printDocument.doctorId()).isEqualTo(doctorId);
        assertThat(printDocument.doctorName()).isEqualTo("Bác sĩ B");
        assertThat(printDocument.prescribedAt()).isEqualTo(prescribedAt);
        assertThat(printDocument.items()).extracting(
                com.benhsoan.port.dto.result.PrescriptionPrintDocument.Item::medicineName,
                com.benhsoan.port.dto.result.PrescriptionPrintDocument.Item::strength,
                com.benhsoan.port.dto.result.PrescriptionPrintDocument.Item::quantity
        ).containsExactly(
                org.assertj.core.groups.Tuple.tuple("Paracetamol Việt", "500 mg", 10),
                org.assertj.core.groups.Tuple.tuple("Amoxicillin", "250 mg", 21),
                org.assertj.core.groups.Tuple.tuple("Vitamin C", "100 mg", 10)
        );
        var auditLogCaptor = org.mockito.ArgumentCaptor.forClass(AuditLog.class);
        Mockito.verify(auditLogRepository, Mockito.times(2)).save(auditLogCaptor.capture());
        assertThat(auditLogCaptor.getAllValues()).allSatisfy(auditLog -> {
            assertThat(auditLog.getActionType()).isEqualTo(ActionType.EXPORT);
            assertThat(auditLog.getResourceType()).isEqualTo(ResourceType.PRESCRIPTION);
            assertThat(auditLog.getResourceId()).isEqualTo(prescriptionId);
        });
        assertThat(auditLogCaptor.getAllValues().get(0).getDetail()).contains(
                "\"prescriptionCode\":\"RX-0001\"",
                printedBy.toString(), printedAt.toString(), "\"DOCTOR\""
        ).doesNotContain("\"printNumber\"");
        assertThat(auditLogCaptor.getAllValues().get(1).getDetail()).doesNotContain("\"printNumber\"");
    }

    @Test
    void rejectsCancelledPrescriptionBeforeRendering() {
        UUID prescriptionId = UUID.randomUUID();
        PrescriptionRepository prescriptionRepository = Mockito.mock(PrescriptionRepository.class);
        Prescription prescription = Prescription.create(
                prescriptionId, "RX-0002", UUID.randomUUID(), null, UUID.randomUUID(), Instant.now(),
                List.of(PrescriptionItem.create(
                        UUID.randomUUID(), prescriptionId, UUID.randomUUID(), "Medicine", "Ingredient",
                        "500 mg", "tablet", "1 tablet", 1, AdministrationRoute.ORAL, 1, 1, null, Instant.now()
                ))
        );
        prescription.cancel(UUID.randomUUID(), Instant.now());
        when(prescriptionRepository.findById(prescriptionId)).thenReturn(java.util.Optional.of(prescription));
        CurrentUserPort currentUserPort = Mockito.mock(CurrentUserPort.class);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        ExportPrescriptionService service = new ExportPrescriptionService(
                prescriptionRepository,
                Mockito.mock(PrescriptionReadAccessValidator.class),
                Mockito.mock(PrescriptionDisplayContextResolver.class),
                Mockito.mock(ClinicConfigurationRepository.class),
                new PdfBoxPrescriptionPdfRenderer(),
                currentUserPort,
                Mockito.mock(AuditLogRepository.class),
                Mockito.mock(ClockPort.class)
        );

        assertThatThrownBy(() -> service.export(prescriptionId))
                .isInstanceOf(PrescriptionNotPrintableException.class)
                .hasMessage("Only active prescriptions can be printed.");
    }

    @Test
    void doesNotWriteAuditWhenPdfRenderingFails() {
        UUID prescriptionId = UUID.randomUUID();
        Instant prescribedAt = Instant.parse("2026-08-20T03:00:00Z");
        Prescription prescription = Prescription.create(
                prescriptionId, "RX-ERROR", UUID.randomUUID(), null, UUID.randomUUID(), prescribedAt,
                List.of(PrescriptionItem.create(
                        UUID.randomUUID(), prescriptionId, UUID.randomUUID(), "Medicine", "Ingredient",
                        "500 mg", "tablet", "1 tablet", 1, AdministrationRoute.ORAL, 1, 1, null, prescribedAt
                ))
        );
        PrescriptionRepository prescriptionRepository = Mockito.mock(PrescriptionRepository.class);
        when(prescriptionRepository.findById(prescriptionId)).thenReturn(java.util.Optional.of(prescription));
        PrescriptionDisplayContextResolver contextResolver = Mockito.mock(PrescriptionDisplayContextResolver.class);
        when(contextResolver.resolve(prescription.getMedicalRecordId(), prescription.getPrescribedBy())).thenReturn(
                new PrescriptionDisplayContextResolver.PrescriptionDisplayContext(
                        UUID.randomUUID(), "VISIT-001", UUID.randomUUID(), "PAT-001", "Patient", "Doctor")
        );
        ClinicConfigurationRepository clinicConfigurationRepository = Mockito.mock(ClinicConfigurationRepository.class);
        when(clinicConfigurationRepository.find()).thenReturn(java.util.Optional.of(ClinicConfiguration.create(
                "Clinic", "Address", "0900000000", LocalTime.of(8, 0), LocalTime.of(17, 0), prescribedAt
        )));
        CurrentUserPort currentUserPort = Mockito.mock(CurrentUserPort.class);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        PrescriptionPdfRenderer renderer = Mockito.mock(PrescriptionPdfRenderer.class);
        when(renderer.render(Mockito.any())).thenThrow(new PdfRenderingException(
                "Unable to generate prescription PDF.", new java.io.IOException("Disk error")
        ));
        AuditLogRepository auditLogRepository = Mockito.mock(AuditLogRepository.class);
        ExportPrescriptionService service = new ExportPrescriptionService(
                prescriptionRepository,
                Mockito.mock(PrescriptionReadAccessValidator.class),
                contextResolver,
                clinicConfigurationRepository,
                renderer,
                currentUserPort,
                auditLogRepository,
                Mockito.mock(ClockPort.class)
        );

        assertThatThrownBy(() -> service.export(prescriptionId))
                .isInstanceOf(PdfRenderingException.class)
                .hasMessage("Unable to generate prescription PDF.");
        Mockito.verifyNoInteractions(auditLogRepository);
    }

    @Test
    void rejectsUsersWhoAreNeitherDoctorNorPharmacist() {
        UUID prescriptionId = UUID.randomUUID();
        PrescriptionRepository prescriptionRepository = Mockito.mock(PrescriptionRepository.class);
        Prescription prescription = Prescription.create(
                prescriptionId, "RX-0003", UUID.randomUUID(), null, UUID.randomUUID(), Instant.now(),
                List.of(PrescriptionItem.create(
                        UUID.randomUUID(), prescriptionId, UUID.randomUUID(), "Medicine", "Ingredient",
                        "500 mg", "tablet", "1 tablet", 1, AdministrationRoute.ORAL, 1, 1, null, Instant.now()
                ))
        );
        when(prescriptionRepository.findById(prescriptionId)).thenReturn(java.util.Optional.of(prescription));
        ExportPrescriptionService service = new ExportPrescriptionService(
                prescriptionRepository,
                Mockito.mock(PrescriptionReadAccessValidator.class),
                Mockito.mock(PrescriptionDisplayContextResolver.class),
                Mockito.mock(ClinicConfigurationRepository.class),
                new PdfBoxPrescriptionPdfRenderer(),
                Mockito.mock(CurrentUserPort.class),
                Mockito.mock(AuditLogRepository.class),
                Mockito.mock(ClockPort.class)
        );

        assertThatThrownBy(() -> service.export(prescriptionId))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessage("Only doctors and pharmacists can print prescriptions.");
    }

    @Test
    void rejectsAdminEvenWhenPrintPermissionWasGranted() {
        UUID prescriptionId = UUID.randomUUID();
        PrescriptionRepository prescriptionRepository = Mockito.mock(PrescriptionRepository.class);
        Prescription prescription = Prescription.create(
                prescriptionId, "RX-0004", UUID.randomUUID(), null, UUID.randomUUID(), Instant.now(),
                List.of(PrescriptionItem.create(
                        UUID.randomUUID(), prescriptionId, UUID.randomUUID(), "Medicine", "Ingredient",
                        "500 mg", "tablet", "1 tablet", 1, AdministrationRoute.ORAL, 1, 1, null, Instant.now()
                ))
        );
        when(prescriptionRepository.findById(prescriptionId)).thenReturn(java.util.Optional.of(prescription));
        CurrentUserPort currentUserPort = Mockito.mock(CurrentUserPort.class);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(currentUserPort.getCurrentUserRoles()).thenReturn(Set.of("ADMIN", "PERMISSION_PRESCRIPTION_PRINT"));
        ExportPrescriptionService service = new ExportPrescriptionService(
                prescriptionRepository,
                Mockito.mock(PrescriptionReadAccessValidator.class),
                Mockito.mock(PrescriptionDisplayContextResolver.class),
                Mockito.mock(ClinicConfigurationRepository.class),
                new PdfBoxPrescriptionPdfRenderer(),
                currentUserPort,
                Mockito.mock(AuditLogRepository.class),
                Mockito.mock(ClockPort.class)
        );

        assertThatThrownBy(() -> service.export(prescriptionId))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessage("Only doctors and pharmacists can print prescriptions.");
    }

    @Test
    void rejectsPrescriptionWithoutCode() {
        UUID prescriptionId = UUID.randomUUID();
        PrescriptionRepository prescriptionRepository = Mockito.mock(PrescriptionRepository.class);
        Prescription prescription = Mockito.mock(Prescription.class);
        when(prescription.getStatus()).thenReturn(com.benhsoan.domain.prescription.enums.PrescriptionStatus.PENDING_DISPENSE);
        when(prescription.getPrescriptionCode()).thenReturn(null);
        when(prescriptionRepository.findById(prescriptionId)).thenReturn(java.util.Optional.of(prescription));
        CurrentUserPort currentUserPort = Mockito.mock(CurrentUserPort.class);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        ExportPrescriptionService service = new ExportPrescriptionService(
                prescriptionRepository, Mockito.mock(PrescriptionReadAccessValidator.class),
                Mockito.mock(PrescriptionDisplayContextResolver.class), Mockito.mock(ClinicConfigurationRepository.class),
                new PdfBoxPrescriptionPdfRenderer(), currentUserPort,
                Mockito.mock(AuditLogRepository.class), Mockito.mock(ClockPort.class)
        );

        assertThatThrownBy(() -> service.export(prescriptionId))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Prescription code is required for printing.");
    }

    @Test
    void usesPrescribingDoctorInsteadOfVisitDoctorInPrintModel() {
        UUID prescriptionId = UUID.randomUUID();
        UUID medicalRecordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID visitDoctorId = UUID.randomUUID();
        UUID prescribingDoctorId = UUID.randomUUID();
        Instant prescribedAt = Instant.parse("2026-08-20T03:00:00Z");
        Prescription prescription = Prescription.create(
                prescriptionId, "RX-0005", medicalRecordId, null, prescribingDoctorId, prescribedAt,
                List.of(PrescriptionItem.create(
                        UUID.randomUUID(), prescriptionId, UUID.randomUUID(), "Medicine", "Ingredient",
                        "500 mg", "tablet", "1 tablet", 1, AdministrationRoute.ORAL, 1, 1, null, prescribedAt
                ))
        );
        PrescriptionRepository prescriptionRepository = Mockito.mock(PrescriptionRepository.class);
        when(prescriptionRepository.findById(prescriptionId)).thenReturn(java.util.Optional.of(prescription));

        MedicalRecordRepository medicalRecordRepository = Mockito.mock(MedicalRecordRepository.class);
        com.benhsoan.domain.medicalrecord.MedicalRecord medicalRecord =
                Mockito.mock(com.benhsoan.domain.medicalrecord.MedicalRecord.class);
        when(medicalRecord.getVisitId()).thenReturn(visitId);
        when(medicalRecordRepository.findById(medicalRecordId)).thenReturn(java.util.Optional.of(medicalRecord));
        VisitRepository visitRepository = Mockito.mock(VisitRepository.class);
        com.benhsoan.domain.visit.Visit visit = Mockito.mock(com.benhsoan.domain.visit.Visit.class);
        when(visit.getVisitCode()).thenReturn("VISIT-001");
        when(visit.getPatientId()).thenReturn(patientId);
        when(visit.getDoctorId()).thenReturn(visitDoctorId);
        when(visitRepository.findById(visitId)).thenReturn(java.util.Optional.of(visit));
        PatientRepository patientRepository = Mockito.mock(PatientRepository.class);
        com.benhsoan.domain.patient.Patient patient = Mockito.mock(com.benhsoan.domain.patient.Patient.class);
        when(patient.getPatientCode()).thenReturn("PAT-001");
        when(patient.getFullName()).thenReturn("Nguyen Van A");
        when(patientRepository.findById(patientId)).thenReturn(java.util.Optional.of(patient));
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        com.benhsoan.domain.auth.User prescribingDoctor = Mockito.mock(com.benhsoan.domain.auth.User.class);
        when(prescribingDoctor.getFullName()).thenReturn("Dr. Prescriber");
        when(userRepository.findById(prescribingDoctorId)).thenReturn(java.util.Optional.of(prescribingDoctor));

        PrescriptionPdfRenderer renderer = Mockito.mock(PrescriptionPdfRenderer.class);
        when(renderer.render(Mockito.any())).thenReturn("%PDF".getBytes());
        ClinicConfigurationRepository clinicConfigurationRepository = Mockito.mock(ClinicConfigurationRepository.class);
        when(clinicConfigurationRepository.find()).thenReturn(java.util.Optional.of(ClinicConfiguration.create(
                "Clinic", "Address", "0900000000", LocalTime.of(8, 0), LocalTime.of(17, 0), prescribedAt
        )));
        CurrentUserPort currentUserPort = Mockito.mock(CurrentUserPort.class);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(currentUserPort.getCurrentUserRoles()).thenReturn(Set.of("DOCTOR"));
        AuditLogRepository auditLogRepository = Mockito.mock(AuditLogRepository.class);
        ClockPort clockPort = Mockito.mock(ClockPort.class);
        when(clockPort.now()).thenReturn(prescribedAt);
        ExportPrescriptionService service = new ExportPrescriptionService(
                prescriptionRepository,
                Mockito.mock(PrescriptionReadAccessValidator.class),
                new PrescriptionDisplayContextResolver(
                        medicalRecordRepository, visitRepository, patientRepository, userRepository),
                clinicConfigurationRepository,
                renderer,
                currentUserPort,
                auditLogRepository,
                clockPort
        );

        service.export(prescriptionId);

        var documentCaptor = org.mockito.ArgumentCaptor.forClass(
                com.benhsoan.port.dto.result.PrescriptionPrintDocument.class);
        Mockito.verify(renderer).render(documentCaptor.capture());
        assertThat(documentCaptor.getValue().doctorName()).isEqualTo("Dr. Prescriber");
        Mockito.verify(userRepository).findById(prescribingDoctorId);
        Mockito.verify(userRepository, Mockito.never()).findById(visitDoctorId);
    }

    @Test
    void rejectsPrintingWhenPrescribingDoctorCannotBeResolved() {
        UUID prescriptionId = UUID.randomUUID();
        PrescriptionRepository prescriptionRepository = Mockito.mock(PrescriptionRepository.class);
        Prescription prescription = Prescription.create(
                prescriptionId, "RX-0006", UUID.randomUUID(), null, UUID.randomUUID(), Instant.now(),
                List.of(PrescriptionItem.create(
                        UUID.randomUUID(), prescriptionId, UUID.randomUUID(), "Medicine", "Ingredient",
                        "500 mg", "tablet", "1 tablet", 1, AdministrationRoute.ORAL, 1, 1, null, Instant.now()
                ))
        );
        when(prescriptionRepository.findById(prescriptionId)).thenReturn(java.util.Optional.of(prescription));
        PrescriptionDisplayContextResolver contextResolver = Mockito.mock(PrescriptionDisplayContextResolver.class);
        when(contextResolver.resolve(prescription.getMedicalRecordId(), prescription.getPrescribedBy())).thenReturn(
                new PrescriptionDisplayContextResolver.PrescriptionDisplayContext(
                        UUID.randomUUID(), "VISIT-001", UUID.randomUUID(), "PAT-001", "Patient", null)
        );
        CurrentUserPort currentUserPort = Mockito.mock(CurrentUserPort.class);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        PrescriptionPdfRenderer renderer = Mockito.mock(PrescriptionPdfRenderer.class);
        ClinicConfigurationRepository clinicConfigurationRepository = Mockito.mock(ClinicConfigurationRepository.class);
        when(clinicConfigurationRepository.find()).thenReturn(java.util.Optional.of(ClinicConfiguration.create(
                "Clinic", "Address", "0900000000", LocalTime.of(8, 0), LocalTime.of(17, 0), Instant.now()
        )));
        ExportPrescriptionService service = new ExportPrescriptionService(
                prescriptionRepository,
                Mockito.mock(PrescriptionReadAccessValidator.class),
                contextResolver,
                clinicConfigurationRepository,
                renderer,
                currentUserPort,
                Mockito.mock(AuditLogRepository.class),
                Mockito.mock(ClockPort.class)
        );

        assertThatThrownBy(() -> service.export(prescriptionId))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Prescribing doctor was not found for printing.");
        Mockito.verifyNoInteractions(renderer);
    }

    @Test
    void reportsNotFoundWhenPrescriptionDoesNotExist() {
        UUID prescriptionId = UUID.randomUUID();
        PrescriptionRepository prescriptionRepository = Mockito.mock(PrescriptionRepository.class);
        when(prescriptionRepository.findById(prescriptionId)).thenReturn(java.util.Optional.empty());
        ExportPrescriptionService service = new ExportPrescriptionService(
                prescriptionRepository, Mockito.mock(PrescriptionReadAccessValidator.class),
                Mockito.mock(PrescriptionDisplayContextResolver.class), Mockito.mock(ClinicConfigurationRepository.class),
                new PdfBoxPrescriptionPdfRenderer(), Mockito.mock(CurrentUserPort.class),
                Mockito.mock(AuditLogRepository.class), Mockito.mock(ClockPort.class)
        );

        assertThatThrownBy(() -> service.export(prescriptionId))
                .isInstanceOf(PrescriptionNotFoundException.class);
    }
}
