package com.benhsoan.application.ucservice.clinical;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.clinical.ClinicalServiceCatalog;
import com.benhsoan.domain.clinical.exception.ClinicalServiceCatalogNotFoundException;
import com.benhsoan.domain.clinical.exception.ClinicalServiceCodeAlreadyExistsException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.clinical.CreateClinicalServiceCommand;
import com.benhsoan.port.dto.command.clinical.UpdateClinicalServiceCommand;
import com.benhsoan.port.dto.result.ClinicalServiceManagementResult;
import com.benhsoan.port.inbound.clinical.CreateClinicalServiceUseCase;
import com.benhsoan.port.inbound.clinical.GetClinicalServicesUseCase;
import com.benhsoan.port.inbound.clinical.UpdateClinicalServiceStatusUseCase;
import com.benhsoan.port.inbound.clinical.UpdateClinicalServiceUseCase;
import com.benhsoan.port.outbound.repository.clinical.ClinicalReferenceRangeRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalServiceCatalogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ClinicalServiceManagementService implements CreateClinicalServiceUseCase, UpdateClinicalServiceUseCase,
        UpdateClinicalServiceStatusUseCase, GetClinicalServicesUseCase {

    private final ClinicalServiceCatalogRepository serviceRepository;
    private final ClinicalReferenceRangeRepository referenceRangeRepository;
    private final ClinicalServiceAuditService auditService;
    private final ClinicalServiceManagementResultMapper resultMapper;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public ClinicalServiceManagementResult create(CreateClinicalServiceCommand command) {
        if (command == null) {
            throw new ValidationException("Create clinical service command is required.");
        }
        if (command.serviceCatalogId() == null) {
            throw new ValidationException("Service catalog id is required.");
        }
        if (serviceRepository.existsByServiceCode(command.serviceCode())) {
            throw new ClinicalServiceCodeAlreadyExistsException(command.serviceCode());
        }

        Instant now = clockPort.now();
        UUID actorId = currentUserPort.getCurrentUserId();
        ClinicalServiceCatalog catalog = ClinicalServiceCatalog.create(
                command.serviceCatalogId(), command.serviceCode(), command.serviceName(), command.serviceType(),
                command.resultDataType(), command.unit(), command.referenceRange(), command.description(), now
        );

        ClinicalServiceCatalog saved = serviceRepository.save(catalog);
        auditService.record(actorId, ActionType.CREATE, saved.getId(),
                "Clinical service created: " + saved.getServiceCode(), now);
        return resultMapper.toResult(saved, List.of());
    }
    @Override
    public ClinicalServiceManagementResult update(UUID clinicalServiceId, UpdateClinicalServiceCommand command) {
        if (clinicalServiceId == null) {
            throw new ValidationException("Clinical service id is required.");
        }
        if (command == null) {
            throw new ValidationException("Update clinical service command is required.");
        }

        ClinicalServiceCatalog catalog = requireCatalog(clinicalServiceId);
        Instant now = clockPort.now();
        catalog.updateInformation(command.serviceName(), command.serviceType(), command.resultDataType(),
                command.unit(), command.referenceRange(), command.description(), now);

        ClinicalServiceCatalog saved = serviceRepository.save(catalog);
        auditService.record(currentUserPort.getCurrentUserId(), ActionType.UPDATE, saved.getId(),
                "Clinical service updated: " + saved.getServiceCode(), now);
        return resultMapper.toResult(saved, referenceRangeRepository.findByClinicalServiceId(saved.getId()));
    }

    @Override
    public ClinicalServiceManagementResult updateStatus(UUID clinicalServiceId, boolean active) {
        if (clinicalServiceId == null) {
            throw new ValidationException("Clinical service id is required.");
        }

        ClinicalServiceCatalog catalog = requireCatalog(clinicalServiceId);
        if (catalog.isActive() == active) {
            return resultMapper.toResult(catalog, referenceRangeRepository.findByClinicalServiceId(catalog.getId()));
        }

        Instant now = clockPort.now();
        if (active) {
            catalog.activate(now);
        } else {
            catalog.deactivate(now);
        }
        ClinicalServiceCatalog saved = serviceRepository.save(catalog);
        auditService.record(currentUserPort.getCurrentUserId(),
                active ? ActionType.ACTIVATE : ActionType.DEACTIVATE, saved.getId(),
                "Clinical service status changed: " + saved.getServiceCode(), now);
        return resultMapper.toResult(saved, referenceRangeRepository.findByClinicalServiceId(saved.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClinicalServiceManagementResult> search(String keyword, Boolean active, Pageable pageable) {
        return serviceRepository.search(keyword, active, pageable)
                .map(service -> resultMapper.toResult(service,
                        referenceRangeRepository.findByClinicalServiceId(service.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public ClinicalServiceManagementResult getById(UUID clinicalServiceId) {
        if (clinicalServiceId == null) {
            throw new ValidationException("Clinical service id is required.");
        }
        ClinicalServiceCatalog catalog = requireCatalog(clinicalServiceId);
        return resultMapper.toResult(catalog, referenceRangeRepository.findByClinicalServiceId(catalog.getId()));
    }

    private ClinicalServiceCatalog requireCatalog(UUID clinicalServiceId) {
        return serviceRepository.findById(clinicalServiceId)
                .orElseThrow(() -> new ClinicalServiceCatalogNotFoundException(clinicalServiceId));
    }
}
