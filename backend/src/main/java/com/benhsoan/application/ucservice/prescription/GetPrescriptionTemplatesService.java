package com.benhsoan.application.ucservice.prescription;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.port.dto.result.PrescriptionTemplateResult;
import com.benhsoan.port.inbound.prescription.GetPrescriptionTemplatesUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionTemplateRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

/** NCL-05-CN-008: lists the calling doctor's own templates for a diagnosis code. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPrescriptionTemplatesService implements GetPrescriptionTemplatesUseCase {

    private final PrescriptionTemplateRepository templateRepository;
    private final DiagnosisCatalogRepository diagnosisCatalogRepository;
    private final PrescriptionTemplateResultMapper resultMapper;
    private final CurrentUserPort currentUserPort;

    @Override
    public List<PrescriptionTemplateResult> getByDiagnosisCode(String diagnosisCode) {
        authorizeDoctor();
        UUID currentUserId = currentUserPort.getCurrentUserId();

        DiagnosisCatalog diagnosis = diagnosisCatalogRepository.findByCode(diagnosisCode)
                .orElse(null);
        if (diagnosis == null) {
            return List.of();
        }

        return templateRepository
                .findByDiagnosisCatalogIdAndCreatedBy(diagnosis.getId(), currentUserId)
                .stream()
                .map(resultMapper::toResult)
                .toList();
    }

    private void authorizeDoctor() {
        if (!currentUserPort.hasRole("DOCTOR")) {
            throw new AccessDeniedException("Only doctors are allowed to list prescription templates.");
        }
    }
}
