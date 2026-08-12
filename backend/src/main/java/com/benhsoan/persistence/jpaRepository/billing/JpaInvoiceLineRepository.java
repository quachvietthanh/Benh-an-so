package com.benhsoan.persistence.jpaRepository.billing;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.billing.InvoiceLineEntity;

public interface JpaInvoiceLineRepository
        extends JpaRepository<InvoiceLineEntity, UUID> {

    void deleteAllByInvoiceId(UUID invoiceId);

    List<InvoiceLineEntity> findByInvoiceIdOrderByCreatedAtAsc(UUID invoiceId);
}
