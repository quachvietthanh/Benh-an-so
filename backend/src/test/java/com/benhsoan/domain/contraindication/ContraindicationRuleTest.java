package com.benhsoan.domain.contraindication;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.contraindication.enums.ContraindicationSeverity;
import com.benhsoan.domain.contraindication.enums.ContraindicationType;
import com.benhsoan.domain.shared.exception.ValidationException;

class ContraindicationRuleTest {

    private static final Instant NOW = Instant.parse("2026-08-20T00:00:00Z");

    @Test
    void rejectsRuleWithoutTarget() {
        assertThrows(ValidationException.class, () -> ContraindicationRule.restore(
                UUID.randomUUID(), null, null, ContraindicationType.AGE,
                null, 15, null, ContraindicationSeverity.CONTRAINDICATED,
                "message", null, true, NOW, null));
    }

    @Test
    void rejectsAgeRuleWithoutAgeBound() {
        assertThrows(ValidationException.class, () -> ContraindicationRule.restore(
                UUID.randomUUID(), null, "Aspirin", ContraindicationType.AGE,
                null, null, null, ContraindicationSeverity.CONTRAINDICATED,
                "message", null, true, NOW, null));
    }

    @Test
    void rejectsAgeRuleWithMinGreaterThanMax() {
        assertThrows(ValidationException.class, () -> ContraindicationRule.restore(
                UUID.randomUUID(), null, "Aspirin", ContraindicationType.AGE,
                18, 15, null, ContraindicationSeverity.CONTRAINDICATED,
                "message", null, true, NOW, null));
    }

    @Test
    void rejectsDiseaseRuleWithoutDiagnosis() {
        assertThrows(ValidationException.class, () -> ContraindicationRule.restore(
                UUID.randomUUID(), null, "Ibuprofen", ContraindicationType.DISEASE,
                null, null, null, ContraindicationSeverity.MODERATE,
                "message", null, true, NOW, null));
    }

    @Test
    void rejectsBlankMessage() {
        assertThrows(ValidationException.class, () -> ContraindicationRule.restore(
                UUID.randomUUID(), null, "Aspirin", ContraindicationType.AGE,
                null, 15, null, ContraindicationSeverity.CONTRAINDICATED,
                "   ", null, true, NOW, null));
    }
}
