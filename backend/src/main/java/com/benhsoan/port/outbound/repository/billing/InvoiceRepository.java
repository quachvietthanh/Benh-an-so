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

    Optional<Invoice> findById(UUID id);

    Optional<Invoice> findOriginalByVisitId(UUID visitId);

    Optional<Invoice> findByPaymentId(UUID paymentId);

    boolean existsByOriginalInvoiceId(UUID originalInvoiceId);

    List<Invoice> findCreatedBetween(Instant fromInclusive, Instant toExclusive);

    Page<PayableEncounterSummary> findPayableEncounters(Pageable pageable);

    Page<Invoice> search(InvoiceSearchCriteria criteria, Pageable pageable);
}
