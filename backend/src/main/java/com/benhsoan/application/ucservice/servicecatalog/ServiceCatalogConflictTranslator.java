package com.benhsoan.application.ucservice.servicecatalog;

import org.springframework.dao.DataIntegrityViolationException;

import com.benhsoan.domain.shared.exception.ValidationException;

final class ServiceCatalogConflictTranslator {

    private ServiceCatalogConflictTranslator() {
    }

    static ValidationException translate(DataIntegrityViolationException exception) {
        String message = extractMessage(exception).toLowerCase();
        if (message.contains("uk_service_catalog_code")
                || message.contains("service_code")) {
            return new ValidationException("Service code already exists.");
        }
        if (message.contains("uk_service_price_effective_from")
                || message.contains("service_catalog_id") && message.contains("effective_from")) {
            return new ValidationException("A service price already exists for this effective date.");
        }
        return new ValidationException("Service catalog data conflicts with an existing record.");
    }

    private static String extractMessage(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null) {
                builder.append(current.getMessage()).append(' ');
            }
            current = current.getCause();
        }
        return builder.toString();
    }
}
