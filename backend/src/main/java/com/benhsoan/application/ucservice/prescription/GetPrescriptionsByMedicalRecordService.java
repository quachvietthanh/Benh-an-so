package com.benhsoan.application.ucservice.prescription;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.result.PrescriptionResult;
import com.benhsoan.port.inbound.prescription.GetPrescriptionsByMedicalRecordUseCase;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionWarningLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPrescriptionsByMedicalRecordService
        implements GetPrescriptionsByMedicalRecordUseCase {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionWarningLogRepository warningLogRepository;
    private final PrescriptionReadAccessValidator accessValidator;
    private final PrescriptionResultMapper resultMapper;

    @Override
    public List<PrescriptionResult> getByMedicalRecordId(UUID medicalRecordId) {
        return prescriptionRepository.findByMedicalRecordId(medicalRecordId).stream()
                .peek(accessValidator::requireCanRead)
                .map(prescription -> resultMapper.toResult(
                        prescription,
                        warningLogRepository.findByPrescriptionId(prescription.getId())
                ))
                .toList();
    }
}
