package com.benhsoan.domain.shared.Guard;

import com.benhsoan.domain.shared.exception.ValidationException;

public final class Guard {

    private Guard() {
    }

    public static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required.");
        }
        return value;
    }

    public static <T> T require(T value, String field) {
        if (value == null) {
            throw new ValidationException(field + " is required.");
        }
        return value;
    }
}
