package com.benhsoan.domain.servicecatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.shared.exception.ValidationException;

class ServiceCatalogTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-02-01T00:00:00Z");

    @Test
    void createNormalizesTextAndActivatesService() {
        ServiceCatalog service = ServiceCatalog.create(
                UUID.randomUUID(),
                " LAB-CBC ",
                " Công thức   máu toàn bộ ",
                CREATED_AT
        );

        assertEquals("LAB-CBC", service.getServiceCode());
        assertEquals("Công thức máu toàn bộ", service.getServiceName());
        assertTrue(service.isActive());
    }

    @Test
    void renameAndActivationChangesRecordUpdateTime() {
        ServiceCatalog service = ServiceCatalog.create(
                UUID.randomUUID(),
                "LAB-CBC",
                "Công thức máu",
                CREATED_AT
        );

        service.rename("Công thức máu toàn bộ", UPDATED_AT);
        service.deactivate(UPDATED_AT);

        assertEquals("Công thức máu toàn bộ", service.getServiceName());
        assertFalse(service.isActive());
        assertEquals(UPDATED_AT, service.getUpdatedAt());
    }

    @Test
    void createRejectsBlankCodeOrName() {
        UUID id = UUID.randomUUID();

        assertThrows(
                ValidationException.class,
                () -> ServiceCatalog.create(id, " ", "Valid name", CREATED_AT)
        );
        assertThrows(
                ValidationException.class,
                () -> ServiceCatalog.create(id, "VALID", " ", CREATED_AT)
        );
    }
}
