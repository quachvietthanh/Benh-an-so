package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.billing.AdjustInvoiceRequest;
import com.benhsoan.adapter.inbound.rest.request.billing.AdjustmentInvoiceLineRequest;
import com.benhsoan.adapter.inbound.rest.request.billing.CreateInvoiceRequest;
import com.benhsoan.adapter.inbound.rest.request.billing.GetPaymentQuoteRequest;
import com.benhsoan.adapter.inbound.rest.request.billing.RecordPaymentRequest;
import com.benhsoan.adapter.inbound.rest.request.billing.RefundPaymentRequest;
import com.benhsoan.adapter.inbound.rest.response.billing.InvoiceLineResponse;
import com.benhsoan.adapter.inbound.rest.response.billing.InvoiceAdjustmentsResponse;
import com.benhsoan.adapter.inbound.rest.response.billing.InvoiceResponse;
import com.benhsoan.adapter.inbound.rest.response.billing.PayableEncounterResponse;
import com.benhsoan.adapter.inbound.rest.response.billing.PaymentResponse;
import com.benhsoan.adapter.inbound.rest.response.billing.PaymentQuoteResponse;
import com.benhsoan.adapter.inbound.rest.response.billing.PaymentServiceFeeQuoteResponse;
import com.benhsoan.adapter.inbound.rest.response.billing.RefundPaymentResponse;
import com.benhsoan.domain.patient.PatientAnonymizer;
import com.benhsoan.port.dto.command.billing.AdjustInvoiceCommand;
import com.benhsoan.port.dto.command.billing.AdjustmentInvoiceLineCommand;
import com.benhsoan.port.dto.command.billing.CreateInvoiceCommand;
import com.benhsoan.port.dto.command.billing.GetPaymentQuoteCommand;
import com.benhsoan.port.dto.command.billing.RecordPaymentCommand;
import com.benhsoan.port.dto.command.billing.RefundPaymentCommand;
import com.benhsoan.port.dto.result.InvoiceAdjustmentsResult;
import com.benhsoan.port.dto.result.InvoiceLineResult;
import com.benhsoan.port.dto.result.InvoiceResult;
import com.benhsoan.port.dto.result.PayableEncounterResult;
import com.benhsoan.port.dto.result.PaymentResult;
import com.benhsoan.port.dto.result.PaymentQuoteResult;
import com.benhsoan.adapter.inbound.rest.response.billing.PaymentDetailResponse;
import com.benhsoan.adapter.inbound.rest.response.billing.PaymentMethodItemResponse;
import com.benhsoan.port.dto.command.billing.PaymentMethodItemCommand;
import com.benhsoan.port.dto.result.PaymentDetailResult;
import com.benhsoan.port.dto.result.PaymentMethodItemResult;
import com.benhsoan.port.dto.result.RefundPaymentResult;

@Component
public class BillingRestMapper {

        private final AnonymizationModeState anonymizationModeState;

        public BillingRestMapper(
                        AnonymizationModeState anonymizationModeState) {
                this.anonymizationModeState = anonymizationModeState;
        }

        public RecordPaymentCommand toCommand(RecordPaymentRequest request) {
                List<PaymentMethodItemCommand> methodCommands = null;
                if (request.paymentMethods() != null && !request.paymentMethods().isEmpty()) {
                        methodCommands = request.paymentMethods().stream()
                                        .map(m -> new PaymentMethodItemCommand(
                                                        m.paymentMethod(),
                                                        m.amount(),
                                                        m.referenceNumber()))
                                        .toList();
                } else if (request.paymentMethod() != null) {
                        methodCommands = List.of(new PaymentMethodItemCommand(
                                        request.paymentMethod(),
                                        request.amountPaid(),
                                        request.referenceNumber()));
                }

                return RecordPaymentCommand.builder()
                                .visitId(request.visitId())
                                .examFee(request.examFee())
                                .medicineFee(request.medicineFee())
                                .amountPaid(request.amountPaid())
                                .paymentMethod(request.paymentMethod())
                                .referenceNumber(request.referenceNumber())
                                .paymentMethods(methodCommands)
                                .build();
        }

        public CreateInvoiceCommand toCommand(CreateInvoiceRequest request) {
                return CreateInvoiceCommand.builder()
                                .visitId(request.visitId())
                                .paymentId(request.paymentId())
                                .build();
        }

        public GetPaymentQuoteCommand toCommand(GetPaymentQuoteRequest request) {
                return new GetPaymentQuoteCommand(request.visitId(), request.examFee(), request.medicineFee());
        }

        public AdjustInvoiceCommand toCommand(UUID originalInvoiceId, AdjustInvoiceRequest request) {
                return new AdjustInvoiceCommand(
                                originalInvoiceId,
                                request.adjustmentReason(),
                                request.lines().stream().map(this::toCommand).toList());
        }

        public RefundPaymentCommand toCommand(UUID paymentId, RefundPaymentRequest request) {
                return new RefundPaymentCommand(paymentId, request.reason());
        }

