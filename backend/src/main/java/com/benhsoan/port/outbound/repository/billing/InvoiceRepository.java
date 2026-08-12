package com.benhsoan.port.outbound.repository.billing;

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

    Page<PayableEncounterSummary> findPayableEncounters(Pageable pageable);

    Page<Invoice> search(InvoiceSearchCriteria criteria, Pageable pageable);
}
