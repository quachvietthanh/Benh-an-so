package com.benhsoan.application.ucservice.servicecatalog;

import java.time.Instant;
import java.util.List;
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
import com.benhsoan.port.dto.command.servicecatalog.UpdateServiceCatalogCommand;
import com.benhsoan.port.dto.result.servicecatalog.ServiceCatalogResult;
import com.benhsoan.port.inbound.servicecatalog.UpdateServiceCatalogUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.servicecatalog.ServiceCatalogRepository;
import com.benhsoan.port.outbound.repository.servicecatalog.ServicePriceRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateServiceCatalogService implements UpdateServiceCatalogUseCase {

    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ServicePriceRepository servicePriceRepository;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final ServiceCatalogResultMapper resultMapper;

    @Override
    public ServiceCatalogResult update(UpdateServiceCatalogCommand command) {
        validateCommand(command);
        ServiceCatalog serviceCatalog = serviceCatalogRepository.findById(command.serviceCatalogId())
                .orElseThrow(() -> new ValidationException(
                        "Service catalog not found: " + command.serviceCatalogId()
                ));

        Instant now = clockPort.now();
        UUID actorId = currentUserPort.getCurrentUserId();
        ServicePrice candidatePrice = ServicePrice.create(
                UUID.randomUUID(),
                serviceCatalog.getId(),
                command.price(),
                command.effectiveFrom(),
                now,
                actorId
        );
        List<ServicePrice> priceHistory = servicePriceRepository
                .findAllByServiceCatalogId(serviceCatalog.getId());

        String normalizedName = normalize(command.serviceName());
        if (serviceCatalogRepository.existsByNormalizedServiceName(
                normalizedName,
                serviceCatalog.getId()
        )) {
            throw new ValidationException("Service name already exists.");
        }

        boolean nameChanged = !serviceCatalog.getServiceName().equals(canonicalName(command.serviceName()));
        boolean statusChanged = serviceCatalog.isActive() != command.active();
        boolean priceCreated = shouldCreatePrice(candidatePrice, priceHistory);

        if (nameChanged) {
            serviceCatalog.rename(command.serviceName(), now);
        }
        if (statusChanged) {
            if (command.active()) {
                serviceCatalog.activate(now);
            } else {
                serviceCatalog.deactivate(now);
            }
        }

        try {
            if (nameChanged || statusChanged) {
                serviceCatalog = serviceCatalogRepository.save(serviceCatalog);
            }
            ServicePrice latestPrice = priceHistory.isEmpty() ? null : priceHistory.getFirst();
            if (priceCreated) {
                latestPrice = servicePriceRepository.save(candidatePrice);
            }
            auditChanges(serviceCatalog, candidatePrice, actorId, now, nameChanged, statusChanged, priceCreated);
            return resultMapper.toResult(serviceCatalog, chooseLatest(latestPrice, priceHistory));
        } catch (DataIntegrityViolationException exception) {
            throw ServiceCatalogConflictTranslator.translate(exception);
        }
    }

    private boolean shouldCreatePrice(ServicePrice candidate, List<ServicePrice> history) {
        return history.stream()
                .filter(price -> price.getEffectiveFrom().equals(candidate.getEffectiveFrom()))
                .findFirst()
                .map(existing -> {
                    if (existing.getPrice().compareTo(candidate.getPrice()) != 0) {
                        throw new ValidationException(
                                "A different service price already exists for this effective date."
                        );
                    }
                    return false;
                })
                .orElse(true);
    }

    private ServicePrice chooseLatest(ServicePrice savedOrExisting, List<ServicePrice> history) {
        if (savedOrExisting == null) {
            return null;
        }
        return history.stream()
                .filter(price -> price.getEffectiveFrom().isAfter(savedOrExisting.getEffectiveFrom()))
                .findFirst()
                .orElse(savedOrExisting);
    }

    private void auditChanges(
            ServiceCatalog serviceCatalog,
            ServicePrice candidatePrice,
            UUID actorId,
            Instant now,
            boolean nameChanged,
            boolean statusChanged,
            boolean priceCreated
    ) {
        if (nameChanged || priceCreated) {
            auditLogRepository.save(AuditLog.create(
                    actorId,
                    ActionType.UPDATE,
                    ResourceType.SERVICE_CATALOG,
                    serviceCatalog.getId(),
                    "Service catalog information or price changed.",
                    null,
                    now
            ));
        }
        if (statusChanged) {
            auditLogRepository.save(AuditLog.create(
                    actorId,
                    serviceCatalog.isActive() ? ActionType.ACTIVATE : ActionType.DEACTIVATE,
                    ResourceType.SERVICE_CATALOG,
                    serviceCatalog.getId(),
                    "Service status changed.",
                    null,
                    now
            ));
        }
        if (priceCreated) {
            auditLogRepository.save(AuditLog.create(
                    actorId,
                    ActionType.CREATE,
                    ResourceType.SERVICE_PRICE,
                    candidatePrice.getId(),
                    "Service price version created.",
                    null,
                    now
            ));
        }
    }

    private static void validateCommand(UpdateServiceCatalogCommand command) {
        if (command == null) {
            throw new ValidationException("Update service catalog command is required.");
        }
        if (command.serviceCatalogId() == null) {
            throw new ValidationException("Service catalog id is required.");
        }
    }

    private static String canonicalName(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Service name is required.");
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String normalize(String value) {
        return canonicalName(value).toLowerCase(Locale.ROOT);
    }
}
