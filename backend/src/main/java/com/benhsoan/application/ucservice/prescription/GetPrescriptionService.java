package com.benhsoan.application.ucservice.prescription;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.prescription.exception.PrescriptionNotFoundException;
import com.benhsoan.port.dto.result.PrescriptionResult;
import com.benhsoan.port.inbound.prescription.GetPrescriptionUseCase;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionWarningLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPrescriptionService implements GetPrescriptionUseCase {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionWarningLogRepository warningLogRepository;
    private final PrescriptionReadAccessValidator accessValidator;
    private final PrescriptionResultMapper resultMapper;

    @Override
    public PrescriptionResult getById(UUID prescriptionId) {
        var prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new PrescriptionNotFoundException(prescriptionId));
        accessValidator.requireCanRead(prescription);
        return resultMapper.toResult(
                prescription,
                warningLogRepository.findByPrescriptionId(prescriptionId)
        );
    }
}
