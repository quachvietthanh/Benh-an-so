package com.benhsoan.persistence.mapper.billing;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.billing.InvoiceLine;
import com.benhsoan.persistence.entity.billing.InvoiceEntity;
import com.benhsoan.persistence.entity.billing.InvoiceLineEntity;

@Component
public class InvoicePersistenceMapper {

    private final InvoiceLinePersistenceMapper invoiceLinePersistenceMapper;

    public InvoicePersistenceMapper(InvoiceLinePersistenceMapper invoiceLinePersistenceMapper) {
        this.invoiceLinePersistenceMapper = invoiceLinePersistenceMapper;
    }

    public Invoice toDomain(
            InvoiceEntity entity,
            List<InvoiceLineEntity> lineEntities
    ) {
        if (entity == null) {
            return null;
        }

        Objects.requireNonNull(lineEntities, "Invoice line entities are required.");

        List<InvoiceLine> lines = lineEntities.stream()
                .map(invoiceLinePersistenceMapper::toDomain)
                .toList();

        return Invoice.restore(
                entity.getId(),
                entity.getInvoiceCode(),
                entity.getVisitId(),
                entity.getPaymentId(),
                entity.getType(),
                entity.getOriginalInvoiceId(),
                entity.getAdjustmentReason(),
                entity.getTotalAmount(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                lines
        );
    }

    public InvoiceEntity toEntity(Invoice domain) {
        if (domain == null) {
            return null;
        }

        return InvoiceEntity.builder()
                .id(domain.getId())
                .invoiceCode(domain.getInvoiceCode())
                .visitId(domain.getVisitId())
                .paymentId(domain.getPaymentId())
                .type(domain.getType())
                .originalInvoiceId(domain.getOriginalInvoiceId())
                .adjustmentReason(domain.getAdjustmentReason())
                .totalAmount(domain.getTotalAmount())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
