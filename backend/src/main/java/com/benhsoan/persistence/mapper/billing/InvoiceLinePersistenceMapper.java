package com.benhsoan.persistence.mapper.billing;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.billing.InvoiceLine;
import com.benhsoan.persistence.entity.billing.InvoiceLineEntity;

@Component
public class InvoiceLinePersistenceMapper {

    public InvoiceLine toDomain(InvoiceLineEntity entity) {
        if (entity == null) {
            return null;
        }

        return InvoiceLine.create(
                entity.getId(),
                entity.getInvoiceId(),
                entity.getLineType(),
                entity.getItemName(),
                entity.getReferenceId(),
                entity.getQuantity(),
                entity.getUnitPrice(),
                entity.getAmount(),
                entity.getCreatedAt()
        );
    }

    public InvoiceLineEntity toEntity(InvoiceLine domain) {
        if (domain == null) {
            return null;
        }

        return InvoiceLineEntity.builder()
                .id(domain.getId())
                .invoiceId(domain.getInvoiceId())
                .lineType(domain.getLineType())
                .itemName(domain.getItemName())
                .referenceId(domain.getReferenceId())
                .quantity(domain.getQuantity())
                .unitPrice(domain.getUnitPrice())
                .amount(domain.getAmount())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
