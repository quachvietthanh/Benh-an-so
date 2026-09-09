package com.benhsoan.application.ucservice.prescription;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionAllergyWarningLog;
import com.benhsoan.port.dto.command.prescription.SearchPrescriptionAllergyWarningLogsQuery;
import com.benhsoan.port.dto.result.PrescriptionAllergyWarningLogResult;
import com.benhsoan.port.inbound.prescription.GetPrescriptionAllergyWarningLogsUseCase;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionAllergyWarningLogRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPrescriptionAllergyWarningLogsService implements GetPrescriptionAllergyWarningLogsUseCase {

    private final PrescriptionAllergyWarningLogRepository warningLogRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final MedicineRepository medicineRepository;
    private final CurrentUserPort currentUserPort;

    @Override
    public Page<PrescriptionAllergyWarningLogResult> search(SearchPrescriptionAllergyWarningLogsQuery query) {
        if (!currentUserPort.hasPermission("PRESCRIPTION_ALLERGY_WARNING_VIEW") && !currentUserPort.hasRole("ADMIN")) {
            throw new AccessDeniedException("Access denied: requires permission PRESCRIPTION_ALLERGY_WARNING_VIEW");
        }

        PageRequest pageRequest = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Order.desc("handledAt"), Sort.Order.desc("id"))
        );

        Page<PrescriptionAllergyWarningLog> logsPage = warningLogRepository.search(query, pageRequest);
        List<PrescriptionAllergyWarningLog> content = logsPage.getContent();
        if (content.isEmpty()) {
            return logsPage.map(log -> null);
        }

        List<UUID> prescriptionIds = content.stream()
                .map(PrescriptionAllergyWarningLog::getPrescriptionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<UUID> patientIds = content.stream()
                .map(PrescriptionAllergyWarningLog::getPatientId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<UUID> doctorIds = content.stream()
                .map(PrescriptionAllergyWarningLog::getHandledBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<UUID> medicineIds = content.stream()
                .map(PrescriptionAllergyWarningLog::getMedicineId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, String> prescriptionCodeMap = prescriptionRepository.findAllById(prescriptionIds).stream()
                .collect(Collectors.toMap(Prescription::getId, Prescription::getPrescriptionCode, (a, b) -> a));

        Map<UUID, Patient> patientMap = patientRepository.findAllById(patientIds).stream()
                .collect(Collectors.toMap(Patient::getId, p -> p, (a, b) -> a));

        Map<UUID, String> doctorNameMap = userRepository.findAllById(doctorIds).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName, (a, b) -> a));

        Map<UUID, String> medicineNameMap = medicineRepository.findAllById(medicineIds).stream()
                .collect(Collectors.toMap(Medicine::getId, Medicine::getMedicineName, (a, b) -> a));

        return logsPage.map(log -> {
            String prescriptionCode = prescriptionCodeMap.get(log.getPrescriptionId());
            Patient patient = patientMap.get(log.getPatientId());
            String patientCode = patient != null ? patient.getPatientCode() : null;
            String patientName = patient != null ? patient.getFullName() : null;
            String doctorName = doctorNameMap.get(log.getHandledBy());
            String medicineName = medicineNameMap.get(log.getMedicineId());

            return new PrescriptionAllergyWarningLogResult(
                    log.getId(),
                    log.getPrescriptionId(),
                    prescriptionCode,
                    log.getPatientId(),
                    patientCode,
                    patientName,
                    log.getHandledBy(),
                    doctorName,
                    log.getMedicineId(),
                    medicineName,
                    log.getActiveIngredient(),
                    log.getAllergenName(),
                    log.getSeverity(),
                    log.getReaction(),
                    log.getOverrideReason(),
                    log.getHandledAt()
            );
        });
    }
}
