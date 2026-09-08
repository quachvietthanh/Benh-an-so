package com.benhsoan.application.ucservice.prescription;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionAllergyWarningLog;
import com.benhsoan.domain.auth.User;
import com.benhsoan.port.dto.command.prescription.SearchPrescriptionAllergyWarningLogsQuery;
import com.benhsoan.port.dto.result.PrescriptionAllergyWarningLogResult;
import com.benhsoan.port.inbound.prescription.GetPrescriptionAllergyWarningLogsUseCase;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionAllergyWarningLogRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;

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

    @Override
    public Page<PrescriptionAllergyWarningLogResult> search(SearchPrescriptionAllergyWarningLogsQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Order.desc("handledAt"), Sort.Order.desc("id"))
        );

        Page<PrescriptionAllergyWarningLog> logsPage = warningLogRepository.search(query, pageRequest);
        return logsPage.map(this::toResult);
    }

    private PrescriptionAllergyWarningLogResult toResult(PrescriptionAllergyWarningLog log) {
        String prescriptionCode = prescriptionRepository.findById(log.getPrescriptionId())
                .map(Prescription::getPrescriptionCode)
                .orElse(null);

        String patientCode = null;
        String patientName = null;
        Patient patient = patientRepository.findById(log.getPatientId()).orElse(null);
        if (patient != null) {
            patientCode = patient.getPatientCode();
            patientName = patient.getFullName();
        }

        String doctorName = userRepository.findById(log.getHandledBy())
                .map(User::getFullName)
                .orElse(null);

        String medicineName = medicineRepository.findById(log.getMedicineId())
                .map(Medicine::getMedicineName)
                .orElse(null);

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
    }
}
