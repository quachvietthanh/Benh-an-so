package com.benhsoan.port.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Standard data exchange document for a signed medical record (NCL-11-CN-007).
 * Encapsulates the 5 required blocks: Administrative, Diagnosis with ICD codes,
 * Clinical Orders, Clinical Results, and Prescriptions.
 */
public record MedicalRecordExchangeDocument(
        String exchangeVersion,
        Instant generatedAt,
        FacilityInfo facility,
        PatientInfo patient,
        EncounterInfo encounter,
        ClinicalRecordInfo clinicalRecord,
        List<DiagnosisItem> diagnoses,
        List<ClinicalOrderItem> clinicalOrders,
        List<ClinicalResultItem> clinicalResults,
        List<PrescriptionItemDocument> prescriptions
) {
    public MedicalRecordExchangeDocument {
        diagnoses = diagnoses == null ? List.of() : List.copyOf(diagnoses);
        clinicalOrders = clinicalOrders == null ? List.of() : List.copyOf(clinicalOrders);
        clinicalResults = clinicalResults == null ? List.of() : List.copyOf(clinicalResults);
        prescriptions = prescriptions == null ? List.of() : List.copyOf(prescriptions);
    }

    public record FacilityInfo(
            String clinicName,
            String address,
            String phone,
            String email
    ) {}

    public record PatientInfo(
            UUID patientId,
            String patientCode,
            String fullName,
            String dateOfBirth,
            String gender,
            String phone,
            String identityNumber,
            String insuranceNumber,
            String address
    ) {}

    public record EncounterInfo(
            UUID visitId,
            String visitCode,
            Instant visitAt,
            Instant startedAt,
            Instant completedAt,
            String visitType,
            String reason,
            UUID doctorId,
            String doctorName
    ) {}

    public record ClinicalRecordInfo(
            UUID medicalRecordId,
            String status,
            Instant signedAt,
            UUID signedByDoctorId,
            String signedByDoctorName,
            String signatureData,
            String chiefComplaint,
            String symptoms,
            String medicalHistory,
            String physicalExamination,
            String clinicalProgress,
            String treatmentPlan,
            String doctorInstructions,
            String conclusion,
            String revisitDate
    ) {}

    public record DiagnosisItem(
            String diagnosisType,
            String diagnosisCode,
            String diagnosisName,
            String note,
            Instant diagnosedAt
    ) {}

    public record ClinicalOrderItem(
            UUID orderId,
            String orderCode,
            Instant orderedAt,
            List<ClinicalOrderServiceItem> services
    ) {
        public ClinicalOrderItem {
            services = services == null ? List.of() : List.copyOf(services);
        }
    }

    public record ClinicalOrderServiceItem(
            UUID itemId,
            String serviceCode,
            String serviceName,
            String serviceType,
            String instruction,
            String status
    ) {}

    public record ClinicalResultItem(
            UUID resultId,
            String serviceCode,
            String serviceName,
            String resultType,
            BigDecimal numericValue,
            String textValue,
            String unit,
            String referenceRange,
            String abnormalFlag,
            String conclusion,
            String status
    ) {}

    public record PrescriptionItemDocument(
            UUID prescriptionId,
            String prescriptionCode,
            String status,
            Instant prescribedAt,
            String interconnectionReceiptCode,
            String note,
            List<PrescriptionMedicationItem> medications
    ) {
        public PrescriptionItemDocument {
            medications = medications == null ? List.of() : List.copyOf(medications);
        }
    }

    public record PrescriptionMedicationItem(
            String medicineCode,
            String medicineName,
            String activeIngredient,
            String strength,
            String dosage,
            Integer frequency,
            String route,
            Integer quantity,
            String unit,
            String usageInstructions,
            Integer daysSupply
    ) {}
}
