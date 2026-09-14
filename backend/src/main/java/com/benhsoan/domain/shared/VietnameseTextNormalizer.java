package com.benhsoan.domain.shared;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Deterministic Vietnamese text normalization for keyword search.
 *
 * <p>Produces a lowercase, diacritic-free, whitespace-collapsed form that
 * makes accented and unaccented Vietnamese input comparable. The Vietnamese
 * {@code đ}/{@code Đ} is a standalone letter and is therefore mapped manually
 * after {@link Normalizer} strips combining tone marks.</p>
 */
public final class VietnameseTextNormalizer {

    private VietnameseTextNormalizer() {
    }

    /**
     * Normalizes the given text.
     *
     * @param input the raw text, may be {@code null}
     * @return {@code null} when {@code input} is {@code null}; otherwise the
     *         lowercase, diacritic-free, whitespace-collapsed form
     */
    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .trim()
                .replaceAll("\\s+", " ");
    }
}
