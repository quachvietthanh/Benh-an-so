package com.benhsoan.application.ucservice.servicecatalog;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.servicecatalog.ServiceCatalog;
import com.benhsoan.domain.servicecatalog.ServicePrice;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.servicecatalog.CreateServiceCatalogCommand;
import com.benhsoan.port.dto.result.servicecatalog.ServiceCatalogResult;
import com.benhsoan.port.inbound.servicecatalog.CreateServiceCatalogUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.servicecatalog.ServiceCatalogRepository;
import com.benhsoan.port.outbound.repository.servicecatalog.ServicePriceRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateServiceCatalogService implements CreateServiceCatalogUseCase {

    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ServicePriceRepository servicePriceRepository;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final ServiceCatalogResultMapper resultMapper;

    @Override
    public ServiceCatalogResult create(CreateServiceCatalogCommand command) {
        if (command == null) {
            throw new ValidationException("Create service catalog command is required.");
        }

        Instant now = clockPort.now();
        UUID actorId = currentUserPort.getCurrentUserId();
        ServiceCatalog serviceCatalog = ServiceCatalog.create(
                UUID.randomUUID(),
                command.serviceCode(),
                command.serviceName(),
                now
        );
        ServicePrice servicePrice = ServicePrice.create(
                UUID.randomUUID(),
                serviceCatalog.getId(),
                command.price(),
                command.effectiveFrom(),
                now,
                actorId
        );

        validateUniqueness(serviceCatalog);

        try {
            ServiceCatalog savedCatalog = serviceCatalogRepository.save(serviceCatalog);
            ServicePrice savedPrice = servicePriceRepository.save(servicePrice);
            auditCreation(savedCatalog, savedPrice, actorId, now);
            return resultMapper.toResult(savedCatalog, savedPrice);
        } catch (DataIntegrityViolationException exception) {
            throw ServiceCatalogConflictTranslator.translate(exception);
        }
    }

    private void validateUniqueness(ServiceCatalog serviceCatalog) {
        if (serviceCatalogRepository.existsByServiceCode(serviceCatalog.getServiceCode())) {
            throw new ValidationException("Service code already exists.");
        }
        if (serviceCatalogRepository.existsByNormalizedServiceName(
                normalize(serviceCatalog.getServiceName()),
                null
        )) {
            throw new ValidationException("Service name already exists.");
        }
    }

    private void auditCreation(
            ServiceCatalog serviceCatalog,
            ServicePrice servicePrice,
            UUID actorId,
            Instant now
    ) {
        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.CREATE,
                ResourceType.SERVICE_CATALOG,
                serviceCatalog.getId(),
                "Service created: " + serviceCatalog.getServiceCode(),
                null,
                now
        ));
        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.CREATE,
                ResourceType.SERVICE_PRICE,
                servicePrice.getId(),
                "Initial price created for service: " + serviceCatalog.getServiceCode(),
                null,
                now
        ));
    }

    private static String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
