package com.benhsoan.application.ucservice.medicalrecord;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.DiagnosisCatalogResult;
import com.benhsoan.port.inbound.medicalrecord.UpdateDiagnosisCatalogStatusUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateDiagnosisCatalogStatusService implements UpdateDiagnosisCatalogStatusUseCase {

    private final DiagnosisCatalogRepository diagnosisCatalogRepository;
    private final ClockPort clockPort;
    private final DiagnosisCatalogResultMapper resultMapper;

    @Override
    public DiagnosisCatalogResult updateStatus(UUID diagnosisCatalogId, boolean active) {
        if (diagnosisCatalogId == null) {
            throw new ValidationException("Diagnosis catalog id is required.");
        }

        DiagnosisCatalog catalog = diagnosisCatalogRepository.findById(diagnosisCatalogId)
                .orElseThrow(() -> new DiagnosisCatalogNotFoundException(diagnosisCatalogId));
        if (catalog.isActive() != active) {
            if (active) {
                catalog.activate(clockPort.now());
            } else {
                catalog.deactivate(clockPort.now());
            }
            catalog = diagnosisCatalogRepository.save(catalog);
        }
        return resultMapper.toResult(catalog);
    }
}
