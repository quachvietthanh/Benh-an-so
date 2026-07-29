package com.benhsoan.persistence.mapper.clinical;

import org.springframework.stereotype.Component;
import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.persistence.entity.clinical.ClinicalOrderItemEntity;

@Component
public class ClinicalOrderItemPersistenceMapper {

    public ClinicalOrderItem toDomain(ClinicalOrderItemEntity e) {
        return e == null ? null : ClinicalOrderItem.restore(e.getId(), e.getClinicalOrderId(), e.getClinicalServiceId(), e.getServiceCode(), e.getServiceName(), e.getInstruction(), e.getStatus(), e.getCreatedAt(), e.getUpdatedAt());
    }

    public ClinicalOrderItemEntity toEntity(ClinicalOrderItem d) {
        return d == null ? null : ClinicalOrderItemEntity.builder().id(d.getId()).clinicalOrderId(d.getClinicalOrderId()).clinicalServiceId(d.getClinicalServiceId()).serviceCode(d.getServiceCode()).serviceName(d.getServiceName()).instruction(d.getInstruction()).status(d.getStatus()).createdAt(d.getCreatedAt()).updatedAt(d.getUpdatedAt()).build();
    }
}
