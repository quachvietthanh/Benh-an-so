package com.benhsoan.domain.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VietnameseTextNormalizer Tests")
class VietnameseTextNormalizerTest {

    @Test
    @DisplayName("Strips Vietnamese diacritics and lowercases")
    void stripsDiacritics() {
        assertEquals("viem hong cap", VietnameseTextNormalizer.normalize("Viêm họng cấp"));
    }

    @Test
    @DisplayName("Normalizes đ and Đ to d")
    void normalizesDStroke() {
        assertEquals("dau that lung", VietnameseTextNormalizer.normalize("Đau thắt lưng"));
        assertEquals("da day", VietnameseTextNormalizer.normalize("Đa dày"));
        assertEquals("da day", VietnameseTextNormalizer.normalize("đa dày"));
    }

    @Test
    @DisplayName("Lowercases uppercase input")
    void lowercases() {
        assertEquals("tang huyet ap", VietnameseTextNormalizer.normalize("TĂNG HUYẾT ÁP"));
    }

    @Test
    @DisplayName("Collapses repeated whitespace")
    void collapsesWhitespace() {
        assertEquals("viem hong", VietnameseTextNormalizer.normalize("  viêm    họng  "));
    }

    @Test
    @DisplayName("Trims leading and trailing whitespace")
    void trimsWhitespace() {
        assertEquals("dau dau", VietnameseTextNormalizer.normalize("   đau đầu   "));
    }

    @Test
    @DisplayName("Returns null for null input")
    void returnsNullForNull() {
        assertNull(VietnameseTextNormalizer.normalize(null));
    }

    @Test
    @DisplayName("Returns empty string for blank input")
    void returnsEmptyForBlank() {
        assertEquals("", VietnameseTextNormalizer.normalize(""));
        assertEquals("", VietnameseTextNormalizer.normalize("   "));
    }

    @Test
    @DisplayName("Preserves ASCII code search text")
    void preservesCode() {
        assertEquals("j02.9", VietnameseTextNormalizer.normalize("J02.9"));
    }
}
