package com.benhsoan.application.ucservice.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.druginteraction.DrugInteractionRule;
import com.benhsoan.domain.druginteraction.enums.InteractionSeverity;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.prescription.CheckDrugInteractionCommand;
import com.benhsoan.port.dto.result.DrugInteractionWarningResult;
import com.benhsoan.port.outbound.repository.druginteraction.DrugInteractionRuleRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;

@ExtendWith(MockitoExtension.class)
class CheckDrugInteractionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-05T02:00:00Z");

    @Mock private MedicineRepository medicineRepository;
    @Mock private DrugInteractionRuleRepository ruleRepository;

    private CheckDrugInteractionService service;

    @BeforeEach
    void setUp() {
        service = new CheckDrugInteractionService(
                medicineRepository,
                ruleRepository
        );
    }

    @Test
    @DisplayName("Finds an interaction regardless of ingredient order (bidirectional check)")
    void findsInteractionRegardlessOfIngredientOrder() {
        UUID aspirinId = UUID.randomUUID();
        UUID warfarinId = UUID.randomUUID();
        DrugInteractionRule rule = rule(
                "Aspirin",
                "Warfarin",
                InteractionSeverity.SEVERE
        );

        stubMedicine(aspirinId, "Aspirin");
        stubMedicine(warfarinId, "Warfarin");
        stubRuleForPair("Aspirin", "Warfarin", rule);

        List<DrugInteractionWarningResult> firstOrder = service.check(
                new CheckDrugInteractionCommand(List.of(aspirinId, warfarinId))
        );
        List<DrugInteractionWarningResult> reversedOrder = service.check(
                new CheckDrugInteractionCommand(List.of(warfarinId, aspirinId))
        );

        assertEquals(1, firstOrder.size());
        assertEquals(aspirinId, firstOrder.getFirst().drugIdA());
        assertEquals(warfarinId, firstOrder.getFirst().drugIdB());
        assertEquals(InteractionSeverity.SEVERE, firstOrder.getFirst().severity());

        assertEquals(1, reversedOrder.size());
        assertEquals(warfarinId, reversedOrder.getFirst().drugIdA());
        assertEquals(aspirinId, reversedOrder.getFirst().drugIdB());
        assertEquals(InteractionSeverity.SEVERE, reversedOrder.getFirst().severity());
    }

    @Test
    @DisplayName("Returns an empty list when no interaction rule matches")
    void returnsEmptyListWhenNoInteraction() {
        UUID paracetamolId = UUID.randomUUID();
        UUID amoxicillinId = UUID.randomUUID();

        stubMedicine(paracetamolId, "Paracetamol");
        stubMedicine(amoxicillinId, "Amoxicillin");
        when(ruleRepository.findActiveRulesByIngredients(any()))
                .thenReturn(List.of());

        List<DrugInteractionWarningResult> warnings = service.check(
                new CheckDrugInteractionCommand(
                        List.of(paracetamolId, amoxicillinId)
                )
        );

        assertTrue(warnings.isEmpty());
    }

    @Test
    @DisplayName("Returns multiple warnings sorted by severity (CONTRAINDICATED first)")
    void returnsMultipleWarningsSortedBySeverity() {
        UUID drugA = UUID.randomUUID();
        UUID drugB = UUID.randomUUID();
        UUID drugC = UUID.randomUUID();

        stubMedicine(drugA, "IngredientA");
        stubMedicine(drugB, "IngredientB");
        stubMedicine(drugC, "IngredientC");

        DrugInteractionRule moderate = rule(
                "IngredientA",
                "IngredientB",
                InteractionSeverity.MODERATE
        );
        DrugInteractionRule contraindicated = rule(
                "IngredientA",
                "IngredientC",
                InteractionSeverity.CONTRAINDICATED
        );
        DrugInteractionRule severe = rule(
                "IngredientB",
                "IngredientC",
                InteractionSeverity.SEVERE
        );

        when(ruleRepository.findActiveRulesByIngredients(any()))
                .thenReturn(List.of(moderate, contraindicated, severe));

        List<DrugInteractionWarningResult> warnings = service.check(
                new CheckDrugInteractionCommand(List.of(drugA, drugB, drugC))
        );

        assertEquals(3, warnings.size());
        assertEquals(InteractionSeverity.CONTRAINDICATED, warnings.get(0).severity());
        assertEquals(InteractionSeverity.SEVERE, warnings.get(1).severity());
        assertEquals(InteractionSeverity.MODERATE, warnings.get(2).severity());
    }

    @Test
    @DisplayName("Returns an empty list when fewer than two drugs are provided")
    void returnsEmptyListForLessThanTwoDrugs() {
        List<DrugInteractionWarningResult> single = service.check(
                new CheckDrugInteractionCommand(List.of(UUID.randomUUID()))
        );
        List<DrugInteractionWarningResult> empty = service.check(
                new CheckDrugInteractionCommand(List.of())
        );

        assertTrue(single.isEmpty());
        assertTrue(empty.isEmpty());
        verify(medicineRepository, never()).findById(any());
        verify(ruleRepository, never()).findActiveRulesByIngredients(any());
    }

    @Test
    @DisplayName("Skips a pair when both medicines share the same active ingredient")
    void skipsPairWithSameActiveIngredient() {
        UUID brandId = UUID.randomUUID();
        UUID genericId = UUID.randomUUID();

        stubMedicine(brandId, "Paracetamol");
        stubMedicine(genericId, "Paracetamol");
        when(ruleRepository.findActiveRulesByIngredients(any()))
                .thenReturn(List.of());

        List<DrugInteractionWarningResult> warnings = service.check(
                new CheckDrugInteractionCommand(List.of(brandId, genericId))
        );

        assertTrue(warnings.isEmpty());
    }

    @Test
    @DisplayName("Deduplicates repeated drug ids")
    void deduplicatesRepeatedDrugIds() {
        UUID aspirinId = UUID.randomUUID();

        List<DrugInteractionWarningResult> warnings = service.check(
                new CheckDrugInteractionCommand(
                        List.of(aspirinId, aspirinId, aspirinId)
                )
        );

        assertTrue(warnings.isEmpty());
        verify(medicineRepository, never()).findById(any());
    }


    @Test
    @DisplayName("Throws when a drug id is unknown")
    void throwsWhenDrugIdIsUnknown() {
        UUID unknownId = UUID.randomUUID();
        when(medicineRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> service.check(new CheckDrugInteractionCommand(
                        List.of(unknownId, UUID.randomUUID())
                )));
    }

    private void stubMedicine(UUID id, String activeIngredient) {
        when(medicineRepository.findById(id))
                .thenReturn(Optional.of(medicine(id, activeIngredient)));
    }

    private void stubRuleForPair(
            String ingredientA,
            String ingredientB,
            DrugInteractionRule rule
    ) {
        when(ruleRepository.findActiveRulesByIngredients(any()))
                .thenAnswer(invocation -> {
                    Set<String> ingredients = invocation.getArgument(0);
                    return ingredients.containsAll(Set.of(ingredientA, ingredientB))
                            ? List.of(rule)
                            : List.of();
                });
    }

    private Medicine medicine(UUID id, String activeIngredient) {
        return Medicine.restore(
                id,
                "MED-" + id.toString().substring(0, 4),
                activeIngredient,
                activeIngredient,
                "500 mg",
                DosageForm.TABLET,
                "tablet",
                AdministrationRoute.ORAL,
                true,
                NOW,
                null,
                0
        );
    }

    private DrugInteractionRule rule(
            String ingredientA,
            String ingredientB,
            InteractionSeverity severity
    ) {
        return DrugInteractionRule.restore(
                UUID.randomUUID(),
                ingredientA,
                ingredientB,
                severity,
                "Interaction detected",
                "Monitor patient closely",
                true,
                NOW,
                null
        );
    }
}
