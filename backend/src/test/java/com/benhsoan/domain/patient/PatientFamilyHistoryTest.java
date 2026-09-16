package com.benhsoan.domain.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.shared.exception.ValidationException;

class PatientFamilyHistoryTest {

    private static final Instant NOW = Instant.parse("2026-09-10T10:00:00Z");

    @Test
    void createsValidFamilyHistory() {
        UUID patientId = UUID.randomUUID();
        UUID diagnosisCatalogId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        PatientFamilyHistory familyHistory = PatientFamilyHistory.create(
                patientId, "Bố", diagnosisCatalogId, null, createdBy, NOW);

        assertEquals(patientId, familyHistory.getPatientId());
        assertEquals("Bố", familyHistory.getRelationship());
        assertEquals(diagnosisCatalogId, familyHistory.getDiagnosisCatalogId());
        assertTrue(familyHistory.isActive());
    }

    @Test
    void rejectsBlankRelationship() {
        assertThrows(ValidationException.class, () -> PatientFamilyHistory.create(
                UUID.randomUUID(), "   ", UUID.randomUUID(), null, UUID.randomUUID(), NOW));
    }

    @Test
    void rejectsMissingDiagnosisCatalogId() {
        assertThrows(ValidationException.class, () -> PatientFamilyHistory.create(
                UUID.randomUUID(), "Mẹ", null, null, UUID.randomUUID(), NOW));
    }

    @Test
    void trimsRelationship() {
        PatientFamilyHistory familyHistory = PatientFamilyHistory.create(
                UUID.randomUUID(), "  Bố  ", UUID.randomUUID(), null, UUID.randomUUID(), NOW);
        assertEquals("Bố", familyHistory.getRelationship());
    }

    @Test
    void acceptsRelationshipAtMaxLength() {
        String relationship = "a".repeat(PatientFamilyHistory.MAX_RELATIONSHIP_LENGTH);

        PatientFamilyHistory familyHistory = PatientFamilyHistory.create(
                UUID.randomUUID(), relationship, UUID.randomUUID(), null, UUID.randomUUID(), NOW);

        assertEquals(relationship, familyHistory.getRelationship());
    }

    @Test
    void rejectsRelationshipOverMaxLength() {
        String relationship = "a".repeat(PatientFamilyHistory.MAX_RELATIONSHIP_LENGTH + 1);

        assertThrows(ValidationException.class, () -> PatientFamilyHistory.create(
                UUID.randomUUID(), relationship, UUID.randomUUID(), null, UUID.randomUUID(), NOW));
    }

    @Test
    void deactivatesFamilyHistory() {
        PatientFamilyHistory familyHistory = PatientFamilyHistory.create(
                UUID.randomUUID(), "Bố", UUID.randomUUID(), null, UUID.randomUUID(), NOW);

        UUID updatedBy = UUID.randomUUID();
        familyHistory.deactivate(updatedBy, NOW.plusSeconds(60));

        assertFalse(familyHistory.isActive());
        assertEquals(updatedBy, familyHistory.getUpdatedBy());
    }
}
