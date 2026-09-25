package com.benhsoan.persistence.adapterRepository.prescription;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.prescription.PrescriptionReconciliationClassifier;
import com.benhsoan.domain.prescription.enums.InterconnectionStatus;
import com.benhsoan.domain.prescription.enums.PrescriptionReconciliationOutcome;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionDispenseItemRepository;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionReconciliationNoteRepository;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionRepository;
import com.benhsoan.persistence.jpaRepository.prescription.PrescriptionDispenseAggregateProjection;
import com.benhsoan.persistence.jpaRepository.prescription.PrescriptionReconciliationNoteCountProjection;
import com.benhsoan.persistence.jpaRepository.prescription.PrescriptionReconciliationProjection;
import com.benhsoan.port.dto.query.prescription.ReconciliationOutcomeGroup;
import com.benhsoan.port.dto.query.prescription.ReconciliationQueryFilter;
import com.benhsoan.port.dto.result.PrescriptionReconciliationItemResult;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionReconciliationQueryRepository;

import lombok.RequiredArgsConstructor;

/**
 * Reconciliation read adapter (NCL-12-CN-007 CV-02).
 *
 * A single page is served by exactly three queries: the reconciliation projection, the
 * paged count, and the batched dispensing/note lookups. No per-row aggregate mapper is
 * used, so the query count does not grow with the page size.
 *
 * The outcome filter is normalised here: unused groups are replaced by a non-empty
 * placeholder and disabled with the group2Active flag, so the query never receives an
 * empty IN list.
 */
@Repository
@RequiredArgsConstructor
public class PrescriptionReconciliationQueryRepositoryAdapter
        implements PrescriptionReconciliationQueryRepository {

    private static final ReconciliationOutcomeGroup UNUSED_FILTER_GROUP =
            new ReconciliationOutcomeGroup(
                    List.of(PrescriptionStatus.values()),
                    List.of(InterconnectionStatus.values()));

    private final JpaPrescriptionRepository jpaRepository;
    private final JpaPrescriptionDispenseItemRepository dispenseItemJpaRepository;
    private final JpaPrescriptionReconciliationNoteRepository noteJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PrescriptionReconciliationItemResult> findByFilter(
            ReconciliationQueryFilter filter,
            Pageable pageable
    ) {
        List<ReconciliationOutcomeGroup> groups = filter.outcomeGroups();
        boolean filterByOutcome = !groups.isEmpty();
        ReconciliationOutcomeGroup first = filterByOutcome ? groups.get(0) : UNUSED_FILTER_GROUP;
        boolean secondActive = groups.size() > 1;
        ReconciliationOutcomeGroup second = secondActive ? groups.get(1) : first;

        Page<PrescriptionReconciliationProjection> page = jpaRepository.findByReconciliationFilter(
                filter.fromInclusive(), filter.toExclusive(), filter.periodUnbounded(), filter.prescriptionCode(),
                filterByOutcome,
                first.dispensingStatuses(), first.interconnectionStatuses(),
                secondActive, second.dispensingStatuses(), second.interconnectionStatuses(),
                pageable);

        List<UUID> prescriptionIds = page.getContent().stream()
                .map(PrescriptionReconciliationProjection::prescriptionId)
                .toList();

        Map<UUID, Instant> lastDispensedAtById = findLastDispensedAt(prescriptionIds);
        Map<UUID, Long> noteCountById = countNotes(prescriptionIds);

        List<PrescriptionReconciliationItemResult> rows = page.getContent().stream()
                .map(projection -> toResult(
                        projection,
                        lastDispensedAtById.get(projection.prescriptionId()),
                        noteCountById.getOrDefault(projection.prescriptionId(), 0L)))
                .toList();

        return new PageImpl<>(rows, pageable, page.getTotalElements());
    }

    private Map<UUID, Instant> findLastDispensedAt(List<UUID> prescriptionIds) {
        if (prescriptionIds.isEmpty()) {
            return Map.of();
        }
        return dispenseItemJpaRepository.findDispenseAggregates(prescriptionIds).stream()
                .collect(Collectors.toMap(
                        PrescriptionDispenseAggregateProjection::prescriptionId,
                        PrescriptionDispenseAggregateProjection::lastDispensedAt));
    }

    private Map<UUID, Long> countNotes(List<UUID> prescriptionIds) {
        if (prescriptionIds.isEmpty()) {
            return Map.of();
        }
        return noteJpaRepository.countByPrescriptionIdIn(prescriptionIds).stream()
                .collect(Collectors.toMap(
                        PrescriptionReconciliationNoteCountProjection::prescriptionId,
                        PrescriptionReconciliationNoteCountProjection::noteCount));
    }

    private PrescriptionReconciliationItemResult toResult(
            PrescriptionReconciliationProjection projection,
            Instant lastDispensedAt,
            long reconciliationNoteCount
    ) {
        PrescriptionReconciliationOutcome outcome = PrescriptionReconciliationClassifier.classify(
                projection.prescriptionStatus(), projection.interconnectionStatus());
        return new PrescriptionReconciliationItemResult(
                projection.prescriptionId(),
                projection.prescriptionCode(),
                projection.patientId(),
                projection.patientCode(),
                projection.patientName(),
                projection.doctorId(),
                projection.doctorName(),
                projection.prescriptionStatus(),
                projection.interconnectionStatus(),
                outcome,
                outcome.isDiscrepancy(),
                PrescriptionReconciliationClassifier.isRetransmissionEligible(projection.interconnectionStatus()),
                projection.prescribedAt(),
                projection.lastInterconnectionAt(),
                lastDispensedAt,
                projection.lastInterconnectionError(),
                projection.interconnectionReceiptCode(),
                reconciliationNoteCount);
    }
}
