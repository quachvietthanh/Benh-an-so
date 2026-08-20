package com.benhsoan.application.ucservice.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.clinical.ClinicalOrder;
import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.domain.clinical.ClinicalResult;
import com.benhsoan.domain.clinical.enums.ClinicalResultStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultType;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;
import com.benhsoan.domain.medicalrecord.enums.DiagnosisType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.portal.exception.PortalLookupNotFoundException;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.dto.query.portal.LookupPortalResultQuery;
import com.benhsoan.port.dto.result.portal.PortalLookupResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalResultRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

class LookupPortalResultServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-15T08:00:00Z");
    private static final String CODE = "APPT-2026-0001";
    private static final UUID APPOINTMENT_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID VISIT_ID = UUID.randomUUID();
    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();

    private final AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final MedicalRecordRepository medicalRecordRepository = mock(MedicalRecordRepository.class);
    private final MedicalRecordDiagnosisRepository diagnosisRepository = mock(MedicalRecordDiagnosisRepository.class);
    private final ClinicalOrderRepository clinicalOrderRepository = mock(ClinicalOrderRepository.class);
    private final ClinicalOrderItemRepository clinicalOrderItemRepository = mock(ClinicalOrderItemRepository.class);
    private final ClinicalResultRepository clinicalResultRepository = mock(ClinicalResultRepository.class);
    private final PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private LookupPortalResultService service;

    @BeforeEach
    void setUp() {
        service = new LookupPortalResultService(
                appointmentRepository, visitRepository, patientRepository, userRepository,
                medicalRecordRepository, diagnosisRepository, clinicalOrderRepository,
                clinicalOrderItemRepository, clinicalResultRepository, prescriptionRepository,
                auditLogRepository, clockPort);

        when(clockPort.now()).thenReturn(NOW);

        Appointment appointment = appointment();
        Visit visit = completedVisit();
        Patient patient = patient();
        User doctor = doctor();
        MedicalRecord record = record();
        MedicalRecordDiagnosis diagnosis = diagnosis();
        ClinicalOrder order = order();
        ClinicalOrderItem orderItem = orderItem();
        ClinicalResult result = result();
        Prescription prescription = prescription();

        when(appointmentRepository.findByAppointmentCode(CODE)).thenReturn(Optional.of(appointment));
        when(visitRepository.findByAppointmentId(APPOINTMENT_ID)).thenReturn(Optional.of(visit));
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(medicalRecordRepository.findByVisitId(VISIT_ID)).thenReturn(Optional.of(record));
        when(diagnosisRepository.findByMedicalRecordId(RECORD_ID)).thenReturn(List.of(diagnosis));
        when(clinicalOrderRepository.findByVisitId(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(clinicalOrderItemRepository.findByClinicalOrderIdIn(any()))
                .thenReturn(List.of(orderItem));
        when(clinicalResultRepository.findByClinicalOrderItemIdIn(any()))
                .thenReturn(List.of(result));
        when(prescriptionRepository.findByMedicalRecordId(RECORD_ID))
                .thenReturn(List.of(prescription));
    }

    @Test
    void returnsCompleteResultAndWritesAuditLog() {
        PortalLookupResult result = service.lookup(new LookupPortalResultQuery(CODE, null));

        assertNotNull(result);
        assertEquals(CODE, result.appointmentCode());
        assertEquals("Nguyen Van A", result.patientName());
        assertEquals("091***001", result.patientPhoneMasked());
        assertEquals("Bac Sy B", result.doctorName());
        assertEquals(1, result.diagnoses().size());
        assertEquals(1, result.clinicalTestResults().size());
        assertEquals(1, result.prescriptions().size());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void omitsNonFinalClinicalResults() {
        ClinicalResult draft = mock(ClinicalResult.class);
        when(draft.getClinicalOrderItemId()).thenReturn(ITEM_ID);
        when(draft.getResultType()).thenReturn(ClinicalResultType.NUMBER);
        when(draft.getStatus()).thenReturn(ClinicalResultStatus.DRAFT);
        when(draft.getNumericValue()).thenReturn(new BigDecimal("5.00"));

        when(clinicalResultRepository.findByClinicalOrderItemIdIn(any()))
                .thenReturn(List.of(draft));

        PortalLookupResult result = service.lookup(new LookupPortalResultQuery(CODE, null));

        assertEquals(0, result.clinicalTestResults().size());
    }

    @Test
    void throwsNotFoundWhenAppointmentMissing() {
        when(appointmentRepository.findByAppointmentCode(CODE)).thenReturn(Optional.empty());

        assertThrows(PortalLookupNotFoundException.class,
                () -> service.lookup(new LookupPortalResultQuery(CODE, null)));

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void throwsNotFoundWhenVisitNotCompleted() {
        Visit incomplete = incompleteVisit();
        when(visitRepository.findByAppointmentId(APPOINTMENT_ID))
                .thenReturn(Optional.of(incomplete));

        assertThrows(PortalLookupNotFoundException.class,
                () -> service.lookup(new LookupPortalResultQuery(CODE, null)));

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void throwsNotFoundWhenCompletedVisitHasNoMedicalResults() {
        when(medicalRecordRepository.findByVisitId(VISIT_ID)).thenReturn(Optional.empty());
        when(clinicalOrderRepository.findByVisitId(eq(VISIT_ID), any())).thenReturn(new PageImpl<>(List.of()));

        assertThrows(PortalLookupNotFoundException.class,
                () -> service.lookup(new LookupPortalResultQuery(CODE, null)));
    }

    @Test
    void throwsNotFoundWhenPhoneDoesNotMatch() {
        assertThrows(PortalLookupNotFoundException.class,
                () -> service.lookup(new LookupPortalResultQuery(CODE, "0987654321")));

        verify(auditLogRepository, never()).save(any());
    }

    private Appointment appointment() {
        Appointment appointment = mock(Appointment.class);
        when(appointment.getId()).thenReturn(APPOINTMENT_ID);
        when(appointment.getAppointmentCode()).thenReturn(CODE);
        when(appointment.getPatientId()).thenReturn(PATIENT_ID);
        when(appointment.getDoctorId()).thenReturn(DOCTOR_ID);
        when(appointment.getStartTime()).thenReturn(NOW);
        when(appointment.getReason()).thenReturn("Kham tong quat");
        return appointment;
    }

    private Visit completedVisit() {
        Visit visit = mock(Visit.class);
        when(visit.isCompleted()).thenReturn(true);
        when(visit.getId()).thenReturn(VISIT_ID);
        when(visit.getVisitCode()).thenReturn("VISIT-001");
        when(visit.getVisitAt()).thenReturn(NOW);
        return visit;
    }

    private Visit incompleteVisit() {
        Visit visit = mock(Visit.class);
        when(visit.isCompleted()).thenReturn(false);
        return visit;
    }

    private Patient patient() {
        Patient patient = mock(Patient.class);
        when(patient.getFullName()).thenReturn("Nguyen Van A");
        when(patient.getDateOfBirth()).thenReturn(LocalDate.of(1990, 1, 1));
        when(patient.getGender()).thenReturn(Gender.MALE);
        when(patient.getPhone()).thenReturn("0912345001");
        return patient;
    }

    private User doctor() {
        User user = mock(User.class);
        when(user.getFullName()).thenReturn("Bac Sy B");
        return user;
    }

    private MedicalRecord record() {
        MedicalRecord record = mock(MedicalRecord.class);
        when(record.getId()).thenReturn(RECORD_ID);
        when(record.getConclusion()).thenReturn("Ket luan kham");
        when(record.getDoctorInstructions()).thenReturn("Uong thuoc theo don");
        return record;
    }

    private MedicalRecordDiagnosis diagnosis() {
        MedicalRecordDiagnosis diagnosis = mock(MedicalRecordDiagnosis.class);
        when(diagnosis.getDiagnosisCode()).thenReturn("D001");
        when(diagnosis.getDiagnosisName()).thenReturn("Viem hong");
        when(diagnosis.getDiagnosisType()).thenReturn(DiagnosisType.PRIMARY);
        return diagnosis;
    }

    private ClinicalOrder order() {
        ClinicalOrder order = mock(ClinicalOrder.class);
        when(order.getId()).thenReturn(UUID.randomUUID());
        return order;
    }

    private ClinicalOrderItem orderItem() {
        ClinicalOrderItem item = mock(ClinicalOrderItem.class);
        when(item.getId()).thenReturn(ITEM_ID);
        when(item.getServiceCode()).thenReturn("XN001");
        when(item.getServiceName()).thenReturn("Cong thuc mau");
        return item;
    }

    private ClinicalResult result() {
        ClinicalResult result = mock(ClinicalResult.class);
        when(result.getClinicalOrderItemId()).thenReturn(ITEM_ID);
        when(result.getResultType()).thenReturn(ClinicalResultType.NUMBER);
        when(result.getStatus()).thenReturn(ClinicalResultStatus.FINAL);
        when(result.getNumericValue()).thenReturn(new BigDecimal("5.00"));
        when(result.getUnit()).thenReturn("10^9/L");
        when(result.getReferenceRange()).thenReturn("4-10");
        when(result.getConclusion()).thenReturn("Binh thuong");
        return result;
    }

    private Prescription prescription() {
        Prescription prescription = mock(Prescription.class);
        PrescriptionItem item = prescriptionItem();
        when(prescription.getItems()).thenReturn(List.of(item));
        return prescription;
    }

    private PrescriptionItem prescriptionItem() {
        PrescriptionItem item = mock(PrescriptionItem.class);
        when(item.getMedicineName()).thenReturn("Paracetamol");
        when(item.getActiveIngredient()).thenReturn("Paracetamol");
        when(item.getStrength()).thenReturn("500mg");
        when(item.getUnit()).thenReturn("vien");
        when(item.getDosage()).thenReturn("1 vien");
        when(item.getFrequency()).thenReturn(3);
        when(item.getRoute()).thenReturn(com.benhsoan.domain.medicine.enums.AdministrationRoute.ORAL);
        when(item.getDurationDays()).thenReturn(5);
        when(item.getQuantity()).thenReturn(15);
        when(item.getInstructions()).thenReturn("Uong sau an");
        return item;
    }
}

