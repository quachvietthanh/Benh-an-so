package com.benhsoan.application.ucservice.prescription;

import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PrescriptionReadAccessValidator {

    private final CurrentUserPort currentUserPort;
    private final MedicalRecordRepository medicalRecordRepository;
    private final VisitRepository visitRepository;

    public void requireCanReadDispensingQueue() {
        if (!currentUserPort.hasRole("ADMIN") && !currentUserPort.hasRole("PHARMACIST")) {
            throw new AccessDeniedException("Only pharmacists and admins can view the dispensing queue.");
        }
    }

    public void requireCanRead(Prescription prescription) {
        if (currentUserPort.hasRole("ADMIN") || currentUserPort.hasRole("PHARMACIST")) {
            return;
        }
        if (!currentUserPort.hasRole("DOCTOR")) {
            throw new AccessDeniedException("You are not allowed to view prescriptions.");
        }

        MedicalRecord record = medicalRecordRepository
                .findById(prescription.getMedicalRecordId())
                .orElseThrow(() -> new ValidationException(
                        "Medical record not found: " + prescription.getMedicalRecordId()
                ));
        Visit visit = visitRepository.findById(record.getVisitId())
                .orElseThrow(() -> new ValidationException(
                        "Visit not found for medical record: " + record.getId()
                ));
        if (!Objects.equals(visit.getDoctorId(), currentUserPort.getCurrentUserId())) {
            throw new AccessDeniedException(
                    "Doctors can only view prescriptions of their own visits."
            );
        }
    }
}
