package com.benhsoan.domain.specialty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.specialty.exception.CannotDeactivateDefaultSpecialtyException;

class SpecialtyTest {

    private final Instant now = Instant.parse("2026-09-18T10:00:00Z");

    @Test
    void createsValidSpecialtyAndNormalizesFields() {
        Specialty specialty = Specialty.create("pediatrics", "  Khoa Nhi  ", " Khám và điều trị nhi ", now);

        assertNotNull(specialty.getId());
        assertEquals("PEDIATRICS", specialty.getCode());
        assertEquals("Khoa Nhi", specialty.getName());
        assertEquals("khoa nhi", specialty.getNameKey());
        assertEquals("Khám và điều trị nhi", specialty.getDescription());
        assertTrue(specialty.isActive());
        assertEquals(now, specialty.getCreatedAt());
        assertEquals(now, specialty.getUpdatedAt());
    }

    @Test
    void rejectsInvalidCodeCharacters() {
        assertThrows(ValidationException.class, () ->
                Specialty.create("PEDIATRICS-1", "Khoa Nhi", null, now));
        assertThrows(ValidationException.class, () ->
                Specialty.create("KHOA NHI", "Khoa Nhi", null, now));
    }

    @Test
    void rejectsCodeExceeding30Characters() {
        String longCode = "A".repeat(31);
        assertThrows(ValidationException.class, () ->
                Specialty.create(longCode, "Khoa Nhi", null, now));
    }

    @Test
    void rejectsNameExceeding100Characters() {
        String longName = "N".repeat(101);
        assertThrows(ValidationException.class, () ->
                Specialty.create("PEDIATRICS", longName, null, now));
    }

    @Test
    void rejectsDescriptionExceeding500Characters() {
        String longDesc = "D".repeat(501);
        assertThrows(ValidationException.class, () ->
                Specialty.create("PEDIATRICS", "Khoa Nhi", longDesc, now));
    }

    @Test
    void deactivatesNormalSpecialty() {
        Specialty specialty = Specialty.create("PEDIATRICS", "Khoa Nhi", null, now);
        Instant later = now.plusSeconds(3600);

        specialty.deactivate(later);

        assertFalse(specialty.isActive());
        assertEquals(later, specialty.getUpdatedAt());
    }

    @Test
    void preventsDeactivationOfDefaultGeneralSpecialty() {
        Specialty general = Specialty.restore(Specialty.GENERAL_ID, "GENERAL", "General", true, now, now);

        assertThrows(CannotDeactivateDefaultSpecialtyException.class, () ->
                general.deactivate(now.plusSeconds(60)));
    }

    @Test
    void activatesAndUpdatesSpecialty() {
        Specialty specialty = Specialty.create("PEDIATRICS", "Khoa Nhi", "Cu", now);
        Instant t1 = now.plusSeconds(60);
        specialty.deactivate(t1);
        assertFalse(specialty.isActive());

        Instant t2 = now.plusSeconds(120);
        specialty.activate(t2);
        assertTrue(specialty.isActive());

        Instant t3 = now.plusSeconds(180);
        specialty.update("Khoa Nhi Mới", "Mới", t3);
        assertEquals("Khoa Nhi Mới", specialty.getName());
        assertEquals("khoa nhi mới", specialty.getNameKey());
        assertEquals("Mới", specialty.getDescription());
        assertEquals(t3, specialty.getUpdatedAt());
    }
}
