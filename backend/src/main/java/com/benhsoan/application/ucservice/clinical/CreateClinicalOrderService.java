package com.benhsoan.application.ucservice.clinical;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.clinical.ClinicalOrder;
import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.domain.clinical.exception.ClinicalOrderInvalidVisitException;
import com.benhsoan.domain.clinical.exception.ClinicalOrderLockedMedicalRecordException;
import com.benhsoan.domain.clinical.exception.ClinicalServiceUnavailableException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.clinical.CreateClinicalOrderCommand;
import com.benhsoan.port.dto.result.ClinicalOrderResult;
import com.benhsoan.port.inbound.clinical.CreateClinicalOrderUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.crudRepository.clinical.ClinicalOrderRepository;
import com.benhsoan.port.outbound.repository.crudRepository.clinical.ClinicalServiceCatalogRepository;
import com.benhsoan.port.outbound.repository.crudRepository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.crudRepository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateClinicalOrderService implements CreateClinicalOrderUseCase {

    private static final char[] ORDER_CODE_CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int ORDER_CODE_RANDOM_LENGTH = 24;

    private final VisitRepository visitRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final ClinicalServiceCatalogRepository clinicalServiceCatalogRepository;
    private final ClinicalOrderRepository clinicalOrderRepository;
    private final ClinicalOrderItemRepository clinicalOrderItemRepository;
    private final ClinicalOrderAuthorizationService authorizationService;
    private final ClinicalOrderAuditService auditService;
    private final ClinicalOrderResultMapper resultMapper;
    private final ClockPort clockPort;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public ClinicalOrderResult createOrder(UUID visitId, CreateClinicalOrderCommand command) {
        UUID actorId = authorizationService.requireWriteAccess();
        var visit = visitRepository.findById(visitId).orElseThrow(() -> new VisitNotFoundException(visitId));
        if (!visit.isActive()) {
            throw new ClinicalOrderInvalidVisitException();
        }

        var medicalRecord = medicalRecordRepository.findByVisitId(visitId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(visitId));
        if (medicalRecord.isLocked()) {
            throw new ClinicalOrderLockedMedicalRecordException();
        }

        List<UUID> serviceIds = command.items().stream()
                .map(CreateClinicalOrderCommand.OrderItemCommand::serviceId)
                .toList();
        ensureDistinctServiceIds(serviceIds);
        Map<UUID, com.benhsoan.domain.clinical.ClinicalServiceCatalog> servicesById =
                clinicalServiceCatalogRepository.findActiveByIdIn(serviceIds).stream()
                        .collect(Collectors.toMap(com.benhsoan.domain.clinical.ClinicalServiceCatalog::getId,
                                Function.identity()));
        if (servicesById.size() != serviceIds.size()) {
            throw new ClinicalServiceUnavailableException();
        }

        Instant now = clockPort.now();
        ClinicalOrder savedOrder = clinicalOrderRepository.save(ClinicalOrder.create(
                generateOrderCode(), visit.getId(), medicalRecord.getId(), visit.getPatientId(), actorId,
                command.clinicalReason(), now
        ));
        List<ClinicalOrderItem> savedItems = clinicalOrderItemRepository.saveAll(command.items().stream()
                .map(item -> {
                    var service = servicesById.get(item.serviceId());
                    return ClinicalOrderItem.create(savedOrder.getId(), service.getId(), service.getServiceCode(),
                            service.getServiceName(), item.instruction(), now);
                })
                .toList());
        auditService.recordCreated(visit.getPatientId(), visit.getId(), medicalRecord.getId(), actorId, now);
        return resultMapper.toResult(savedOrder, savedItems);
    }

    private void ensureDistinctServiceIds(List<UUID> serviceIds) {
        if (new HashSet<>(serviceIds).size() != serviceIds.size()) {
            throw new ClinicalServiceUnavailableException();
        }
    }

    private String generateOrderCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder value = new StringBuilder("ORD-");
            for (int index = 0; index < ORDER_CODE_RANDOM_LENGTH; index++) {
                value.append(ORDER_CODE_CHARACTERS[secureRandom.nextInt(ORDER_CODE_CHARACTERS.length)]);
            }
            String orderCode = value.toString();
            if (!clinicalOrderRepository.existsByOrderCode(orderCode)) {
                return orderCode;
            }
        }
        throw new IllegalStateException("Unable to generate a unique clinical order code.");
    }
}
