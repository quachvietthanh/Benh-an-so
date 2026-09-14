package com.benhsoan.domain.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

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

    @Test
    void trimsAbbreviationAndNormalizesBlankToNull() {
        DiagnosisCatalog withAbbreviation = DiagnosisCatalog.create(
                "I10", "Tăng huyết áp", "  THA  ", "Hệ tuần hoàn", null
        );
        assertEquals("THA", withAbbreviation.getAbbreviation());

        DiagnosisCatalog blankAbbreviation = DiagnosisCatalog.create(
                "I10", "Tăng huyết áp", "   ", "Hệ tuần hoàn", null
        );
        assertEquals(null, blankAbbreviation.getAbbreviation());
    }

    @Test
    void computesAccentInsensitiveNormalizedColumns() {
        DiagnosisCatalog catalog = DiagnosisCatalog.create(
                "J02.9", "Viêm họng cấp", "VHC", "Hệ hô hấp", null
        );

        assertEquals("viem hong cap", catalog.getNameNorm());
        assertEquals("vhc", catalog.getAbbreviationNorm());
    }

    @Test
    void recomputesNormalizedColumnsOnRename() {
        DiagnosisCatalog catalog = DiagnosisCatalog.create(
                "J02.9", "Viêm họng cấp", "VHC", "Hệ hô hấp", null
        );
        assertEquals("viem hong cap", catalog.getNameNorm());

        catalog.updateInformation("Viêm amidan cấp", "VHC", "Hệ hô hấp", null,
                Instant.parse("2026-08-26T00:00:00Z"));

        assertEquals("Viêm amidan cấp", catalog.getName());
        assertEquals("viem amidan cap", catalog.getNameNorm());
        assertEquals("vhc", catalog.getAbbreviationNorm());
    }

    @Test
    void recomputesAbbreviationNormOnAbbreviationUpdate() {
        DiagnosisCatalog catalog = DiagnosisCatalog.create(
                "I10", "Tăng huyết áp", "THA", "Hệ tuần hoàn", null
        );
        assertEquals("tha", catalog.getAbbreviationNorm());

        catalog.updateInformation("Tăng huyết áp", "HA", "Hệ tuần hoàn", null,
                Instant.parse("2026-08-26T00:00:00Z"));

        assertEquals("HA", catalog.getAbbreviation());
        assertEquals("ha", catalog.getAbbreviationNorm());
    }

    @Test
    void nullAbbreviationKeepsNullAbbreviationNorm() {
        DiagnosisCatalog catalog = DiagnosisCatalog.create(
                "I10", "Tăng huyết áp", "THA", "Hệ tuần hoàn", null
        );

        catalog.updateInformation("Tăng huyết áp", null, "Hệ tuần hoàn", null,
                Instant.parse("2026-08-26T00:00:00Z"));

        assertNull(catalog.getAbbreviation());
        assertNull(catalog.getAbbreviationNorm());
        assertEquals("tang huyet ap", catalog.getNameNorm());
    }
}
