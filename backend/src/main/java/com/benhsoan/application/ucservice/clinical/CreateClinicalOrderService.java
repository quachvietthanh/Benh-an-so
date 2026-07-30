package com.benhsoan.application.ucservice.clinical;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.port.dto.command.clinical.CreateClinicalOrderCommand;
import com.benhsoan.port.dto.result.ClinicalOrderResult;
import com.benhsoan.port.inbound.clinical.CreateClinicalOrderUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateClinicalOrderService implements CreateClinicalOrderUseCase {

    private final VisitRepository visitRepository;
    private final CurrentUserPort currentUserPort;

    private static final AtomicLong orderCounter = new AtomicLong(System.currentTimeMillis());

    @Override
    public ClinicalOrderResult createOrder(UUID examinationId, CreateClinicalOrderCommand command) {
        Visit visit = visitRepository.findById(examinationId)
                .orElseThrow(() -> new ValidationException("Examination not found: " + examinationId));

        // QTN-13: Order must be linked to an examination encounter
        // QTN-07: Cannot order on completed visits
        if (visit.getStatus() == VisitStatus.COMPLETED || visit.getStatus() == VisitStatus.CANCELLED) {
            throw new ValidationException("Cannot create orders on a completed or cancelled examination.");
        }

        UUID doctorId = currentUserPort.getCurrentUserId();
        Instant now = Instant.now();
        String orderCode = "ORD-" + orderCounter.incrementAndGet();

        List<ClinicalOrderResult.OrderItemResult> itemResults = (command.items() != null)
                ? command.items().stream()
                        .map(item -> new ClinicalOrderResult.OrderItemResult(
                                UUID.randomUUID(),
                                item.serviceCode(),
                                item.serviceName(),
                                item.instruction(),
                                "ORDERED"))
                        .toList()
                : java.util.Collections.emptyList();

        return new ClinicalOrderResult(
                UUID.randomUUID(),
                orderCode,
                examinationId,
                visit.getPatientId(),
                doctorId,
                command.clinicalReason(),
                "ORDERED",
                now,
                null,
                itemResults
        );
    }
}
