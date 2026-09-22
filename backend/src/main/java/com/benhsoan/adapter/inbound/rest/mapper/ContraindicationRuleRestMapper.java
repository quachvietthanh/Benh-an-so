package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.contraindication.ContraindicationRuleResponse;
import com.benhsoan.domain.contraindication.ContraindicationRule;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContraindicationRuleRestMapper {

    private final MedicineRepository medicineRepository;

    public ContraindicationRuleResponse toResponse(ContraindicationRule domain) {
        if (domain == null) {
            return null;
        }
        String medicineName = null;
        if (domain.getMedicineId() != null) {
            medicineName = medicineRepository.findById(domain.getMedicineId())
                    .map(m -> m.getMedicineName())
                    .orElse(null);
        }

        return new ContraindicationRuleResponse(
                domain.getId(),
                domain.getMedicineId(),
                medicineName,
                domain.getActiveIngredient(),
                domain.getType(),
                domain.getMinAgeYears(),
                domain.getMaxAgeYears(),
                domain.getDiagnosisCatalogId(),
                null,
                domain.getSeverity(),
                domain.getMessage(),
                domain.getRecommendation(),
                domain.isActive(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
