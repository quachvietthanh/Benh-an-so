package com.benhsoan.application.ucservice.prescription;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.druginteraction.DrugInteractionRule;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.prescription.CheckDrugInteractionCommand;
import com.benhsoan.port.dto.result.DrugInteractionWarningResult;
import com.benhsoan.port.inbound.prescription.CheckDrugInteractionUseCase;
import com.benhsoan.port.outbound.repository.druginteraction.DrugInteractionRuleRepositoryPort;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckDrugInteractionService
        implements CheckDrugInteractionUseCase {

    private final MedicineRepository medicineRepository;

    private final DrugInteractionRuleRepositoryPort ruleRepository;

    @Override
    public List<DrugInteractionWarningResult> check(
            CheckDrugInteractionCommand command
    ) {
        requireCommand(command);

        List<UUID> drugIds = distinctDrugIds(command.drugIds());
        if (drugIds.size() < 2) {
            return List.of();
        }

        List<Medicine> medicines = loadMedicines(drugIds);

        List<DrugInteractionWarningResult> warnings = new ArrayList<>();
        for (int i = 0; i < medicines.size(); i++) {
            Medicine first = medicines.get(i);
            for (int j = i + 1; j < medicines.size(); j++) {
                Medicine second = medicines.get(j);
                if (first.getActiveIngredient()
                        .equalsIgnoreCase(second.getActiveIngredient())) {
                    continue;
                }

                ruleRepository
                        .findActiveRuleBetween(
                                first.getActiveIngredient(),
                                second.getActiveIngredient()
                        )
                        .ifPresent(rule -> warnings.add(
                                toWarning(rule, first, second)
                        ));
            }
        }

        // InteractionSeverity is declared in ascending order
        // (MILD < MODERATE < SEVERE < CONTRAINDICATED), so reversed ordinal
        // ordering puts the most severe warnings first.
        warnings.sort(Comparator
                .comparingInt(
                        (DrugInteractionWarningResult warning)
                                -> warning.severity().ordinal()
                )
                .reversed());

        return List.copyOf(warnings);
    }

    private DrugInteractionWarningResult toWarning(
            DrugInteractionRule rule,
            Medicine first,
            Medicine second
    ) {
        return new DrugInteractionWarningResult(
                first.getId(),
                second.getId(),
                rule.getSeverity(),
                rule.getDescription(),
                rule.getClinicalRecommendation()
        );
    }

    private List<UUID> distinctDrugIds(List<UUID> drugIds) {
        if (drugIds == null) {
            return List.of();
        }

        Set<UUID> distinctIds = new LinkedHashSet<>();
        for (UUID drugId : drugIds) {
            if (drugId == null) {
                throw new ValidationException("Drug id is required.");
            }
            distinctIds.add(drugId);
        }
        return List.copyOf(distinctIds);
    }

    private List<Medicine> loadMedicines(List<UUID> drugIds) {
        List<Medicine> medicines = new ArrayList<>(drugIds.size());
        for (UUID drugId : drugIds) {
            Medicine medicine = medicineRepository
                    .findById(drugId)
                    .orElseThrow(() -> new ValidationException(
                            "Medicine not found: " + drugId
                    ));

            if (!medicine.isActive()) {
                throw new ValidationException(
                        "Inactive medicine cannot be checked: " + drugId
                );
            }

            medicines.add(medicine);
        }
        return List.copyOf(medicines);
    }

    private void requireCommand(CheckDrugInteractionCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Check drug interaction command is required."
            );
        }
    }
}
