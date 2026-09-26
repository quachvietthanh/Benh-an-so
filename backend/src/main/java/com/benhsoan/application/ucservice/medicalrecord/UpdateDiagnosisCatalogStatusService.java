package com.benhsoan.application.ucservice.medicalrecord;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.auditlog.AdminOperationAuditService;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.DiagnosisCatalogResult;
import com.benhsoan.port.inbound.medicalrecord.UpdateDiagnosisCatalogStatusUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateDiagnosisCatalogStatusService implements UpdateDiagnosisCatalogStatusUseCase {

    private final DiagnosisCatalogRepository diagnosisCatalogRepository;
    private final ClockPort clockPort;
    private final DiagnosisCatalogResultMapper resultMapper;
    private final AdminOperationAuditService adminOperationAuditService;
    private final CurrentUserPort currentUserPort;

    @Override
    public DiagnosisCatalogResult updateStatus(UUID diagnosisCatalogId, boolean active) {
        if (diagnosisCatalogId == null) {
            throw new ValidationException("Diagnosis catalog id is required.");
        }

        DiagnosisCatalog catalog = diagnosisCatalogRepository.findById(diagnosisCatalogId)
                .orElseThrow(() -> new DiagnosisCatalogNotFoundException(diagnosisCatalogId));
        if (catalog.isActive() != active) {
            boolean beforeActive = catalog.isActive();
            Instant now = clockPort.now();
            if (active) {
                catalog.activate(now);
            } else {
                catalog.deactivate(now);
            }
            catalog = diagnosisCatalogRepository.save(catalog);

            adminOperationAuditService.record(
                    currentUserPort.getCurrentUserId(),
                    active ? ActionType.ACTIVATE : ActionType.DEACTIVATE,
                    ResourceType.DIAGNOSIS_CATALOG,
                    catalog.getId(),
                    AdminOperationAuditService.fields("code", catalog.getCode(), "name", catalog.getName(), "active", beforeActive),
                    AdminOperationAuditService.fields("code", catalog.getCode(), "name", catalog.getName(), "active", catalog.isActive()),
                    now
            );
        }
        return resultMapper.toResult(catalog);
    }
}
