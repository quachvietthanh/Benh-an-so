package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.billing.AdjustInvoiceRequest;
import com.benhsoan.adapter.inbound.rest.request.billing.AdjustmentInvoiceLineRequest;
import com.benhsoan.adapter.inbound.rest.request.billing.CreateInvoiceRequest;
import com.benhsoan.adapter.inbound.rest.request.billing.RecordPaymentRequest;
import com.benhsoan.adapter.inbound.rest.request.billing.RefundPaymentRequest;
import com.benhsoan.adapter.inbound.rest.response.billing.InvoiceLineResponse;
import com.benhsoan.adapter.inbound.rest.response.billing.InvoiceResponse;
import com.benhsoan.adapter.inbound.rest.response.billing.PayableEncounterResponse;
import com.benhsoan.adapter.inbound.rest.response.billing.PaymentResponse;
import com.benhsoan.adapter.inbound.rest.response.billing.RefundPaymentResponse;
import com.benhsoan.port.dto.command.billing.AdjustInvoiceCommand;
import com.benhsoan.port.dto.command.billing.AdjustmentInvoiceLineCommand;
import com.benhsoan.port.dto.command.billing.CreateInvoiceCommand;
import com.benhsoan.port.dto.command.billing.RecordPaymentCommand;
import com.benhsoan.port.dto.command.billing.RefundPaymentCommand;
import com.benhsoan.port.dto.result.InvoiceLineResult;
import com.benhsoan.port.dto.result.InvoiceResult;
import com.benhsoan.port.dto.result.PayableEncounterResult;
import com.benhsoan.port.dto.result.PaymentResult;
import com.benhsoan.port.dto.result.RefundPaymentResult;

@Component
public class BillingRestMapper {

    public RecordPaymentCommand toCommand(RecordPaymentRequest request) {
        return RecordPaymentCommand.builder()
                .visitId(request.visitId())
                .examFee(request.examFee())
                .medicineFee(request.medicineFee())
                .amountPaid(request.amountPaid())
                .paymentMethod(request.paymentMethod())
                .build();
    }

    public CreateInvoiceCommand toCommand(CreateInvoiceRequest request) {
        return CreateInvoiceCommand.builder()
                .visitId(request.visitId())
                .paymentId(request.paymentId())
                .build();
    }

    public AdjustInvoiceCommand toCommand(UUID originalInvoiceId, AdjustInvoiceRequest request) {
        return new AdjustInvoiceCommand(
                originalInvoiceId,
                request.adjustmentReason(),
                request.lines().stream().map(this::toCommand).toList()
        );
    }

    public RefundPaymentCommand toCommand(UUID paymentId, RefundPaymentRequest request) {
        return new RefundPaymentCommand(paymentId, request.reason());
    }

    public PaymentResponse toResponse(PaymentResult result) {
        return new PaymentResponse(
                result.id(),
                result.visitId(),
                result.examFee(),
                result.medicineFee(),
                result.serviceFee(),
                result.totalAmount(),
                result.amountPaid(),
                result.paymentMethod(),
                result.status(),
                result.collectedBy(),
                result.paidAt(),
                result.createdAt()
        );
    }

    public InvoiceResponse toResponse(InvoiceResult result) {
        List<InvoiceLineResponse> lines = result.lines().stream()
                .map(this::toResponse)
                .toList();

        return new InvoiceResponse(
                result.id(),
                result.invoiceCode(),
                result.visitId(),
                result.paymentId(),
                result.type(),
                result.originalInvoiceId(),
                result.adjustmentReason(),
                result.totalAmount(),
                result.createdBy(),
                result.createdAt(),
                lines
        );
    }

    public RefundPaymentResponse toResponse(RefundPaymentResult result) {
        return new RefundPaymentResponse(
                result.paymentId(),
                result.visitId(),
                result.status(),
                result.amountRefunded(),
                result.refundReason(),
                result.refundedBy(),
                result.refundedAt(),
                toResponse(result.adjustmentInvoice())
        );
    }

    public Page<InvoiceResponse> toInvoiceResponse(Page<InvoiceResult> results) {
        return results.map(this::toResponse);
    }

    public PayableEncounterResponse toResponse(PayableEncounterResult result) {
        return new PayableEncounterResponse(
                result.visitId(),
                result.visitCode(),
                result.patientId(),
                result.patientCode(),
                result.patientName(),
                result.reason(),
                result.completedAt()
        );
    }

    public Page<PayableEncounterResponse> toPayableResponse(Page<PayableEncounterResult> results) {
        return results.map(this::toResponse);
    }

    private AdjustmentInvoiceLineCommand toCommand(AdjustmentInvoiceLineRequest request) {
        return new AdjustmentInvoiceLineCommand(
                request.itemName(),
                request.referenceId(),
                request.quantity(),
                request.unitPrice()
        );
    }

    private InvoiceLineResponse toResponse(InvoiceLineResult result) {
        return new InvoiceLineResponse(
                result.id(),
                result.invoiceId(),
                result.lineType(),
                result.itemName(),
                result.referenceId(),
                result.quantity(),
                result.unitPrice(),
                result.amount(),
                result.createdAt()
        );
    }
}
