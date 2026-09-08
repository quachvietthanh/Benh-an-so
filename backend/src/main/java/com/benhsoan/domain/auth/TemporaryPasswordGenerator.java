package com.benhsoan.domain.auth;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility generator for temporary passwords fulfilling QTN-28 strength requirements.
 */
public final class TemporaryPasswordGenerator {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // exclude I, O to avoid confusion
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz"; // exclude l, o
    private static final String DIGITS = "23456789";                  // exclude 0, 1
    private static final String ALL_CHARS = UPPER + LOWER + DIGITS;
    private static final int DEFAULT_LENGTH = 10;

    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPasswordGenerator() {
    }

    public static String generate() {
        return generate(DEFAULT_LENGTH);
    }

    public static String generate(int length) {
        int targetLength = Math.max(length, PasswordPolicyValidator.MIN_LENGTH);

        List<Character> chars = new ArrayList<>(targetLength);

        // Ensure at least 1 uppercase, 1 lowercase, 1 digit
        chars.add(UPPER.charAt(RANDOM.nextInt(UPPER.length())));
        chars.add(LOWER.charAt(RANDOM.nextInt(LOWER.length())));
        chars.add(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));

        // Fill remaining with random characters from ALL_CHARS
        for (int i = 3; i < targetLength; i++) {
            chars.add(ALL_CHARS.charAt(RANDOM.nextInt(ALL_CHARS.length())));
        }

        // Shuffle to randomize character positions
        Collections.shuffle(chars, RANDOM);

        StringBuilder sb = new StringBuilder(targetLength);
        for (char c : chars) {
            sb.append(c);
        }
        return sb.toString();
    }
}
