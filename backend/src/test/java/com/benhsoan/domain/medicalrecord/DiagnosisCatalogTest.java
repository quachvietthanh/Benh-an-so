package com.benhsoan.domain.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.shared.exception.ValidationException;

class DiagnosisCatalogTest {

    @Test
    void normalizesCodeAndRequiresDiseaseGroup() {
        DiagnosisCatalog catalog = DiagnosisCatalog.create(
                "  j06.9 ", "Nhiễm trùng hô hấp trên", "Hệ hô hấp", null
        );

        assertEquals("J06.9", catalog.getCode());
        assertEquals("Hệ hô hấp", catalog.getDiseaseGroup());
        assertThrows(ValidationException.class, () -> DiagnosisCatalog.create(
                "J00", "Cảm lạnh thông thường", " ", null
        ));
    }
}
