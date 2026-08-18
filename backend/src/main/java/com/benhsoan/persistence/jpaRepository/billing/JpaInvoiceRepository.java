package com.benhsoan.persistence.jpaRepository.billing;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.persistence.entity.billing.InvoiceEntity;

public interface JpaInvoiceRepository
        extends JpaRepository<InvoiceEntity, UUID> {

    Optional<InvoiceEntity> findByVisitIdAndType(
            UUID visitId,
            InvoiceType type
    );

    Optional<InvoiceEntity> findByPaymentId(UUID paymentId);

    boolean existsByOriginalInvoiceId(UUID originalInvoiceId);

    @Query("""
            select invoice
            from InvoiceEntity invoice
            where invoice.createdAt >= :fromInclusive
              and invoice.createdAt < :toExclusive
            order by invoice.createdAt asc
            """)
    List<InvoiceEntity> findCreatedBetween(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive
    );

    @Query("""
            select
                visit.id as visitId,
                visit.visitCode as visitCode,
                patient.id as patientId,
                patient.patientCode as patientCode,
                patient.fullName as patientName,
                visit.reason as reason,
                visit.completedAt as completedAt
            from VisitEntity visit
            join PatientEntity patient on patient.id = visit.patientId
            left join PaymentEntity payment on payment.visitId = visit.id
            where visit.status <> com.benhsoan.domain.visit.enums.VisitStatus.CANCELLED
              and payment.id is null
              and not exists (
                  select 1
                  from MedicalRecordEntity medicalRecord
                  join PrescriptionEntity prescription
                    on prescription.medicalRecordId = medicalRecord.id
                  where medicalRecord.visitId = visit.id
                    and prescription.status = com.benhsoan.domain.prescription.enums.PrescriptionStatus.PENDING_DISPENSE
              )
            order by visit.completedAt desc
            """)
    Page<PayableEncounterProjection> findPayableEncounters(Pageable pageable);

    @Query(
            value = """
                    select invoice
                    from InvoiceEntity invoice
                    where (:invoiceCode is null
                        or lower(invoice.invoiceCode) like lower(concat('%', :invoiceCode, '%')))
                      and (:invoiceType is null or invoice.type = :invoiceType)
                      and (:visitId is null or invoice.visitId = :visitId)
                      and (:createdFrom is null or invoice.createdAt >= :createdFrom)
                      and (:createdTo is null or invoice.createdAt <= :createdTo)
                    order by invoice.createdAt desc
                    """,
            countQuery = """
                    select count(invoice)
                    from InvoiceEntity invoice
                    where (:invoiceCode is null
                        or lower(invoice.invoiceCode) like lower(concat('%', :invoiceCode, '%')))
                      and (:invoiceType is null or invoice.type = :invoiceType)
                      and (:visitId is null or invoice.visitId = :visitId)
                      and (:createdFrom is null or invoice.createdAt >= :createdFrom)
                      and (:createdTo is null or invoice.createdAt <= :createdTo)
                    """
    )
    Page<InvoiceEntity> search(
            @Param("invoiceCode") String invoiceCode,
            @Param("invoiceType") InvoiceType invoiceType,
            @Param("visitId") UUID visitId,
            @Param("createdFrom") Instant createdFrom,
            @Param("createdTo") Instant createdTo,
            Pageable pageable
    );
}
