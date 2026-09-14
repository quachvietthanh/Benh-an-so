package com.benhsoan.application.ucservice.clinical;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.clinical.ClinicalReferenceRange;
import com.benhsoan.domain.clinical.ClinicalServiceCatalog;
import com.benhsoan.port.dto.result.ClinicalReferenceRangeResult;
import com.benhsoan.port.dto.result.ClinicalServiceManagementResult;

@Component
public class ClinicalServiceManagementResultMapper {

    public ClinicalServiceManagementResult toResult(ClinicalServiceCatalog service, List<ClinicalReferenceRange> ranges) {
        return new ClinicalServiceManagementResult(
                service.getId(), service.getServiceCatalogId(), service.getServiceCode(), service.getServiceName(),
                service.getServiceType(), service.getResultDataType(), service.getUnit(), service.getReferenceRange(),
                service.getDescription(), service.isActive(), service.getCreatedAt(), service.getUpdatedAt(),
                ranges.stream().map(this::toRangeResult).toList()
        );
    }

    public ClinicalReferenceRangeResult toRangeResult(ClinicalReferenceRange range) {
        return new ClinicalReferenceRangeResult(
                range.getId(), range.getClinicalServiceId(), range.getGender(), range.getMinAge(), range.getMaxAge(),
                range.getLowerBound(), range.getUpperBound(), range.isActive(), range.getCreatedAt(), range.getUpdatedAt()
        );
    }
}
