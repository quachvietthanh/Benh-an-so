package com.benhsoan.domain.servicecatalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
public class ServicePrice {

    private UUID id;
    private UUID serviceCatalogId;
    private BigDecimal price;
    private LocalDate effectiveFrom;
    private Instant createdAt;
    private UUID createdBy;

    private ServicePrice(
            UUID id,
            UUID serviceCatalogId,
            BigDecimal price,
            LocalDate effectiveFrom,
            Instant createdAt,
            UUID createdBy
    ) {
        this.id = requireNonNull(id, "Service price id is required.");
        this.serviceCatalogId = requireNonNull(serviceCatalogId, "Service catalog id is required.");
        this.price = requireNonNegative(price);
        this.effectiveFrom = requireNonNull(effectiveFrom, "Price effective date is required.");
        this.createdAt = requireNonNull(createdAt, "Service price creation time is required.");
        this.createdBy = requireNonNull(createdBy, "Service price creator is required.");
    }

    public static ServicePrice create(
            UUID id,
            UUID serviceCatalogId,
            BigDecimal price,
            LocalDate effectiveFrom,
            Instant createdAt,
            UUID createdBy
    ) {
        return new ServicePrice(id, serviceCatalogId, price, effectiveFrom, createdAt, createdBy);
    }

    public static ServicePrice restore(
            UUID id,
            UUID serviceCatalogId,
            BigDecimal price,
            LocalDate effectiveFrom,
            Instant createdAt,
            UUID createdBy
    ) {
        return new ServicePrice(id, serviceCatalogId, price, effectiveFrom, createdAt, createdBy);
    }

    private static BigDecimal requireNonNegative(BigDecimal value) {
        BigDecimal validated = requireNonNull(value, "Service price is required.");
        if (validated.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Service price must not be negative.");
        }
        return validated;
    }

    private static <T> T requireNonNull(T value, String message) {
        if (Objects.isNull(value)) {
            throw new ValidationException(message);
        }
        return value;
    }
}