        public PaymentResponse toResponse(PaymentResult result) {
                List<PaymentMethodItemResponse> methodResponses = List.of();
                if (result.paymentMethods() != null) {
                        methodResponses = result.paymentMethods().stream()
                                        .map(this::toResponse)
                                        .toList();
                }

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
                                result.createdAt(),
                                methodResponses);
        }

        public PaymentMethodItemResponse toResponse(PaymentMethodItemResult result) {
                return new PaymentMethodItemResponse(
                                result.id(),
                                result.paymentId(),
                                result.paymentMethod(),
                                result.amount(),
                                result.referenceNumber(),
                                result.createdAt());
        }

        public PaymentDetailResponse toResponse(PaymentDetailResult result) {
                if (result == null) {
                        return null;
                }
                List<PaymentMethodItemResponse> methodResponses = List.of();
                if (result.paymentMethods() != null) {
                        methodResponses = result.paymentMethods().stream()
                                        .map(this::toResponse)
                                        .toList();
                }
                return new PaymentDetailResponse(
                                result.id(),
                                result.visitId(),
                                result.status(),
                                result.totalAmount(),
                                result.amountPaid(),
                                result.paymentMethod(),
                                result.collectedBy(),
                                result.collectorName(),
                                result.paidAt(),
                                result.createdAt(),
                                methodResponses);
        }

        public PaymentQuoteResponse toResponse(PaymentQuoteResult result) {
                return new PaymentQuoteResponse(
                                result.visitId(),
                                result.examFee(),
                                result.medicineFee(),
                                result.serviceFee(),
                                result.totalAmount(),
                                result.serviceFees().stream()
                                                .map(fee -> new PaymentServiceFeeQuoteResponse(
                                                                fee.clinicalOrderItemId(), fee.serviceName(),
                                                                fee.amount()))
                                                .toList(),
                                result.calculatedAt());
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
                                result.reprintCount(),
                                result.lastReprintedAt(),
                                lines,
                                toResponse(result.payment()));
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
                                toResponse(result.adjustmentInvoice()));
        }

        public Page<InvoiceResponse> toInvoiceResponse(Page<InvoiceResult> results) {
                return results.map(this::toResponse);
        }

        public InvoiceAdjustmentsResponse toResponse(InvoiceAdjustmentsResult result) {
                return new InvoiceAdjustmentsResponse(
                                result.originalInvoiceId(),
                                result.originalAmount(),
                                result.finalAmount(),
                                result.adjustments().stream()
                                                .map(this::toResponse)
                                                .toList());
        }

        public PayableEncounterResponse toResponse(PayableEncounterResult result) {
                return new PayableEncounterResponse(
                                result.visitId(),
                                result.visitCode(),
                                result.patientId(),
                                result.patientCode(),
                                anonymizationModeState.isEnabled()
                                                ? PatientAnonymizer.maskFullName(result.patientCode())
                                                : result.patientName(),
                                result.reason(),
                                result.completedAt(),
                                result.examFee(),
                                result.medicineFee(),
                                result.serviceFee(),
                                result.totalEstimatedAmount(),
                                result.hasPrescription(),
                                result.hasPendingDispense());
        }

        public Page<PayableEncounterResponse> toPayableResponse(Page<PayableEncounterResult> results) {
                return results.map(this::toResponse);
        }

        private AdjustmentInvoiceLineCommand toCommand(AdjustmentInvoiceLineRequest request) {
                return new AdjustmentInvoiceLineCommand(
                                request.itemName(),
                                request.referenceId(),
                                request.quantity(),
                                request.unitPrice());
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
                                result.createdAt());
        }

        public com.benhsoan.port.dto.command.billing.CreateDiscountRequestCommand toCommand(
                        com.benhsoan.adapter.inbound.rest.request.billing.CreateDiscountRequest request) {
                return com.benhsoan.port.dto.command.billing.CreateDiscountRequestCommand.builder()
                                .visitId(request.getVisitId())
                                .discountType(request.getDiscountType())
                                .discountValue(request.getDiscountValue())
                                .originalAmount(request.getOriginalAmount())
                                .reason(request.getReason())
                                .build();
        }

        public com.benhsoan.port.dto.command.billing.RejectDiscountRequestCommand toCommand(
                        UUID discountRequestId,
                        com.benhsoan.adapter.inbound.rest.request.billing.RejectDiscountRequest request) {
                return com.benhsoan.port.dto.command.billing.RejectDiscountRequestCommand.builder()
                                .discountRequestId(discountRequestId)
                                .rejectionReason(request.getRejectionReason())
                                .build();
        }

        public com.benhsoan.adapter.inbound.rest.response.billing.DiscountRequestResponse toResponse(
                        com.benhsoan.port.dto.result.DiscountRequestResult result) {
                if (result == null) {
                        return null;
                }
                return com.benhsoan.adapter.inbound.rest.response.billing.DiscountRequestResponse.builder()
                                .id(result.id())
                                .visitId(result.visitId())
                                .discountType(result.discountType())
                                .discountValue(result.discountValue())
                                .originalAmount(result.originalAmount())
                                .discountAmount(result.discountAmount())
                                .finalAmount(result.finalAmount())
                                .reason(result.reason())
                                .status(result.status())
                                .requestedBy(result.requestedBy())
                                .requestedAt(result.requestedAt())
                                .approvedBy(result.approvedBy())
                                .approvedAt(result.approvedAt())
                                .rejectedBy(result.rejectedBy())
                                .rejectionReason(result.rejectionReason())
                                .rejectedAt(result.rejectedAt())
                                .invoiceId(result.invoiceId())
                                .build();
        }

        public Page<com.benhsoan.adapter.inbound.rest.response.billing.DiscountRequestResponse> toDiscountResponse(
                        Page<com.benhsoan.port.dto.result.DiscountRequestResult> results) {
                return results.map(this::toResponse);
        }

}
