package com.benhsoan.application.ucservice.prescription;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PrescriptionDisplayContextResolver {

    private final MedicalRecordRepository medicalRecordRepository;
    private final VisitRepository visitRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    public PrescriptionDisplayContext resolve(
            UUID medicalRecordId,
            UUID prescribedBy
    ) {
        UUID visitId = null;
        String visitCode = null;
        UUID patientId = null;
        String patientCode = null;
        String patientName = null;
        String doctorName = null;

        if (medicalRecordId != null) {
            var medicalRecord = medicalRecordRepository.findById(medicalRecordId)
                    .orElse(null);
            if (medicalRecord != null) {
                visitId = medicalRecord.getVisitId();
            }
        }

        if (visitId != null) {
            var visit = visitRepository.findById(visitId).orElse(null);
            if (visit != null) {
                visitCode = visit.getVisitCode();
                patientId = visit.getPatientId();

                if (visit.getDoctorId() != null) {
                    doctorName = userRepository.findById(visit.getDoctorId())
                            .map(user -> user.getFullName())
                            .orElse(null);
                }
            }
        }

        if (patientId != null) {
            var patient = patientRepository.findById(patientId).orElse(null);
            if (patient != null) {
                patientCode = patient.getPatientCode();
                patientName = patient.getFullName();
            }
        }

        if (doctorName == null && prescribedBy != null) {
            doctorName = userRepository.findById(prescribedBy)
                    .map(user -> user.getFullName())
                    .orElse(null);
        }

        return new PrescriptionDisplayContext(
                visitId,
                visitCode,
                patientId,
                patientCode,
                patientName,
                doctorName
        );
    }

    public record PrescriptionDisplayContext(
            UUID visitId,
            String visitCode,
            UUID patientId,
            String patientCode,
            String patientName,
            String doctorName
    ) {
    }
}
