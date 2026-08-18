package com.benhsoan.domain.clinical;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.clinical.enums.ClinicalResultDataType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;
import com.benhsoan.domain.shared.Guard.Guard;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClinicalServiceCatalog {

    private UUID id;
    private UUID serviceCatalogId;
    private String serviceCode, serviceName, unit, referenceRange, description;
    private ClinicalServiceType serviceType;
    private ClinicalResultDataType resultDataType;
    private boolean active;
    private Instant createdAt, updatedAt;

    private ClinicalServiceCatalog(UUID id, UUID serviceCatalogId, String code, String name, ClinicalServiceType type, ClinicalResultDataType dataType, String unit, String range, String description, boolean active, Instant created, Instant updated) {
        this.id = Objects.requireNonNull(id);
        this.serviceCatalogId = Objects.requireNonNull(serviceCatalogId);
        serviceCode = Guard.require(code, "Service code");
        serviceName = Guard.require(name, "Service name");
        serviceType = Objects.requireNonNull(type);
        resultDataType = Objects.requireNonNull(dataType);
        this.unit = unit;
        referenceRange = range;
        this.description = description;
        this.active = active;
        createdAt = Objects.requireNonNull(created);
        updatedAt = updated;
    }

    public static ClinicalServiceCatalog create(UUID serviceCatalogId, String code, String name, ClinicalServiceType type,
            ClinicalResultDataType dataType, String unit, String range, String description, Instant createdAt) {
        return new ClinicalServiceCatalog(UUID.randomUUID(), serviceCatalogId, code, name, type, dataType, unit, range,
                description, true, Objects.requireNonNull(createdAt), null);
    }

    public static ClinicalServiceCatalog restore(UUID id, UUID serviceCatalogId, String code, String name, ClinicalServiceType type, ClinicalResultDataType dataType, String unit, String range, String description, boolean active, Instant created, Instant updated) {
        return new ClinicalServiceCatalog(id, serviceCatalogId, code, name, type, dataType, unit, range, description, active, created, updated);
    }

    public void activate(Instant at) {
        active = true;
        updatedAt = Objects.requireNonNull(at);
    }

    public void deactivate(Instant at) {
        active = false;
        updatedAt = Objects.requireNonNull(at);
    }

    public void updateInformation(String name, ClinicalServiceType type, ClinicalResultDataType dataType, String unit, String range, String description, Instant at) {
        serviceName = Guard.require(name, "Service name");
        serviceType = Objects.requireNonNull(type);
        resultDataType = Objects.requireNonNull(dataType);
        this.unit = unit;
        referenceRange = range;
        this.description = description;
        updatedAt = Objects.requireNonNull(at);
    }
}
