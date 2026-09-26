package com.benhsoan.domain.auth.exception;

import java.util.Collections;
import java.util.List;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import lombok.Getter;

@Getter
public class WeakPasswordException extends AuthException {

    private final List<String> violations;

    public WeakPasswordException(List<String> violations) {
        super(
                DomainErrorCode.WEAK_PASSWORD,
                violations == null || violations.isEmpty()
                        ? "Password does not meet minimum strength requirements."
                        : String.join("; ", violations)
        );
        this.violations = violations == null ? Collections.emptyList() : List.copyOf(violations);
    }
}
