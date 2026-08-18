package com.benhsoan.domain.clinical;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.clinical.enums.ClinicalResultDataType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;

class ClinicalServiceCatalogTest {

    @Test
    void updatesAndReactivatesService() {
        ClinicalServiceCatalog service = ClinicalServiceCatalog.create(
                UUID.randomUUID(), "LAB-001", "Blood glucose", ClinicalServiceType.LAB_TEST,
                ClinicalResultDataType.NUMBER, "mmol/L", "3.9-5.5", null,
                Instant.parse("2026-08-20T01:00:00Z")
        );
        Instant updatedAt = Instant.parse("2026-08-20T02:00:00Z");

        service.deactivate(updatedAt);
        assertFalse(service.isActive());

        service.updateInformation(
                "Fasting blood glucose", ClinicalServiceType.LAB_TEST,
                ClinicalResultDataType.NUMBER, "mmol/L", "3.9-5.5", "Fasting", updatedAt
        );
        service.activate(updatedAt.plusSeconds(1));

        assertTrue(service.isActive());
    }
}
