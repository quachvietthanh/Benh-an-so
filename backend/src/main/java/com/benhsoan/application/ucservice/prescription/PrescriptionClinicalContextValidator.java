package com.benhsoan.application.ucservice.prescription;

import java.util.Objects;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.prescription.exception.PrescriptionClinicalContextConflictException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;

import lombok.RequiredArgsConstructor;

/** Validates that a doctor can make clinical changes for a medical record. */
@Component
@RequiredArgsConstructor
public class PrescriptionClinicalContextValidator {

    private final MedicalRecordRepository medicalRecordRepository;
    private final VisitRepository visitRepository;

    public MedicalRecord requireEditableRecordForDoctor(
            UUID medicalRecordId,
            UUID doctorId
    ) {
        if (medicalRecordId == null) {
            throw new ValidationException("Medical record id is required.");
        }

        MedicalRecord medicalRecord = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new ValidationException(
                        "Medical record not found: " + medicalRecordId
                ));
        medicalRecord.ensureEditable();

        Visit visit = visitRepository.findById(medicalRecord.getVisitId())
                .orElseThrow(() -> new ValidationException(
                        "Visit not found for medical record: " + medicalRecordId
                ));
        if (!visit.isActive()) {
            throw new PrescriptionClinicalContextConflictException(
                    "Prescriptions can only be changed during an active visit."
            );
        }
        if (!Objects.equals(visit.getDoctorId(), doctorId)) {
            throw new AccessDeniedException(
                    "Only the doctor responsible for the visit can change prescriptions."
            );
        }

        return medicalRecord;
    }
}
