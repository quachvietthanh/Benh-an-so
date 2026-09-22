package com.benhsoan.application.ucservice.billing;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.billing.Payment;
import com.benhsoan.domain.billing.exception.InvoiceNotFoundException;
import com.benhsoan.domain.auth.User;
import com.benhsoan.port.dto.result.InvoiceResult;
import com.benhsoan.port.dto.result.PaymentDetailResult;
import com.benhsoan.port.dto.result.PaymentMethodItemResult;
import com.benhsoan.port.inbound.billing.GetInvoiceByIdUseCase;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.repository.billing.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetInvoiceByIdService implements GetInvoiceByIdUseCase {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final InvoiceResultMapper resultMapper;

    @Override
    public InvoiceResult getById(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        PaymentDetailResult paymentDetail = null;
        if (invoice.getPaymentId() != null) {
            Payment payment = paymentRepository.findById(invoice.getPaymentId()).orElse(null);
            if (payment != null) {
                String collectorName = null;
                if (payment.getCollectedBy() != null) {
                    collectorName = userRepository.findById(payment.getCollectedBy())
                            .map(User::getFullName)
                            .orElse(null);
                }

                List<PaymentMethodItemResult> methodResults = List.of();
                if (payment.getPaymentMethodItems() != null) {
                    methodResults = payment.getPaymentMethodItems().stream()
                            .map(item -> new PaymentMethodItemResult(
                                    item.getId(),
                                    item.getPaymentId(),
                                    item.getPaymentMethod(),
                                    item.getAmount(),
                                    item.getReferenceNumber(),
                                    item.getCreatedAt()
                            ))
                            .toList();
                }

                paymentDetail = new PaymentDetailResult(
                        payment.getId(),
                        payment.getVisitId(),
                        payment.getStatus(),
                        payment.getTotalAmount(),
                        payment.getAmountPaid(),
                        payment.getPaymentMethod(),
                        payment.getCollectedBy(),
                        collectorName,
                        payment.getPaidAt(),
                        payment.getCreatedAt(),
                        methodResults
                );
            }
        }

        return resultMapper.toResult(invoice, paymentDetail);
    }
}
