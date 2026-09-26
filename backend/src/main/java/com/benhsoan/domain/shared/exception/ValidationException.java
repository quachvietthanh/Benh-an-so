package com.benhsoan.domain.shared.exception;

import java.util.Collections;
import java.util.Map;

public class ValidationException extends DomainException {

    private final String field;
    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        this(DomainErrorCode.VALIDATION_FAILED, (String) null, message);
    }

    public ValidationException(String field, String message) {
        this(DomainErrorCode.VALIDATION_FAILED, field, message);
    }

    public ValidationException(DomainErrorCode code, String field, String message) {
        super(code, (field != null && !field.isBlank() ? field + ": " : "") + message);
        this.field = field;
        this.fieldErrors = (field != null && !field.isBlank() && message != null)
                ? Map.of(field, message)
                : Collections.emptyMap();
    }

    public ValidationException(Map<String, String> fieldErrors, String message) {
        this(DomainErrorCode.VALIDATION_FAILED, fieldErrors, message);
    }

    public ValidationException(DomainErrorCode code, Map<String, String> fieldErrors, String message) {
        super(code, message);
        this.field = (fieldErrors != null && !fieldErrors.isEmpty())
                ? fieldErrors.keySet().iterator().next()
                : null;
        this.fieldErrors = fieldErrors != null ? Map.copyOf(fieldErrors) : Collections.emptyMap();
    }

    protected ValidationException(DomainErrorCode code, String message) {
        this(code, (String) null, message);
    }

    public String getField() {
        return field;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
