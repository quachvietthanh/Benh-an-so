package com.benhsoan.application.ucservice.medicalrecord;

import com.benhsoan.application.ucservice.auditlog.AdminOperationAuditService;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.medicalrecord.UpdateDiagnosisCatalogCommand;
import com.benhsoan.port.dto.result.DiagnosisCatalogResult;
import com.benhsoan.port.inbound.medicalrecord.UpdateDiagnosisCatalogUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateDiagnosisCatalogService implements UpdateDiagnosisCatalogUseCase {

    private final DiagnosisCatalogRepository diagnosisCatalogRepository;
    private final ClockPort clockPort;
    private final DiagnosisCatalogResultMapper resultMapper;
    private final AdminOperationAuditService adminOperationAuditService;
    private final CurrentUserPort currentUserPort;

    @Override
    public DiagnosisCatalogResult update(UpdateDiagnosisCatalogCommand command) {
        if (command == null || command.diagnosisCatalogId() == null) {
            throw new ValidationException("Diagnosis catalog id is required.");
        }

        DiagnosisCatalog catalog = diagnosisCatalogRepository.findById(command.diagnosisCatalogId())
                .orElseThrow(() -> new DiagnosisCatalogNotFoundException(command.diagnosisCatalogId()));

        String beforeName = catalog.getName();
        String beforeAbbreviation = catalog.getAbbreviation();
        String beforeDiseaseGroup = catalog.getDiseaseGroup();
        String beforeDescription = catalog.getDescription();

        Instant now = clockPort.now();
        catalog.updateInformation(command.name(), command.abbreviation(), command.diseaseGroup(), command.description(), now);
        DiagnosisCatalog saved = diagnosisCatalogRepository.save(catalog);

        adminOperationAuditService.record(
                currentUserPort.getCurrentUserId(),
                ActionType.UPDATE,
                ResourceType.DIAGNOSIS_CATALOG,
                saved.getId(),
                AdminOperationAuditService.fields(
                        "code", saved.getCode(),
                        "name", beforeName,
                        "abbreviation", beforeAbbreviation,
                        "diseaseGroup", beforeDiseaseGroup,
                        "description", beforeDescription,
                        "active", saved.isActive()
                ),
                AdminOperationAuditService.fields(
                        "code", saved.getCode(),
                        "name", saved.getName(),
                        "abbreviation", saved.getAbbreviation(),
                        "diseaseGroup", saved.getDiseaseGroup(),
                        "description", saved.getDescription(),
                        "active", saved.isActive()
                ),
                now
        );

        return resultMapper.toResult(saved);
    }
}
