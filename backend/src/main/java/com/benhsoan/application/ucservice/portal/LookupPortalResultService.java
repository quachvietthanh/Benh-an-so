package com.benhsoan.application.ucservice.portal;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.clinical.ClinicalOrder;
import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.domain.clinical.ClinicalResult;
import com.benhsoan.domain.clinical.enums.ClinicalResultStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultType;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.portal.exception.PortalLookupNotFoundException;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.dto.query.portal.LookupPortalResultQuery;
import com.benhsoan.port.dto.result.portal.PortalLookupResult;
import com.benhsoan.port.dto.result.portal.PortalLookupResult.ClinicalTestResultItem;
import com.benhsoan.port.dto.result.portal.PortalLookupResult.DiagnosisItem;
import com.benhsoan.port.dto.result.portal.PortalLookupResult.PrescriptionItemView;
import com.benhsoan.port.inbound.portal.LookupPortalResultUseCase;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LookupPortalResultService implements LookupPortalResultUseCase {

    /** Technical "system" account (seeded inactive) acting as the audit actor for anonymous lookups. */
    private static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final AppointmentRepository appointmentRepository;
    private final VisitRepository visitRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordDiagnosisRepository diagnosisRepository;
    private final ClinicalOrderRepository clinicalOrderRepository;
    private final ClinicalOrderItemRepository clinicalOrderItemRepository;
    private final ClinicalResultRepository clinicalResultRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final AuditLogRepository auditLogRepository;
    private final ClockPort clockPort;

    @Override
    public PortalLookupResult lookup(LookupPortalResultQuery query) {
        Appointment appointment = appointmentRepository.findByAppointmentCode(query.appointmentCode())
                .orElseThrow(PortalLookupNotFoundException::new);

        Patient patient = patientRepository.findById(appointment.getPatientId())
                .orElseThrow(PortalLookupNotFoundException::new);

        if (!matchesPhone(query.phoneNumber(), patient)) {
            throw new PortalLookupNotFoundException();
        }

        Visit visit = visitRepository.findByAppointmentId(appointment.getId())
                .filter(Visit::isCompleted)
                .orElseThrow(PortalLookupNotFoundException::new);

        User doctor = userRepository.findById(appointment.getDoctorId()).orElse(null);

        MedicalRecord record = medicalRecordRepository.findByVisitId(visit.getId()).orElse(null);
        List<MedicalRecordDiagnosis> diagnoses = record == null
                ? List.of()
                : diagnosisRepository.findByMedicalRecordId(record.getId());

        List<ClinicalTestResultItem> testResults = resolveClinicalTestResults(visit.getId());

        List<PrescriptionItemView> prescriptions = resolvePrescriptions(record);

        boolean hasMedicalResult = (record != null && (record.getConclusion() != null || !diagnoses.isEmpty() || record.getDoctorInstructions() != null))
                || !testResults.isEmpty()
                || !prescriptions.isEmpty();

        if (!hasMedicalResult) {
            throw new PortalLookupNotFoundException();
        }

        PortalLookupResult result = new PortalLookupResult(
                appointment.getAppointmentCode(),
                appointment.getStartTime(),
                appointment.getReason(),
                patient.getFullName(),
                patient.getDateOfBirth(),
                patient.getGender() == null ? null : patient.getGender().name(),
                maskPhone(patient.getPhone()),
                visit.getVisitCode(),
                visit.getVisitAt(),
                doctor == null ? null : doctor.getFullName(),
                diagnoses.stream().map(this::toDiagnosisItem).toList(),
                record == null ? null : record.getConclusion(),
                record == null ? null : record.getDoctorInstructions(),
                testResults,
                prescriptions
        );

        auditLogRepository.save(AuditLog.create(
                SYSTEM_USER_ID,
                ActionType.READ,
                ResourceType.PATIENT_PORTAL,
                appointment.getId(),
                "Patient portal lookup by appointment code: " + appointment.getAppointmentCode(),
                null,
                clockPort.now()
        ));

        return result;
    }

    private boolean matchesPhone(String phoneNumber, Patient patient) {
        if (phoneNumber == null) {
            return true;
        }
        if (patient.getPhone() == null) {
            return false;
        }
        return patient.getPhone().replaceAll("\\D", "")
                .equals(phoneNumber.replaceAll("\\D", ""));
    }

    private List<ClinicalTestResultItem> resolveClinicalTestResults(UUID visitId) {
        List<ClinicalOrder> orders = clinicalOrderRepository
                .findByVisitId(visitId, Pageable.unpaged())
                .getContent();

        if (orders.isEmpty()) {
            return List.of();
        }

        List<UUID> orderIds = orders.stream().map(ClinicalOrder::getId).toList();
        List<ClinicalOrderItem> items = clinicalOrderItemRepository.findByClinicalOrderIdIn(orderIds);

        if (items.isEmpty()) {
            return List.of();
        }

        List<UUID> itemIds = items.stream().map(ClinicalOrderItem::getId).toList();
        List<ClinicalResult> results = clinicalResultRepository.findByClinicalOrderItemIdIn(itemIds);

        return items.stream()
                .map(item -> toClinicalTestResultItem(item, findResult(results, item.getId())))
                .filter(Objects::nonNull)
                .toList();
    }

    private ClinicalResult findResult(List<ClinicalResult> results, UUID itemId) {
        return results.stream()
                .filter(r -> r.getClinicalOrderItemId().equals(itemId))
                .filter(r -> r.getStatus() == ClinicalResultStatus.FINAL)
                .findFirst()
                .orElse(null);
    }

    private ClinicalTestResultItem toClinicalTestResultItem(ClinicalOrderItem item, ClinicalResult result) {
        if (result == null) {
            return null;
        }

        String value = null;
        if (result.getResultType() == ClinicalResultType.NUMBER && result.getNumericValue() != null) {
            value = result.getNumericValue().stripTrailingZeros().toPlainString();
        } else if (result.getTextValue() != null) {
            value = result.getTextValue();
        } else if (result.getConclusion() != null) {
            value = result.getConclusion();
        }

        return new ClinicalTestResultItem(
                item.getServiceCode(),
                item.getServiceName(),
                result.getResultType().name(),
                value,
                result.getUnit(),
                result.getReferenceRange(),
                result.getConclusion()
        );
    }

    private List<PrescriptionItemView> resolvePrescriptions(MedicalRecord record) {
        if (record == null) {
            return List.of();
        }
        return prescriptionRepository.findByMedicalRecordId(record.getId()).stream()
                .flatMap(prescription -> toPrescriptionItemViews(prescription).stream())
                .toList();
    }

    private List<PrescriptionItemView> toPrescriptionItemViews(Prescription prescription) {
        List<PrescriptionItem> items = prescription.getItems();
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new PrescriptionItemView(
                        item.getMedicineName(),
                        item.getActiveIngredient(),
                        item.getStrength(),
                        item.getUnit(),
                        item.getDosage(),
                        item.getFrequency(),
                        item.getRoute() == null ? null : item.getRoute().name(),
                        item.getDurationDays(),
                        item.getQuantity(),
                        item.getInstructions()
                ))
                .toList();
    }

    private DiagnosisItem toDiagnosisItem(MedicalRecordDiagnosis diagnosis) {
        return new DiagnosisItem(
                diagnosis.getDiagnosisCode(),
                diagnosis.getDiagnosisName(),
                diagnosis.getDiagnosisType() == null ? null : diagnosis.getDiagnosisType().name()
        );
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 7) {
            return "***";
        }
        return digits.substring(0, 3) + "***" + digits.substring(digits.length() - 3);
    }
}
