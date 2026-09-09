package com.benhsoan.persistence.adapterRepository.medicalrecord;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.persistence.jpaRepository.clinical.JpaClinicalOrderItemRepository;
import com.benhsoan.persistence.jpaRepository.clinical.JpaClinicalOrderRepository;
import com.benhsoan.persistence.jpaRepository.clinical.JpaClinicalResultHistoryRepository;
import com.benhsoan.persistence.jpaRepository.clinical.JpaClinicalResultRepository;
import com.benhsoan.persistence.jpaRepository.clinical.JpaMedicalAttachmentRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordAccessLogRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordAmendmentRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordDiagnosisRepository;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionAllergyWarningLogRepository;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionAmendmentRepository;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionDispenseItemRepository;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionItemRepository;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionRepository;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionWarningLogRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicalRecordCascadeDeleter Unit Tests")
class MedicalRecordCascadeDeleterTest {

    @Mock private JpaMedicalRecordAccessLogRepository accessLogRepository;
    @Mock private JpaMedicalRecordAmendmentRepository amendmentRepository;
    @Mock private JpaMedicalRecordDiagnosisRepository diagnosisRepository;
    @Mock private JpaMedicalAttachmentRepository attachmentRepository;
    @Mock private JpaClinicalOrderRepository clinicalOrderRepository;
    @Mock private JpaClinicalOrderItemRepository clinicalOrderItemRepository;
    @Mock private JpaClinicalResultRepository clinicalResultRepository;
    @Mock private JpaClinicalResultHistoryRepository clinicalResultHistoryRepository;
    @Mock private JpaPrescriptionRepository prescriptionRepository;
    @Mock private JpaPrescriptionItemRepository prescriptionItemRepository;
    @Mock private JpaPrescriptionAmendmentRepository prescriptionAmendmentRepository;
    @Mock private JpaPrescriptionWarningLogRepository prescriptionWarningLogRepository;
    @Mock private JpaPrescriptionAllergyWarningLogRepository prescriptionAllergyWarningLogRepository;
    @Mock private JpaPrescriptionDispenseItemRepository prescriptionDispenseItemRepository;

    @InjectMocks
    private MedicalRecordCascadeDeleter deleter;

    @Test
    @DisplayName("Deletes all prescription child rows including allergy warning logs when prescriptions exist")
    void deletesPrescriptionAllergyWarningLogsWhenPrescriptionsExist() {
        UUID medicalRecordId = UUID.randomUUID();
        UUID prescriptionId1 = UUID.randomUUID();
        UUID prescriptionId2 = UUID.randomUUID();
        List<UUID> prescriptionIds = List.of(prescriptionId1, prescriptionId2);

        when(clinicalOrderRepository.findIdsByMedicalRecordId(medicalRecordId)).thenReturn(List.of());
        when(prescriptionRepository.findIdsByMedicalRecordId(medicalRecordId)).thenReturn(prescriptionIds);

        deleter.deleteByMedicalRecordId(medicalRecordId);

        verify(prescriptionDispenseItemRepository).deleteByPrescriptionIdIn(prescriptionIds);
        verify(prescriptionAmendmentRepository).deleteByPrescriptionIdIn(prescriptionIds);
        verify(prescriptionWarningLogRepository).deleteByPrescriptionIdIn(prescriptionIds);
        verify(prescriptionAllergyWarningLogRepository).deleteByPrescriptionIdIn(prescriptionIds);
        verify(prescriptionItemRepository).deleteAllByPrescriptionIdIn(prescriptionIds);
        verify(prescriptionRepository).deleteByMedicalRecordId(medicalRecordId);
    }
}
