package com.benhsoan.persistence.adapterRepository.reporting;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.port.outbound.repository.reporting.DailyRevenueSummary;
import com.benhsoan.port.outbound.repository.reporting.DailyVisitSummary;
import com.benhsoan.port.outbound.repository.reporting.DiseasePatternSummary;
import com.benhsoan.port.outbound.repository.reporting.DoctorVisitSummary;
import com.benhsoan.port.outbound.repository.reporting.OperationalReportQueryRepository;
import com.benhsoan.port.outbound.repository.reporting.TopMedicineSummary;

import java.util.UUID;

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
    public List<DiseasePatternSummary> findDiseasePatternSummaries(
            Instant fromInclusive,
            Instant toExclusive,
            UUID doctorId
    ) {
        String jpql = """
                select d.diagnosisCatalogId,
                       c.code,
                       c.name,
                       c.diseaseGroup,
                       count(d.id)
                from MedicalRecordDiagnosisEntity d
                join DiagnosisCatalogEntity c on c.id = d.diagnosisCatalogId
                join MedicalRecordEntity mr on mr.id = d.medicalRecordId
                join VisitEntity v on v.id = mr.visitId
                where d.diagnosedAt >= :fromInclusive
                  and d.diagnosedAt < :toExclusive
                  and d.diagnosisCatalogId is not null
                  and v.status <> :cancelledStatus
                """
                + (doctorId != null ? " and d.diagnosedBy = :doctorId\n" : "\n")
                + """
                group by d.diagnosisCatalogId, c.code, c.name, c.diseaseGroup
                order by count(d.id) desc, c.code asc
                """;

        var query = entityManager.createQuery(jpql, Object[].class)
                .setParameter("fromInclusive", fromInclusive)
                .setParameter("toExclusive", toExclusive)
                .setParameter("cancelledStatus", VisitStatus.CANCELLED);

        if (doctorId != null) {
            query.setParameter("doctorId", doctorId);
        }

        return query.getResultList()
                .stream()
                .map(row -> new DiseasePatternSummary(
                        (UUID) row[0],
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        ((Number) row[4]).longValue()
                ))
                .toList();
    }

    @Override
    public boolean hasDiagnoses(Instant fromInclusive, Instant toExclusive, UUID doctorId) {
        String jpql = """
                select count(d.id)
                from MedicalRecordDiagnosisEntity d
                join MedicalRecordEntity mr on mr.id = d.medicalRecordId
                join VisitEntity v on v.id = mr.visitId
                where d.diagnosedAt >= :fromInclusive
                  and d.diagnosedAt < :toExclusive
                  and d.diagnosisCatalogId is not null
                  and v.status <> :cancelledStatus
                """
                + (doctorId != null ? " and d.diagnosedBy = :doctorId" : "");

        var query = entityManager.createQuery(jpql, Long.class)
                .setParameter("fromInclusive", fromInclusive)
                .setParameter("toExclusive", toExclusive)
                .setParameter("cancelledStatus", VisitStatus.CANCELLED);

        if (doctorId != null) {
            query.setParameter("doctorId", doctorId);
        }

        Long count = query.getSingleResult();
        return count != null && count > 0;
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
