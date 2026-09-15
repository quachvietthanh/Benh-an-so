package com.benhsoan.domain.patient.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PatientAlreadyMergedException extends PatientException {

    private final UUID mergedIntoPatientId;

    public PatientAlreadyMergedException(UUID patientId) {
        super(DomainErrorCode.PATIENT_ALREADY_MERGED,
                "Hồ sơ bệnh nhân " + patientId + " đã được gộp trước đó và chỉ có quyền xem tra cứu.");
        this.mergedIntoPatientId = null;
    }

    public PatientAlreadyMergedException(UUID patientId, UUID mergedIntoPatientId) {
        super(DomainErrorCode.PATIENT_ALREADY_MERGED,
                "Hồ sơ bệnh nhân đã được gộp sang hồ sơ " + mergedIntoPatientId + " và chỉ có quyền xem tra cứu.");
        this.mergedIntoPatientId = mergedIntoPatientId;
    }

    public PatientAlreadyMergedException(String patientCode, UUID mergedIntoPatientId) {
        super(DomainErrorCode.PATIENT_ALREADY_MERGED,
                "Hồ sơ bệnh nhân " + patientCode + " đã được gộp sang hồ sơ " + mergedIntoPatientId + " và chỉ có quyền xem tra cứu.");
        this.mergedIntoPatientId = mergedIntoPatientId;
    }

    public UUID getMergedIntoPatientId() {
        return mergedIntoPatientId;
    }
}
