package com.benhsoan.application.ucservice.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.servicecatalog.ServicePrice;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.outbound.repository.clinical.BillableClinicalService;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.servicecatalog.ServicePriceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class ClinicalServiceFeeCalculator {

    private static final ZoneId BILLING_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ClinicalOrderItemRepository clinicalOrderItemRepository;
    private final ServicePriceRepository servicePriceRepository;

    List<ClinicalServiceCharge> calculate(UUID visitId, Instant billingAt) {
        LocalDate billingDate = billingAt.atZone(BILLING_ZONE).toLocalDate();
        return clinicalOrderItemRepository.findBillableByVisitId(visitId).stream()
                .map(service -> toCharge(service, billingDate))
                .toList();
    }

    BigDecimal total(List<ClinicalServiceCharge> charges) {
        return charges.stream()
                .map(ClinicalServiceCharge::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ClinicalServiceCharge toCharge(
            BillableClinicalService service,
            LocalDate billingDate
    ) {
        ServicePrice price = servicePriceRepository
                .findEffectivePrice(service.serviceCatalogId(), billingDate)
                .orElseThrow(() -> new ValidationException(
                        "No effective price found for clinical service item: "
                                + service.clinicalOrderItemId()
                ));
        return new ClinicalServiceCharge(
                service.clinicalOrderItemId(),
                service.serviceName(),
                price.getPrice()
        );
    }
}
