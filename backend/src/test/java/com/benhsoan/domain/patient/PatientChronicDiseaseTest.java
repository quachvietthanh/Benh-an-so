package com.benhsoan.domain.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.shared.exception.ValidationException;

class PatientChronicDiseaseTest {

    private static final Instant NOW = Instant.parse("2026-09-10T10:00:00Z");

    @Test
    void createsValidChronicDisease() {
        UUID patientId = UUID.randomUUID();
        UUID diagnosisCatalogId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        PatientChronicDisease disease = PatientChronicDisease.create(
                patientId, diagnosisCatalogId, 2015, "Đang điều trị", createdBy, NOW);

        assertEquals(patientId, disease.getPatientId());
        assertEquals(diagnosisCatalogId, disease.getDiagnosisCatalogId());
        assertEquals(2015, disease.getYearDetected());
        assertEquals("Đang điều trị", disease.getNotes());
        assertTrue(disease.isActive());
        assertEquals(createdBy, disease.getCreatedBy());
    }

    @Test
    void rejectsMissingDiagnosisCatalogId() {
        assertThrows(ValidationException.class, () -> PatientChronicDisease.create(
                UUID.randomUUID(), null, null, null, UUID.randomUUID(), NOW));
    }

    @Test
    void rejectsMissingPatientId() {
        assertThrows(ValidationException.class, () -> PatientChronicDisease.create(
                null, UUID.randomUUID(), null, null, UUID.randomUUID(), NOW));
    }

    @Test
    void rejectsYearBefore1900() {
        assertThrows(ValidationException.class, () -> PatientChronicDisease.create(
                UUID.randomUUID(), UUID.randomUUID(), 1899, null, UUID.randomUUID(), NOW));
    }

    @Test
    void rejectsYearInTheFuture() {
        int futureYear = java.time.Year.now().getValue() + 1;
        assertThrows(ValidationException.class, () -> PatientChronicDisease.create(
                UUID.randomUUID(), UUID.randomUUID(), futureYear, null, UUID.randomUUID(), NOW));
    }

    @Test
    void trimsNotesAndAllowsNullYearAndNotes() {
        PatientChronicDisease disease = PatientChronicDisease.create(
                UUID.randomUUID(), UUID.randomUUID(), null, "  ghi chú  ", UUID.randomUUID(), NOW);

        assertNull(disease.getYearDetected());
        assertEquals("ghi chú", disease.getNotes());
    }

    @Test
    void deactivatesChronicDisease() {
        PatientChronicDisease disease = PatientChronicDisease.create(
                UUID.randomUUID(), UUID.randomUUID(), null, null, UUID.randomUUID(), NOW);

        UUID updatedBy = UUID.randomUUID();
        disease.deactivate(updatedBy, NOW.plusSeconds(60));

        assertFalse(disease.isActive());
        assertEquals(updatedBy, disease.getUpdatedBy());
    }
}
