package com.benhsoan.port.inbound.contraindication;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.contraindication.ContraindicationRule;
import com.benhsoan.domain.contraindication.enums.ContraindicationSeverity;
import com.benhsoan.domain.contraindication.enums.ContraindicationType;

public interface SearchContraindicationRuleUseCase {
    Page<ContraindicationRule> search(
            String activeIngredient,
            ContraindicationType type,
            ContraindicationSeverity severity,
            Boolean active,
            Pageable pageable
    );
}
