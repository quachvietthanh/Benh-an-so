package com.benhsoan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.benhsoan.domain.clinical.ReferenceRangeEvaluator;

/**
 * Wires pure domain services that must not depend on Spring directly.
 */
@Configuration
public class ClinicalDomainConfiguration {

    @Bean
    public ReferenceRangeEvaluator referenceRangeEvaluator() {
        return new ReferenceRangeEvaluator();
    }
}
