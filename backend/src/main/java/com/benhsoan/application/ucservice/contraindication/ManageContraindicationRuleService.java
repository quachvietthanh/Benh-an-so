package com.benhsoan.application.ucservice.contraindication;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.contraindication.ContraindicationRule;
import com.benhsoan.domain.contraindication.enums.ContraindicationSeverity;
import com.benhsoan.domain.contraindication.enums.ContraindicationType;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.inbound.contraindication.ActivateContraindicationRuleUseCase;
import com.benhsoan.port.inbound.contraindication.CreateContraindicationRuleUseCase;
import com.benhsoan.port.inbound.contraindication.DeactivateContraindicationRuleUseCase;
import com.benhsoan.port.inbound.contraindication.SearchContraindicationRuleUseCase;
import com.benhsoan.port.inbound.contraindication.UpdateContraindicationRuleUseCase;
import com.benhsoan.port.outbound.repository.contraindication.ContraindicationRuleRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ManageContraindicationRuleService implements
        SearchContraindicationRuleUseCase,
        CreateContraindicationRuleUseCase,
        UpdateContraindicationRuleUseCase,
        DeactivateContraindicationRuleUseCase,
        ActivateContraindicationRuleUseCase {

    private final ContraindicationRuleRepository ruleRepository;
    private final MedicineRepository medicineRepository;
    private final ClockPort clockPort;

    @Override
    @Transactional(readOnly = true)
    public Page<ContraindicationRule> search(
            String activeIngredient,
            ContraindicationType type,
            ContraindicationSeverity severity,
            Boolean active,
            Pageable pageable
    ) {
        String normalizedIngredient = activeIngredient == null || activeIngredient.isBlank() ? null : activeIngredient.trim();
        return ruleRepository.search(normalizedIngredient, type, severity, active, pageable);
    }

    @Override
    public ContraindicationRule create(
            UUID medicineId,
            String activeIngredient,
            ContraindicationType type,
            Integer minAgeYears,
            Integer maxAgeYears,
            UUID diagnosisCatalogId,
            ContraindicationSeverity severity,
            String message,
            String recommendation
    ) {
        validateMedicine(medicineId);
        Instant now = clockPort.now();
        ContraindicationRule rule = ContraindicationRule.create(
                medicineId,
                activeIngredient,
                type,
                minAgeYears,
                maxAgeYears,
                diagnosisCatalogId,
                severity,
                message,
                recommendation,
                now
        );
        return ruleRepository.save(rule);
    }

    @Override
    public ContraindicationRule update(
            UUID id,
            UUID medicineId,
            String activeIngredient,
            ContraindicationType type,
            Integer minAgeYears,
            Integer maxAgeYears,
            UUID diagnosisCatalogId,
            ContraindicationSeverity severity,
            String message,
            String recommendation
    ) {
        ContraindicationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Contraindication rule not found with id: " + id));
        validateMedicine(medicineId);
        Instant now = clockPort.now();
        rule.update(
                medicineId,
                activeIngredient,
                type,
                minAgeYears,
                maxAgeYears,
                diagnosisCatalogId,
                severity,
                message,
                recommendation,
                now
        );
        return ruleRepository.save(rule);
    }

    @Override
    public void deactivate(UUID id) {
        ContraindicationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Contraindication rule not found with id: " + id));
        rule.deactivate(clockPort.now());
        ruleRepository.save(rule);
    }

    @Override
    public void activate(UUID id) {
        ContraindicationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Contraindication rule not found with id: " + id));
        rule.activate(clockPort.now());
        ruleRepository.save(rule);
    }

    private void validateMedicine(UUID medicineId) {
        if (medicineId != null && medicineRepository.findById(medicineId).isEmpty()) {
            throw new ValidationException("Medicine not found with id: " + medicineId);
        }
    }
}
