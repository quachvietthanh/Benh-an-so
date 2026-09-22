package com.benhsoan.port.outbound.repository.billing;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.billing.Invoice;

public interface InvoiceRepository {

    Invoice save(Invoice invoice);

    void updateReprintMetadata(UUID invoiceId, int reprintCount, Instant lastReprintedAt);

    Optional<Invoice> findById(UUID id);

    Optional<Invoice> findOriginalByVisitId(UUID visitId);

    Optional<Invoice> findByPaymentId(UUID paymentId);

    boolean existsByOriginalInvoiceId(UUID originalInvoiceId);

    List<Invoice> findAdjustmentsByOriginalInvoiceId(UUID originalInvoiceId);

    List<Invoice> findCreatedBetween(Instant fromInclusive, Instant toExclusive);

    Page<PayableEncounterSummary> findPayableEncounters(
            Instant fromCompletedAt,
            Instant toCompletedAt,
            String search,
            Pageable pageable
    );

    default Page<PayableEncounterSummary> findPayableEncounters(Pageable pageable) {
        return findPayableEncounters(null, null, null, pageable);
    }

    Page<Invoice> search(InvoiceSearchCriteria criteria, Pageable pageable);

    List<Invoice> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
}
