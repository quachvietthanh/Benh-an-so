package com.benhsoan.application.ucservice.clinical;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.clinical.ClinicalResult;
import com.benhsoan.domain.clinical.ClinicalResultHistory;
import com.benhsoan.domain.clinical.MedicalAttachment;
import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.domain.clinical.enums.ClinicalOrderStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultType;
import com.benhsoan.domain.clinical.exception.ClinicalOrderInvalidVisitException;
import com.benhsoan.domain.clinical.exception.ClinicalOrderLockedMedicalRecordException;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.clinical.EnterClinicalResultCommand;
import com.benhsoan.port.dto.command.clinical.GetClinicalResultsByVisitQuery;
import com.benhsoan.port.dto.command.clinical.UpdateClinicalResultCommand;
import com.benhsoan.port.dto.result.ClinicalResultResult;
import com.benhsoan.port.inbound.clinical.EnterClinicalResultUseCase;
import com.benhsoan.port.inbound.clinical.FinalizeClinicalResultUseCase;
import com.benhsoan.port.inbound.clinical.GetClinicalResultHistoryUseCase;
import com.benhsoan.port.inbound.clinical.GetClinicalResultUseCase;
import com.benhsoan.port.inbound.clinical.GetClinicalResultsByVisitUseCase;
import com.benhsoan.port.inbound.clinical.UpdateClinicalResultUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.crudRepository.clinical.ClinicalOrderRepository;
import com.benhsoan.port.outbound.repository.crudRepository.clinical.ClinicalResultHistoryRepository;
import com.benhsoan.port.outbound.repository.crudRepository.clinical.ClinicalResultRepository;
import com.benhsoan.port.outbound.repository.crudRepository.clinical.ClinicalServiceCatalogRepository;
import com.benhsoan.port.outbound.repository.crudRepository.clinical.MedicalAttachmentRepository;
import com.benhsoan.port.outbound.repository.crudRepository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.crudRepository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ClinicalResultService implements EnterClinicalResultUseCase, UpdateClinicalResultUseCase,
        FinalizeClinicalResultUseCase, GetClinicalResultUseCase, GetClinicalResultsByVisitUseCase,
        GetClinicalResultHistoryUseCase {

    private final ClinicalOrderItemRepository clinicalOrderItemRepository;
    private final ClinicalOrderRepository clinicalOrderRepository;
    private final ClinicalServiceCatalogRepository clinicalServiceCatalogRepository;
    private final ClinicalResultRepository clinicalResultRepository;
    private final ClinicalResultHistoryRepository clinicalResultHistoryRepository;
    private final MedicalAttachmentRepository medicalAttachmentRepository;
    private final VisitRepository visitRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final ClinicalOrderAuthorizationService authorizationService;
    private final ClinicalResultAuditService auditService;
    private final ClockPort clock;

    @Override
    public ClinicalResultResult enter(UUID clinicalOrderItemId, EnterClinicalResultCommand command) {
        UUID actorId = authorizationService.requireWriteAccess();
        var item = clinicalOrderItemRepository.findById(clinicalOrderItemId)
                .orElseThrow(() -> new ValidationException("Clinical order item not found."));
        if (item.getStatus() != ClinicalOrderItemStatus.PENDING
                || clinicalResultRepository.findByClinicalOrderItemId(clinicalOrderItemId).isPresent()) {
            throw new ValidationException("Clinical order item cannot receive a result.");
        }

        var order = clinicalOrderRepository.findById(item.getClinicalOrderId())
                .orElseThrow(() -> new ValidationException("Clinical order not found."));
        var visit = visitRepository.findById(order.getVisitId())
                .orElseThrow(() -> new ValidationException("Visit not found."));
        if (!visit.isActive()) {
            throw new ClinicalOrderInvalidVisitException();
        }
        var medicalRecord = medicalRecordRepository.findByVisitId(visit.getId())
                .orElseThrow(() -> new ValidationException("Medical record not found."));
        if (medicalRecord.isLocked()) {
            throw new ClinicalOrderLockedMedicalRecordException();
        }
        var clinicalService = clinicalServiceCatalogRepository.findById(item.getClinicalServiceId())
                .orElseThrow(() -> new ValidationException("Clinical service not found."));

        ClinicalResultType resultType = ClinicalResultType.from(clinicalService.getResultDataType());

        Instant now = clock.now();
        ClinicalResult result = clinicalResultRepository.save(ClinicalResult.create(
                clinicalOrderItemId, visit.getId(), resultType, command.numericValue(), command.textValue(),
                clinicalService.getUnit(), clinicalService.getReferenceRange(), command.abnormalFlag(),
                command.conclusion(), actorId, now));
        auditService.recordWrite(result.getId(), visit.getPatientId(), visit.getId(), medicalRecord.getId(), actorId,
                MedicalRecordAccessAction.CREATE, now);
        return mapDetail(result);
    }

    @Override
    public ClinicalResultResult update(UUID clinicalResultId, UpdateClinicalResultCommand command) {
        UUID actorId = authorizationService.requireWriteAccess();
        ClinicalResult result = findResult(clinicalResultId);
        ensureWritableVisitAndRecord(result.getVisitId());
        ClinicalResult previousResult = snapshot(result);
        Instant now = clock.now();

        result.updateResult(command.numericValue(), command.textValue(), result.getUnit(), result.getReferenceRange(),
                command.abnormalFlag(), command.conclusion(), actorId, now);
        ClinicalResult savedResult = clinicalResultRepository.save(result);
        clinicalResultHistoryRepository.save(ClinicalResultHistory.create(
                previousResult, savedResult, command.changeReason(), actorId, now));
        auditWrite(savedResult, actorId, MedicalRecordAccessAction.UPDATE, now);
        return mapDetail(savedResult);
    }

    @Override
    public ClinicalResultResult finalizeResult(UUID clinicalResultId) {
        UUID actorId = authorizationService.requireWriteAccess();
        ClinicalResult result = findResult(clinicalResultId);
        ensureWritableVisitAndRecord(result.getVisitId());
        if (result.getResultType().requiresAttachment()
                && !medicalAttachmentRepository.existsByClinicalResultId(result.getId())) {
            throw new ValidationException("File result requires an uploaded attachment before finalization.");
        }
        ClinicalResult previousResult = snapshot(result);
        Instant now = clock.now();

        result.finalizeResult(actorId, now);
        ClinicalResult savedResult = clinicalResultRepository.save(result);
        clinicalResultHistoryRepository.save(ClinicalResultHistory.create(
                previousResult, savedResult, "Clinical result finalized.", actorId, now));

        var item = clinicalOrderItemRepository.findById(savedResult.getClinicalOrderItemId())
                .orElseThrow(() -> new ValidationException("Clinical order item not found."));
        item.complete(now);
        clinicalOrderItemRepository.save(item);
        synchronizeOrder(item.getClinicalOrderId(), now);
        auditWrite(savedResult, actorId, MedicalRecordAccessAction.UPDATE, now);
        return mapDetail(savedResult);
    }

    @Override
    @Transactional(readOnly = true)
    public ClinicalResultResult getById(UUID clinicalResultId) {
        UUID actorId = authorizationService.requireReadAccess();
        ClinicalResult result = findResult(clinicalResultId);
        auditView(result, actorId);
        return mapDetail(result);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClinicalResultResult> getResultsByVisit(GetClinicalResultsByVisitQuery query) {
        authorizationService.requireReadAccess();
        visitRepository.findById(query.visitId()).orElseThrow(() -> new VisitNotFoundException(query.visitId()));
        Page<ClinicalResult> page = clinicalResultRepository.findByVisitId(
                query.visitId(), PageRequest.of(query.page(), query.size()));
        Map<UUID, List<MedicalAttachment>> attachmentsByResultId = medicalAttachmentRepository
                .findByClinicalResultIdIn(page.getContent().stream().map(ClinicalResult::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(MedicalAttachment::getClinicalResultId));
        auditViews(page.getContent());
        return page.map(result -> map(result, attachmentsByResultId.getOrDefault(result.getId(), List.of()), List.of()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClinicalResultResult.History> getHistory(UUID clinicalResultId) {
        UUID actorId = authorizationService.requireReadAccess();
        ClinicalResult result = findResult(clinicalResultId);
        auditView(result, actorId, MedicalRecordAccessAction.VIEW_HISTORY);
        return clinicalResultHistoryRepository.findByClinicalResultId(clinicalResultId).stream()
                .map(this::toHistoryResult)
                .toList();
    }

    private void synchronizeOrder(UUID clinicalOrderId, Instant now) {
        var order = clinicalOrderRepository.findById(clinicalOrderId)
                .orElseThrow(() -> new ValidationException("Clinical order not found."));
        if (order.getStatus() == ClinicalOrderStatus.ORDERED) {
            order.start(now);
        }
        boolean allCompleted = clinicalOrderItemRepository.findByClinicalOrderIdIn(List.of(order.getId())).stream()
                .allMatch(item -> item.getStatus() == ClinicalOrderItemStatus.COMPLETED);
        if (allCompleted) {
            order.complete(now);
        } else if (order.getStatus() == ClinicalOrderStatus.IN_PROGRESS) {
            order.markPartiallyCompleted(now);
        }
        clinicalOrderRepository.save(order);
    }

    private void auditWrite(ClinicalResult result, UUID actorId, MedicalRecordAccessAction action, Instant at) {
        var visit = visitRepository.findById(result.getVisitId())
                .orElseThrow(() -> new ValidationException("Visit not found."));
        var medicalRecord = medicalRecordRepository.findByVisitId(visit.getId())
                .orElseThrow(() -> new ValidationException("Medical record not found."));
        auditService.recordWrite(result.getId(), visit.getPatientId(), visit.getId(), medicalRecord.getId(), actorId, action, at);
    }

    private void ensureWritableVisitAndRecord(UUID visitId) {
        var visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ValidationException("Visit not found."));
        if (!visit.isActive()) {
            throw new ClinicalOrderInvalidVisitException();
        }
        var medicalRecord = medicalRecordRepository.findByVisitId(visitId)
                .orElseThrow(() -> new ValidationException("Medical record not found."));
        if (medicalRecord.isLocked()) {
            throw new ClinicalOrderLockedMedicalRecordException();
        }
    }

    private void auditView(ClinicalResult result, UUID actorId) {
        auditView(result, actorId, MedicalRecordAccessAction.VIEW);
    }

    private void auditView(ClinicalResult result, UUID actorId, MedicalRecordAccessAction action) {
        var visit = visitRepository.findById(result.getVisitId())
                .orElseThrow(() -> new ValidationException("Visit not found."));
        var medicalRecord = medicalRecordRepository.findByVisitId(visit.getId())
                .orElseThrow(() -> new ValidationException("Medical record not found."));
        auditService.recordView(result.getId(), visit.getPatientId(), visit.getId(), medicalRecord.getId(), actorId,
                action, clock.now());
    }

    private void auditViews(List<ClinicalResult> results) {
        if (results.isEmpty()) {
            return;
        }
        UUID actorId = authorizationService.requireReadAccess();
        var visit = visitRepository.findById(results.getFirst().getVisitId())
                .orElseThrow(() -> new ValidationException("Visit not found."));
        var medicalRecord = medicalRecordRepository.findByVisitId(visit.getId())
                .orElseThrow(() -> new ValidationException("Medical record not found."));
        Instant now = clock.now();
        for (ClinicalResult result : results) {
            auditService.recordView(result.getId(), visit.getPatientId(), visit.getId(), medicalRecord.getId(), actorId,
                    MedicalRecordAccessAction.VIEW, now);
        }
    }

    private ClinicalResult findResult(UUID clinicalResultId) {
        return clinicalResultRepository.findById(clinicalResultId)
                .orElseThrow(() -> new ValidationException("Clinical result not found."));
    }

    private ClinicalResult snapshot(ClinicalResult result) {
        return ClinicalResult.restore(result.getId(), result.getClinicalOrderItemId(), result.getVisitId(),
                result.getResultType(), result.getNumericValue(), result.getTextValue(), result.getUnit(),
                result.getReferenceRange(), result.getAbnormalFlag(), result.getConclusion(), result.getStatus(),
                result.getEnteredBy(), result.getEnteredAt(), result.getUpdatedBy(), result.getUpdatedAt());
    }

    private ClinicalResultResult mapDetail(ClinicalResult result) {
        return map(result, medicalAttachmentRepository.findByClinicalResultId(result.getId()),
                clinicalResultHistoryRepository.findByClinicalResultId(result.getId()));
    }

    private ClinicalResultResult map(ClinicalResult result, List<MedicalAttachment> attachments,
            List<ClinicalResultHistory> histories) {
        List<ClinicalResultResult.Attachment> attachmentResults = attachments.stream()
                .map(attachment -> new ClinicalResultResult.Attachment(attachment.getId(), attachment.getFileName(),
                        attachment.getContentType(), attachment.getFileSize(), attachment.getAttachmentType()))
                .toList();
        List<ClinicalResultResult.History> historyResults = histories.stream()
                .map(this::toHistoryResult)
                .toList();
        return new ClinicalResultResult(result.getId(), result.getClinicalOrderItemId(), result.getVisitId(),
                result.getResultType(), result.getNumericValue(), result.getTextValue(), result.getUnit(),
                result.getReferenceRange(), result.getAbnormalFlag(), result.getConclusion(), result.getStatus(),
                attachmentResults, historyResults);
    }

    private ClinicalResultResult.History toHistoryResult(ClinicalResultHistory history) {
        return new ClinicalResultResult.History(history.getId(), history.getOldResultType(), history.getNewResultType(),
                history.getOldNumericValue(), history.getNewNumericValue(), history.getOldTextValue(),
                history.getNewTextValue(), history.getOldUnit(), history.getNewUnit(), history.getOldReferenceRange(),
                history.getNewReferenceRange(), history.getOldAbnormalFlag(), history.getNewAbnormalFlag(),
                history.getOldConclusion(), history.getNewConclusion(), history.getOldStatus(), history.getNewStatus(),
                history.getChangeReason(), history.getChangedBy(), history.getChangedAt());
    }
}
