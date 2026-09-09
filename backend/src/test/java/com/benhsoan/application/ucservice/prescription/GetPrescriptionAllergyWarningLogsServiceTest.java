package com.benhsoan.application.ucservice.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.AllergySeverity;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionAllergyWarningLog;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.auth.User;
import com.benhsoan.port.dto.command.prescription.SearchPrescriptionAllergyWarningLogsQuery;
import com.benhsoan.port.dto.result.PrescriptionAllergyWarningLogResult;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionAllergyWarningLogRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import org.springframework.security.access.AccessDeniedException;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetPrescriptionAllergyWarningLogsService Unit Tests")
class GetPrescriptionAllergyWarningLogsServiceTest {

        private static final Instant NOW = Instant.parse("2026-08-08T00:00:00Z");

        @Mock
        private PrescriptionAllergyWarningLogRepository warningLogRepository;
        @Mock
        private PrescriptionRepository prescriptionRepository;
        @Mock
        private PatientRepository patientRepository;
        @Mock
        private UserRepository userRepository;
        @Mock
        private MedicineRepository medicineRepository;
        @Mock
        private CurrentUserPort currentUserPort;

        private GetPrescriptionAllergyWarningLogsService service;

        @BeforeEach
        void setUp() {
                service = new GetPrescriptionAllergyWarningLogsService(
                                warningLogRepository,
                                prescriptionRepository,
                                patientRepository,
                                userRepository,
                                medicineRepository,
                                currentUserPort);
        }

        @Test
        void searchesLogsAndEnrichesDetails() {
                UUID logId = UUID.randomUUID();
                UUID prescriptionId = UUID.randomUUID();
                UUID patientId = UUID.randomUUID();
                UUID doctorId = UUID.randomUUID();
                UUID medicineId = UUID.randomUUID();

                when(currentUserPort.hasPermission("PRESCRIPTION_ALLERGY_WARNING_VIEW")).thenReturn(true);

                UUID allergyId = UUID.randomUUID();
                PrescriptionAllergyWarningLog log = PrescriptionAllergyWarningLog.restore(
                                logId, prescriptionId, patientId, allergyId, medicineId,
                                "Amoxicillin 500mg", "Amoxicillin", AllergySeverity.SEVERE,
                                "Anaphylaxis", "Clinically justified with monitoring", doctorId, NOW, NOW);

                when(warningLogRepository.search(any(), any()))
                                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1));

                PrescriptionItem item = PrescriptionItem.create(
                                UUID.randomUUID(), prescriptionId, medicineId,
                                "Amoxicillin 500mg", "Amoxicillin", "500 mg", "capsule",
                                "1 capsule", 2, AdministrationRoute.ORAL, 5, 10, null, NOW);
                Prescription prescription = Prescription.restore(
                                prescriptionId, "RX-000001", UUID.randomUUID(), PrescriptionStatus.PENDING_DISPENSE,
                                "Note", doctorId, NOW, null, null, List.of(item));
                when(prescriptionRepository.findAllById(any())).thenReturn(List.of(prescription));

                Patient patient = Patient.restore(
                                patientId, "PAT-001", "Nguyen Van A", java.time.LocalDate.of(1990, 1, 1),
                                Gender.MALE, "0901234567", null, null, null, null, null, null, null,
                                true, NOW, NOW, null, doctorId);
                when(patientRepository.findAllById(any())).thenReturn(List.of(patient));

                User doctor = User.restore(
                                doctorId, "dr.nguyen", "encodedPassword", "Dr. Nguyen", "dr@test.com", "0909999999",
                                UUID.randomUUID(), true, null, NOW);
                when(userRepository.findAllById(any())).thenReturn(List.of(doctor));

                Medicine medicine = Medicine.restore(
                                medicineId, "MED-001", "Amoxicillin 500mg", "Amoxicillin 500mg", "500 mg",
                                DosageForm.CAPSULE, "capsule", AdministrationRoute.ORAL, true, NOW, null, 0, 10);
                when(medicineRepository.findAllById(any())).thenReturn(List.of(medicine));

                SearchPrescriptionAllergyWarningLogsQuery query = new SearchPrescriptionAllergyWarningLogsQuery(null,
                                null, null, null, 0, 20);

                Page<PrescriptionAllergyWarningLogResult> resultPage = service.search(query);

                assertNotNull(resultPage);
                assertEquals(1, resultPage.getTotalElements());
                PrescriptionAllergyWarningLogResult result = resultPage.getContent().getFirst();

                assertEquals(logId, result.id());
                assertEquals("RX-000001", result.prescriptionCode());
                assertEquals("PAT-001", result.patientCode());
                assertEquals("Nguyen Van A", result.patientName());
                assertEquals("Dr. Nguyen", result.doctorName());
                assertEquals("Amoxicillin 500mg", result.medicineName());
                assertEquals("Amoxicillin", result.allergenName());
                assertEquals(AllergySeverity.SEVERE, result.severity());
                assertEquals("Clinically justified with monitoring", result.overrideReason());
        }

        @Test
        void throwsAccessDeniedExceptionWhenCallerLacksPermission() {
                when(currentUserPort.hasPermission("PRESCRIPTION_ALLERGY_WARNING_VIEW")).thenReturn(false);
                when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

                SearchPrescriptionAllergyWarningLogsQuery query = new SearchPrescriptionAllergyWarningLogsQuery(null,
                                null, null, null, 0, 20);

                assertThrows(AccessDeniedException.class, () -> service.search(query));
        }
}
