package com.benhsoan.domain.servicecatalog;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceCatalog {

    private UUID id;
    private String serviceCode;
    private String serviceName;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    private ServiceCatalog(
            UUID id,
            String serviceCode,
            String serviceName,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = requireNonNull(id, "Service catalog id is required.");
        this.serviceCode = requireText(serviceCode, "Service code is required.");
        this.serviceName = requireText(serviceName, "Service name is required.");
        this.active = active;
        this.createdAt = requireNonNull(createdAt, "Service creation time is required.");
        this.updatedAt = updatedAt;
    }

    public static ServiceCatalog create(
            UUID id,
            String serviceCode,
            String serviceName,
            Instant createdAt
    ) {
        return new ServiceCatalog(id, serviceCode, serviceName, true, createdAt, null);
    }

    public static ServiceCatalog restore(
            UUID id,
            String serviceCode,
            String serviceName,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new ServiceCatalog(id, serviceCode, serviceName, active, createdAt, updatedAt);
    }

    public void rename(String serviceName, Instant updatedAt) {
        String validatedName = requireText(serviceName, "Service name is required.");
        Instant validatedUpdatedAt = requireNonNull(updatedAt, "Service update time is required.");
        this.serviceName = validatedName;
        this.updatedAt = validatedUpdatedAt;
    }

    public void activate(Instant updatedAt) {
        Instant validatedUpdatedAt = requireNonNull(updatedAt, "Service update time is required.");
        this.active = true;
        this.updatedAt = validatedUpdatedAt;
    }

    public void deactivate(Instant updatedAt) {
        Instant validatedUpdatedAt = requireNonNull(updatedAt, "Service update time is required.");
        this.active = false;
        this.updatedAt = validatedUpdatedAt;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private static <T> T requireNonNull(T value, String message) {
        if (Objects.isNull(value)) {
            throw new ValidationException(message);
        }
        return value;
    }
}
