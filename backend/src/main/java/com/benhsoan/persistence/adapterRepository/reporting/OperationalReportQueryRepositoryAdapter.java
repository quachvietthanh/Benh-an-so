package com.benhsoan.persistence.adapterRepository.reporting;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.billing.enums.InvoiceLineType;
import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.port.outbound.repository.reporting.DailyRevenueSummary;
import com.benhsoan.port.outbound.repository.reporting.DailyVisitSummary;
import com.benhsoan.port.outbound.repository.reporting.DoctorVisitSummary;
import com.benhsoan.port.outbound.repository.reporting.InvoiceLineReportDetail;
import com.benhsoan.port.outbound.repository.reporting.OperationalReportQueryRepository;
import com.benhsoan.port.outbound.repository.reporting.TopMedicineSummary;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperationalReportQueryRepositoryAdapter implements OperationalReportQueryRepository {

    private final EntityManager entityManager;

    @Override
    public long countCompletedVisits(Instant fromInclusive, Instant toExclusive) {
        Long count = entityManager.createQuery("""
                select count(visit)
                from VisitEntity visit
                where visit.status = :completedStatus
                  and visit.completedAt >= :fromInclusive
                  and visit.completedAt < :toExclusive
                """, Long.class)
                .setParameter("completedStatus", VisitStatus.COMPLETED)
                .setParameter("fromInclusive", fromInclusive)
                .setParameter("toExclusive", toExclusive)
                .getSingleResult();

        return count == null ? 0L : count;
    }

    @Override
    public boolean hasCompletedVisits(Instant fromInclusive, Instant toExclusive) {
        return countCompletedVisits(fromInclusive, toExclusive) > 0;
    }

    @Override
    public boolean hasInvoices(Instant fromInclusive, Instant toExclusive) {
        Long invoiceCount = entityManager.createQuery("""
                select count(invoice)
                from InvoiceEntity invoice
                where invoice.createdAt >= :fromInclusive
                  and invoice.createdAt < :toExclusive
                """, Long.class)
                .setParameter("fromInclusive", fromInclusive)
                .setParameter("toExclusive", toExclusive)
                .getSingleResult();

        return invoiceCount != null && invoiceCount > 0;
    }

    @Override
    public List<DailyVisitSummary> findDailyCompletedVisits(Instant fromInclusive, Instant toExclusive) {
        return entityManager.createQuery("""
                select cast(visit.completedAt as date), count(visit)
                from VisitEntity visit
                where visit.status = :completedStatus
                  and visit.completedAt >= :fromInclusive
                  and visit.completedAt < :toExclusive
                group by cast(visit.completedAt as date)
                order by cast(visit.completedAt as date)
                """, Object[].class)
                .setParameter("completedStatus", VisitStatus.COMPLETED)
                .setParameter("fromInclusive", fromInclusive)
                .setParameter("toExclusive", toExclusive)
                .getResultList()
                .stream()
                .map(row -> new DailyVisitSummary(
                        toLocalDate(row[0]),
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    @Override
    public BigDecimal sumNetRevenue(Instant fromInclusive, Instant toExclusive) {
        BigDecimal revenue = entityManager.createQuery("""
                select coalesce(sum(invoice.totalAmount), 0)
                from InvoiceEntity invoice
                where invoice.createdAt >= :fromInclusive
                  and invoice.createdAt < :toExclusive
                """, BigDecimal.class)
                .setParameter("fromInclusive", fromInclusive)
                .setParameter("toExclusive", toExclusive)
                .getSingleResult();

        return revenue == null ? BigDecimal.ZERO : revenue;
    }

    @Override
    public List<DailyRevenueSummary> findDailyNetRevenue(Instant fromInclusive, Instant toExclusive) {
        return entityManager.createQuery("""
                select cast(invoice.createdAt as date),
                       coalesce(sum(invoice.totalAmount), 0)
                from InvoiceEntity invoice
                where invoice.createdAt >= :fromInclusive
                  and invoice.createdAt < :toExclusive
                group by cast(invoice.createdAt as date)
                order by cast(invoice.createdAt as date)
                """, Object[].class)
                .setParameter("fromInclusive", fromInclusive)
                .setParameter("toExclusive", toExclusive)
                .getResultList()
                .stream()
                .map(row -> new DailyRevenueSummary(
                        toLocalDate(row[0]),
                        row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1]
                ))
                .toList();
    }

    @Override
    public List<TopMedicineSummary> findTopDispensedMedicines(Instant fromInclusive, Instant toExclusive) {
        return entityManager.createQuery("""
                select dispense.medicineId,
                       medicine.medicineCode,
                       medicine.medicineName,
                       sum(dispense.dispensedQuantity)
                from PrescriptionDispenseItemEntity dispense
                join MedicineEntity medicine on medicine.id = dispense.medicineId
                where dispense.dispensedAt >= :fromInclusive
                  and dispense.dispensedAt < :toExclusive
                group by dispense.medicineId, medicine.medicineCode, medicine.medicineName
                order by sum(dispense.dispensedQuantity) desc, medicine.medicineCode asc
                """, Object[].class)
                .setParameter("fromInclusive", fromInclusive)
                .setParameter("toExclusive", toExclusive)
                .getResultList()
                .stream()
                .map(row -> new TopMedicineSummary(
                        (java.util.UUID) row[0],
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue()
                ))
                .toList();
    }

    @Override
    public List<DoctorVisitSummary> findDoctorVisitSummaries(Instant fromInclusive, Instant toExclusive) {
        return entityManager.createQuery("""
                select doctor.id,
                       doctor.username,
                       doctor.fullName,
                       count(visit)
                from VisitEntity visit
                join UserEntity doctor on doctor.id = visit.doctorId
                where visit.status = :completedStatus
                  and visit.completedAt >= :fromInclusive
                  and visit.completedAt < :toExclusive
                group by doctor.id, doctor.username, doctor.fullName
                order by count(visit) desc, doctor.fullName asc
                """, Object[].class)
                .setParameter("completedStatus", VisitStatus.COMPLETED)
                .setParameter("fromInclusive", fromInclusive)
                .setParameter("toExclusive", toExclusive)
                .getResultList()
                .stream()
                .map(row -> new DoctorVisitSummary(
                        (java.util.UUID) row[0],
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue()
                ))
                .toList();
    }

    @Override
    public List<InvoiceLineReportDetail> findInvoiceLineReportDetails(Instant fromInclusive, Instant toExclusive) {
        List<Object[]> rawLines = entityManager.createQuery("""
                select line.id,
                       line.invoiceId,
                       invoice.type,
                       line.lineType,
                       line.itemName,
                       line.amount,
                       line.referenceId,
                       invoice.visitId,
                       visit.doctorId,
                       invoice.originalInvoiceId
                from InvoiceLineEntity line
                join InvoiceEntity invoice on invoice.id = line.invoiceId
                join VisitEntity visit on visit.id = invoice.visitId
                where invoice.createdAt >= :fromInclusive
                  and invoice.createdAt < :toExclusive
                """, Object[].class)
                .setParameter("fromInclusive", fromInclusive)
                .setParameter("toExclusive", toExclusive)
                .getResultList();

        if (rawLines.isEmpty()) {
            return List.of();
        }

        Set<UUID> clinicalOrderItemIds = new HashSet<>();
        Set<UUID> originalInvoiceIds = new HashSet<>();

        for (Object[] row : rawLines) {
            InvoiceLineType lineType = (InvoiceLineType) row[3];
            UUID refId = (UUID) row[6];
            UUID origInvId = (UUID) row[9];

            if (lineType == InvoiceLineType.SERVICE_FEE && refId != null) {
                clinicalOrderItemIds.add(refId);
            } else if (lineType == InvoiceLineType.ADJUSTMENT) {
                if (refId != null) {
                    clinicalOrderItemIds.add(refId);
                }
                if (origInvId != null) {
                    originalInvoiceIds.add(origInvId);
                }
            }
        }

        Map<UUID, ClinicalItemLookup> clinicalItemMap = new HashMap<>();
        if (!clinicalOrderItemIds.isEmpty()) {
            for (List<UUID> batch : partition(clinicalOrderItemIds, 500)) {
                List<Object[]> clinicalItemRows = entityManager.createQuery("""
                        select item.id,
                               catalog.serviceType,
                               orders.orderedBy
                        from ClinicalOrderItemEntity item
                        join ClinicalOrderEntity orders on orders.id = item.clinicalOrderId
                        join ClinicalServiceCatalogEntity catalog on catalog.id = item.clinicalServiceId
                        where item.id in (:itemIds)
                        """, Object[].class)
                        .setParameter("itemIds", batch)
                        .getResultList();

                for (Object[] itemRow : clinicalItemRows) {
                    clinicalItemMap.put(
                            (UUID) itemRow[0],
                            new ClinicalItemLookup((ClinicalServiceType) itemRow[1], (UUID) itemRow[2])
                    );
                }
            }
        }

        Map<UUID, List<OriginalLineLookup>> origLinesByInvoiceId = new HashMap<>();
        if (!originalInvoiceIds.isEmpty()) {
            for (List<UUID> batch : partition(originalInvoiceIds, 500)) {
                List<Object[]> origLineRows = entityManager.createQuery("""
                        select orig.invoiceId,
                               orig.id,
                               orig.lineType,
                               orig.itemName,
                               orig.referenceId
                        from InvoiceLineEntity orig
                        where orig.invoiceId in (:origInvoiceIds)
                        """, Object[].class)
                        .setParameter("origInvoiceIds", batch)
                        .getResultList();

                for (Object[] oRow : origLineRows) {
                    UUID invId = (UUID) oRow[0];
                    UUID lId = (UUID) oRow[1];
                    InvoiceLineType lType = (InvoiceLineType) oRow[2];
                    String iName = (String) oRow[3];
                    UUID rId = (UUID) oRow[4];

                    origLinesByInvoiceId.computeIfAbsent(invId, k -> new ArrayList<>())
                            .add(new OriginalLineLookup(lId, lType, iName, rId));
                }
            }
        }

        Set<UUID> medicineVisitIds = new HashSet<>();
        for (Object[] row : rawLines) {
            InvoiceLineType lineType = (InvoiceLineType) row[3];
            String itemName = (String) row[4];
            UUID refId = (UUID) row[6];
            UUID visitId = (UUID) row[7];
            UUID origInvId = (UUID) row[9];

            InvoiceLineType targetLineType = resolveTargetLineType(lineType, refId, origInvId, itemName, origLinesByInvoiceId, clinicalItemMap);
            if (visitId != null && (lineType == InvoiceLineType.MEDICINE_FEE || targetLineType == InvoiceLineType.MEDICINE_FEE)) {
                medicineVisitIds.add(visitId);
            }
        }

        Map<UUID, UUID> prescriptionDoctorMap = new HashMap<>();
        if (!medicineVisitIds.isEmpty()) {
            for (List<UUID> batch : partition(medicineVisitIds, 500)) {
                List<Object[]> prescriptionRows = entityManager.createQuery("""
                        select mr.visitId,
                               p.prescribedBy
                        from MedicalRecordEntity mr
                        join PrescriptionEntity p on p.medicalRecordId = mr.id
                        where mr.visitId in (:visitIds)
                          and p.status != com.benhsoan.domain.prescription.enums.PrescriptionStatus.CANCELLED
                        order by p.prescribedAt desc
                        """, Object[].class)
                        .setParameter("visitIds", batch)
                        .getResultList();

                for (Object[] pRow : prescriptionRows) {
                    if (pRow[0] != null && pRow[1] != null) {
                        prescriptionDoctorMap.putIfAbsent((UUID) pRow[0], (UUID) pRow[1]);
                    }
                }
            }
        }

        Set<UUID> allDoctorIds = new HashSet<>();
        for (Object[] row : rawLines) {
            InvoiceLineType lineType = (InvoiceLineType) row[3];
            String itemName = (String) row[4];
            UUID refId = (UUID) row[6];
            UUID visitId = (UUID) row[7];
            UUID visitDoctorId = (UUID) row[8];
            UUID origInvId = (UUID) row[9];

            InvoiceLineType targetLineType = resolveTargetLineType(lineType, refId, origInvId, itemName, origLinesByInvoiceId, clinicalItemMap);
            UUID effectiveDocId = resolveDoctorId(lineType, targetLineType, refId, visitId, visitDoctorId, clinicalItemMap, prescriptionDoctorMap);
            if (effectiveDocId != null) {
                allDoctorIds.add(effectiveDocId);
            }
        }

        Map<UUID, DoctorLookup> doctorInfoMap = new HashMap<>();
        if (!allDoctorIds.isEmpty()) {
            for (List<UUID> batch : partition(allDoctorIds, 500)) {
                List<Object[]> doctorRows = entityManager.createQuery("""
                        select u.id, u.username, u.fullName
                        from UserEntity u
                        where u.id in (:doctorIds)
                        """, Object[].class)
                        .setParameter("doctorIds", batch)
                        .getResultList();

                for (Object[] dRow : doctorRows) {
                    doctorInfoMap.put((UUID) dRow[0], new DoctorLookup((String) dRow[1], (String) dRow[2]));
                }
            }
        }

        return rawLines.stream().map(row -> {
            UUID lineId = (UUID) row[0];
            UUID invoiceId = (UUID) row[1];
            InvoiceType invoiceType = (InvoiceType) row[2];
            InvoiceLineType lineType = (InvoiceLineType) row[3];
            String itemName = (String) row[4];
            BigDecimal amount = (BigDecimal) row[5];
            UUID refId = (UUID) row[6];
            UUID visitId = (UUID) row[7];
            UUID visitDoctorId = (UUID) row[8];
            UUID origInvId = (UUID) row[9];

            InvoiceLineType targetLineType = resolveTargetLineType(lineType, refId, origInvId, itemName, origLinesByInvoiceId, clinicalItemMap);
            UUID effectiveDocId = resolveDoctorId(lineType, targetLineType, refId, visitId, visitDoctorId, clinicalItemMap, prescriptionDoctorMap);
            ClinicalServiceType clinicalServiceType = resolveClinicalServiceType(lineType, refId, clinicalItemMap);
            DoctorLookup doc = effectiveDocId != null ? doctorInfoMap.get(effectiveDocId) : null;

            return new InvoiceLineReportDetail(
                    lineId,
                    invoiceId,
                    invoiceType,
                    lineType,
                    targetLineType,
                    itemName,
                    amount != null ? amount : BigDecimal.ZERO,
                    refId,
                    visitId,
                    effectiveDocId,
                    doc != null ? doc.username() : null,
                    doc != null ? doc.fullName() : null,
                    clinicalServiceType
            );
        }).toList();
    }

    private InvoiceLineType resolveTargetLineType(
            InvoiceLineType lineType,
            UUID refId,
            UUID origInvId,
            String itemName,
            Map<UUID, List<OriginalLineLookup>> origLinesByInvoiceId,
            Map<UUID, ClinicalItemLookup> clinicalItemMap
    ) {
        if (lineType != InvoiceLineType.ADJUSTMENT) {
            return lineType;
        }
        if (refId != null && clinicalItemMap.containsKey(refId)) {
            return InvoiceLineType.SERVICE_FEE;
        }
        if (origInvId != null && origLinesByInvoiceId.containsKey(origInvId)) {
            List<OriginalLineLookup> origLines = origLinesByInvoiceId.get(origInvId);
            if (refId != null) {
                for (OriginalLineLookup orig : origLines) {
                    if (refId.equals(orig.lineId()) || refId.equals(orig.referenceId())) {
                        return orig.lineType();
                    }
                }
            }
            if (itemName != null) {
                for (OriginalLineLookup orig : origLines) {
                    if (itemName.equalsIgnoreCase(orig.itemName())) {
                        return orig.lineType();
                    }
                }
            }
            if (origLines.size() == 1) {
                return origLines.get(0).lineType();
            }
        }
        return null;
    }

    private UUID resolveDoctorId(
            InvoiceLineType lineType,
            InvoiceLineType targetLineType,
            UUID refId,
            UUID visitId,
            UUID visitDoctorId,
            Map<UUID, ClinicalItemLookup> clinicalItemMap,
            Map<UUID, UUID> prescriptionDoctorMap
    ) {
        if ((lineType == InvoiceLineType.SERVICE_FEE || targetLineType == InvoiceLineType.SERVICE_FEE) && refId != null) {
            ClinicalItemLookup item = clinicalItemMap.get(refId);
            if (item != null && item.orderedBy() != null) {
                return item.orderedBy();
            }
        }
        if (lineType == InvoiceLineType.MEDICINE_FEE || targetLineType == InvoiceLineType.MEDICINE_FEE) {
            if (visitId != null) {
                UUID prescribedBy = prescriptionDoctorMap.get(visitId);
                if (prescribedBy != null) {
                    return prescribedBy;
                }
            }
        }
        if (lineType == InvoiceLineType.ADJUSTMENT && refId != null) {
            ClinicalItemLookup item = clinicalItemMap.get(refId);
            if (item != null && item.orderedBy() != null) {
                return item.orderedBy();
            }
        }
        if (lineType == InvoiceLineType.ADJUSTMENT && targetLineType == null) {
            return null;
        }
        return visitDoctorId;
    }

    private ClinicalServiceType resolveClinicalServiceType(
            InvoiceLineType lineType,
            UUID refId,
            Map<UUID, ClinicalItemLookup> clinicalItemMap
    ) {
        if ((lineType == InvoiceLineType.SERVICE_FEE || lineType == InvoiceLineType.ADJUSTMENT) && refId != null) {
            ClinicalItemLookup item = clinicalItemMap.get(refId);
            if (item != null) {
                return item.serviceType();
            }
        }
        return null;
    }

    private record OriginalLineLookup(UUID lineId, InvoiceLineType lineType, String itemName, UUID referenceId) {}

    private record ClinicalItemLookup(ClinicalServiceType serviceType, UUID orderedBy) {}

    private record DoctorLookup(String username, String fullName) {}

    private static <T> List<List<T>> partition(Collection<T> collection, int batchSize) {
        List<T> list = new ArrayList<>(collection);
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(list.size(), i + batchSize)));
        }
        return batches;
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof Instant instant) {
            return instant.atZone(ZoneOffset.UTC).toLocalDate();
        }

        throw new IllegalStateException("Unsupported date projection type: " + value);
    }
}
