package com.benhsoan.application.ucservice.clinical;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.clinical.ClinicalReferenceRange;
import com.benhsoan.domain.clinical.exception.ClinicalReferenceRangeNotFoundException;
import com.benhsoan.domain.clinical.exception.ClinicalReferenceRangeOverlapException;
import com.benhsoan.domain.clinical.exception.ClinicalServiceCatalogNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.clinical.CreateClinicalReferenceRangeCommand;
import com.benhsoan.port.dto.command.clinical.UpdateClinicalReferenceRangeCommand;
import com.benhsoan.port.dto.result.ClinicalReferenceRangeResult;
import com.benhsoan.port.inbound.clinical.CreateClinicalReferenceRangeUseCase;
import com.benhsoan.port.inbound.clinical.GetClinicalReferenceRangesUseCase;
import com.benhsoan.port.inbound.clinical.UpdateClinicalReferenceRangeStatusUseCase;
import com.benhsoan.port.inbound.clinical.UpdateClinicalReferenceRangeUseCase;
import com.benhsoan.port.outbound.repository.clinical.ClinicalReferenceRangeRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalServiceCatalogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ClinicalReferenceRangeService implements CreateClinicalReferenceRangeUseCase,
        UpdateClinicalReferenceRangeUseCase, UpdateClinicalReferenceRangeStatusUseCase, GetClinicalReferenceRangesUseCase {

    private final ClinicalServiceCatalogRepository serviceRepository;
    private final ClinicalReferenceRangeRepository referenceRangeRepository;
    private final ClinicalServiceAuditService auditService;
    private final ClinicalServiceManagementResultMapper resultMapper;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public ClinicalReferenceRangeResult create(UUID clinicalServiceId, CreateClinicalReferenceRangeCommand command) {
        requireServiceForUpdate(clinicalServiceId);
        if (command == null) {
            throw new ValidationException("Create clinical reference range command is required.");
        }
        Instant now = clockPort.now();
        ClinicalReferenceRange range = ClinicalReferenceRange.create(
                clinicalServiceId, command.gender(), command.minAge(), command.maxAge(),
                command.lowerBound(), command.upperBound(), now
        );
        ensureNoOverlap(range, null, clinicalServiceId);

        ClinicalReferenceRange saved = referenceRangeRepository.save(range);
        auditService.record(currentUserPort.getCurrentUserId(), ActionType.CREATE, clinicalServiceId,
                "Reference range created: " + saved.getId(), now);
        return resultMapper.toRangeResult(saved);
    }
    @Override
    public ClinicalReferenceRangeResult update(UUID clinicalServiceId, UUID referenceRangeId,
            UpdateClinicalReferenceRangeCommand command) {
        requireServiceForUpdate(clinicalServiceId);
        if (command == null) {
            throw new ValidationException("Update clinical reference range command is required.");
        }
        ClinicalReferenceRange range = requireRange(clinicalServiceId, referenceRangeId);
        Instant now = clockPort.now();
        range.updateInformation(command.gender(), command.minAge(), command.maxAge(),
                command.lowerBound(), command.upperBound(), now);
        if (range.isActive()) {
            ensureNoOverlap(range, range.getId(), clinicalServiceId);
        }

        ClinicalReferenceRange saved = referenceRangeRepository.save(range);
        auditService.record(currentUserPort.getCurrentUserId(), ActionType.UPDATE, clinicalServiceId,
                "Reference range updated: " + saved.getId(), now);
        return resultMapper.toRangeResult(saved);
    }

    @Override
    public ClinicalReferenceRangeResult updateStatus(UUID clinicalServiceId, UUID referenceRangeId, boolean active) {
        requireServiceForUpdate(clinicalServiceId);
        ClinicalReferenceRange range = requireRange(clinicalServiceId, referenceRangeId);
        if (range.isActive() == active) {
            return resultMapper.toRangeResult(range);
        }
        Instant now = clockPort.now();
        if (active) {
            ensureNoOverlap(range, range.getId(), clinicalServiceId);
            range.activate(now);
        } else {
            range.deactivate(now);
        }
        ClinicalReferenceRange saved = referenceRangeRepository.save(range);
        auditService.record(currentUserPort.getCurrentUserId(),
                active ? ActionType.ACTIVATE : ActionType.DEACTIVATE, clinicalServiceId,
                "Reference range status changed: " + saved.getId(), now);
        return resultMapper.toRangeResult(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClinicalReferenceRangeResult> list(UUID clinicalServiceId) {
        requireService(clinicalServiceId);
        return referenceRangeRepository.findByClinicalServiceId(clinicalServiceId).stream()
                .map(resultMapper::toRangeResult).toList();
    }

    private void requireService(UUID clinicalServiceId) {
        if (clinicalServiceId == null) {
            throw new ValidationException("Clinical service id is required.");
        }
        serviceRepository.findById(clinicalServiceId)
                .orElseThrow(() -> new ClinicalServiceCatalogNotFoundException(clinicalServiceId));
    }

    private void requireServiceForUpdate(UUID clinicalServiceId) {
        if (clinicalServiceId == null) {
            throw new ValidationException("Clinical service id is required.");
        }
        serviceRepository.findByIdForUpdate(clinicalServiceId)
                .orElseThrow(() -> new ClinicalServiceCatalogNotFoundException(clinicalServiceId));
    }

    private ClinicalReferenceRange requireRange(UUID clinicalServiceId, UUID referenceRangeId) {
        if (referenceRangeId == null) {
            throw new ValidationException("Reference range id is required.");
        }
        ClinicalReferenceRange range = referenceRangeRepository.findById(referenceRangeId)
                .orElseThrow(() -> new ClinicalReferenceRangeNotFoundException(referenceRangeId));
        if (!range.getClinicalServiceId().equals(clinicalServiceId)) {
            throw new ValidationException("Reference range does not belong to the clinical service.");
        }
        return range;
    }

    private void ensureNoOverlap(ClinicalReferenceRange candidate, UUID excludeId, UUID clinicalServiceId) {
        for (ClinicalReferenceRange existing : referenceRangeRepository.findActiveByClinicalServiceId(clinicalServiceId)) {
            if (excludeId != null && existing.getId().equals(excludeId)) {
                continue;
            }
            if (candidate.overlaps(existing)) {
                throw new ClinicalReferenceRangeOverlapException(
                        "Reference range overlaps an existing active range for the same gender and age group.");
            }
        }
    }
}
