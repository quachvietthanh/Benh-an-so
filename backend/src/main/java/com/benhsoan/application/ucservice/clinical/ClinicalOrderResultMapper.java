package com.benhsoan.application.ucservice.clinical;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.clinical.ClinicalOrder;
import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.domain.clinical.ClinicalServiceCatalog;
import com.benhsoan.port.dto.result.ClinicalOrderResult;
import com.benhsoan.port.dto.result.ClinicalServiceCatalogResult;

@Component
public class ClinicalOrderResultMapper {

    public ClinicalServiceCatalogResult toResult(ClinicalServiceCatalog service) {
        return new ClinicalServiceCatalogResult(
                service.getId(), service.getServiceCode(), service.getServiceName(), service.getServiceType(),
                service.getResultDataType(), service.getUnit(), service.getReferenceRange(), service.getDescription(),
                service.getCreatedAt(), service.getUpdatedAt()
        );
    }

    public ClinicalOrderResult toResult(ClinicalOrder order, List<ClinicalOrderItem> items) {
        List<ClinicalOrderResult.OrderItemResult> itemResults = items.stream()
                .map(item -> new ClinicalOrderResult.OrderItemResult(
                        item.getId(), item.getServiceCode(), item.getServiceName(), item.getInstruction(),
                        item.getStatus().name()
                ))
                .toList();
        return new ClinicalOrderResult(
                order.getId(), order.getOrderCode(), order.getVisitId(), order.getPatientId(), order.getOrderedBy(),
                order.getClinicalReason(), order.getStatus().name(), order.getOrderedAt(), order.getCompletedAt(),
                itemResults
        );
    }
}
