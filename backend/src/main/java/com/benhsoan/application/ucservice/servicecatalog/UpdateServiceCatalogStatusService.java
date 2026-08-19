package com.benhsoan.application.ucservice.servicecatalog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.servicecatalog.ServiceCatalog;
import com.benhsoan.domain.servicecatalog.ServicePrice;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.servicecatalog.ServiceCatalogResult;
import com.benhsoan.port.inbound.servicecatalog.UpdateServiceCatalogStatusUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.servicecatalog.ServiceCatalogRepository;
import com.benhsoan.port.outbound.repository.servicecatalog.ServicePriceRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateServiceCatalogStatusService implements UpdateServiceCatalogStatusUseCase {

    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ServicePriceRepository servicePriceRepository;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final ServiceCatalogResultMapper resultMapper;

    @Override
    public ServiceCatalogResult updateStatus(UUID serviceCatalogId, boolean active) {
        if (serviceCatalogId == null) {
            throw new ValidationException("Service catalog id is required.");
        }

        ServiceCatalog serviceCatalog = serviceCatalogRepository.findById(serviceCatalogId)
                .orElseThrow(() -> new ValidationException(
                        "Service catalog not found: " + serviceCatalogId
                ));
        List<ServicePrice> priceHistory = servicePriceRepository
                .findAllByServiceCatalogId(serviceCatalogId);

        if (serviceCatalog.isActive() == active) {
            return resultMapper.toResult(
                    serviceCatalog,
                    priceHistory.isEmpty() ? null : priceHistory.getFirst()
            );
        }

        Instant now = clockPort.now();
        UUID actorId = currentUserPort.getCurrentUserId();
        if (active) {
            serviceCatalog.activate(now);
        } else {
            serviceCatalog.deactivate(now);
        }

        ServiceCatalog savedCatalog = serviceCatalogRepository.save(serviceCatalog);
        auditLogRepository.save(AuditLog.create(
                actorId,
                active ? ActionType.ACTIVATE : ActionType.DEACTIVATE,
                ResourceType.SERVICE_CATALOG,
                serviceCatalogId,
                "Service status changed.",
                null,
                now
        ));

        return resultMapper.toResult(
                savedCatalog,
                priceHistory.isEmpty() ? null : priceHistory.getFirst()
        );
    }
}
