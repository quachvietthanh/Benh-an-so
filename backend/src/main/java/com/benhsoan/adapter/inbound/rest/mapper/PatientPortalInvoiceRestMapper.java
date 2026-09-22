package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.portal.PatientPortalInvoiceDetailResponse;
import com.benhsoan.adapter.inbound.rest.response.portal.PatientPortalInvoiceDetailResponse.InvoiceLineItemResponse;
import com.benhsoan.adapter.inbound.rest.response.portal.PatientPortalInvoiceSummaryResponse;
import com.benhsoan.port.dto.result.portal.PatientPortalInvoiceDetailResult;
import com.benhsoan.port.dto.result.portal.PatientPortalInvoiceSummaryResult;

@Component
public class PatientPortalInvoiceRestMapper {

    public PatientPortalInvoiceSummaryResponse toResponse(PatientPortalInvoiceSummaryResult result) {
        if (result == null) {
            return null;
        }
        return new PatientPortalInvoiceSummaryResponse(
                result.invoiceId(),
                result.invoiceCode(),
                result.invoiceType(),
                result.totalAmount(),
                result.createdAt(),
                result.visitId(),
                result.visitCode(),
                result.visitDate(),
                result.doctorName(),
                result.specialtyName(),
                result.itemCount()
        );
    }

    public PatientPortalInvoiceDetailResponse toResponse(PatientPortalInvoiceDetailResult result) {
        if (result == null) {
            return null;
        }
        List<InvoiceLineItemResponse> items = result.items() == null ? List.of() : result.items().stream()
                .map(item -> new InvoiceLineItemResponse(
                        item.lineId(),
                        item.lineType(),
                        item.itemName(),
                        item.quantity(),
                        item.unitPrice(),
                        item.amount()
                ))
                .toList();

        return new PatientPortalInvoiceDetailResponse(
                result.invoiceId(),
                result.invoiceCode(),
                result.invoiceType(),
                result.originalInvoiceId(),
                result.originalInvoiceCode(),
                result.adjustmentReason(),
                result.totalAmount(),
                result.createdAt(),
                result.creatorName(),
                result.visitId(),
                result.visitCode(),
                result.visitDate(),
                result.doctorName(),
                result.specialtyName(),
                items
        );
    }
}
