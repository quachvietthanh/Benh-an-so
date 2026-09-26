package com.benhsoan.application.ucservice.medicalrecord;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.auditlog.AdminOperationAuditService;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogCodeAlreadyExistsException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.medicalrecord.CreateDiagnosisCatalogCommand;
import com.benhsoan.port.dto.result.DiagnosisCatalogResult;
import com.benhsoan.port.inbound.medicalrecord.CreateDiagnosisCatalogUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateDiagnosisCatalogService implements CreateDiagnosisCatalogUseCase {

    private final DiagnosisCatalogRepository diagnosisCatalogRepository;
    private final ClockPort clockPort;
    private final DiagnosisCatalogResultMapper resultMapper;
    private final AdminOperationAuditService adminOperationAuditService;
    private final CurrentUserPort currentUserPort;

    @Override
    public DiagnosisCatalogResult create(CreateDiagnosisCatalogCommand command) {
        if (command == null) {
            throw new ValidationException("Create diagnosis catalog command is required.");
        }

        Instant now = clockPort.now();
        DiagnosisCatalog catalog = DiagnosisCatalog.create(
                UUID.randomUUID(), command.code(), command.name(), command.abbreviation(),
                command.diseaseGroup(), command.description(), now
        );
        if (diagnosisCatalogRepository.existsByCode(catalog.getCode())) {
            throw new DiagnosisCatalogCodeAlreadyExistsException(catalog.getCode());
        }

        DiagnosisCatalog saved;
        try {
            saved = diagnosisCatalogRepository.save(catalog);
        } catch (DataIntegrityViolationException exception) {
            throw new DiagnosisCatalogCodeAlreadyExistsException(catalog.getCode());
        }

        adminOperationAuditService.record(
                currentUserPort.getCurrentUserId(),
                ActionType.CREATE,
                ResourceType.DIAGNOSIS_CATALOG,
                saved.getId(),
                null,
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
