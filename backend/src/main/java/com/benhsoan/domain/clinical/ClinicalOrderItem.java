package com.benhsoan.domain.clinical;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.domain.clinical.exception.ClinicalOrderItemInvalidStatusException;
import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClinicalOrderItem {

    private UUID id, clinicalOrderId, clinicalServiceId;
    private String serviceCode, serviceName, instruction;
    private ClinicalOrderItemStatus status;
    private Instant createdAt, updatedAt;
    private String cancelReason;
    private UUID cancelledBy;
    private Instant cancelledAt;

    private ClinicalOrderItem(UUID id, UUID orderId, UUID serviceId, String code, String name, String instruction, ClinicalOrderItemStatus status, Instant created, Instant updated, String cancelReason, UUID cancelledBy, Instant cancelledAt) {
        this.id = Objects.requireNonNull(id);
        clinicalOrderId = Objects.requireNonNull(orderId);
        clinicalServiceId = Objects.requireNonNull(serviceId);
        serviceCode = Guard.require(code, "Service code");
        serviceName = Guard.require(name, "Service name");
        this.instruction = instruction;
        this.status = Objects.requireNonNull(status);
        createdAt = Objects.requireNonNull(created);
        updatedAt = updated;
        this.cancelReason = cancelReason;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = cancelledAt;
    }

    public static ClinicalOrderItem create(UUID orderId, UUID serviceId, String code, String name,
            String instruction, Instant createdAt) {
        return new ClinicalOrderItem(UUID.randomUUID(), orderId, serviceId, code, name, instruction,
                ClinicalOrderItemStatus.PENDING, Objects.requireNonNull(createdAt), null, null, null, null);
    }

    public static ClinicalOrderItem restore(UUID id, UUID orderId, UUID serviceId, String code, String name, String instruction, ClinicalOrderItemStatus status, Instant created, Instant updated) {
        return restore(id, orderId, serviceId, code, name, instruction, status, created, updated, null, null, null);
    }

    public static ClinicalOrderItem restore(UUID id, UUID orderId, UUID serviceId, String code, String name, String instruction, ClinicalOrderItemStatus status, Instant created, Instant updated, String cancelReason, UUID cancelledBy, Instant cancelledAt) {
        return new ClinicalOrderItem(id, orderId, serviceId, code, name, instruction, status, created, updated, cancelReason, cancelledBy, cancelledAt);
    }

    public void complete(Instant at) {
        if (status != ClinicalOrderItemStatus.PENDING) {
            throw new ClinicalOrderItemInvalidStatusException("Only pending items can be completed.");
        }
        status = ClinicalOrderItemStatus.COMPLETED;
        updatedAt = Objects.requireNonNull(at);
    }

    public void cancel(String reason, UUID by, Instant at) {
        if (status == ClinicalOrderItemStatus.CANCELLED) {
            throw new ClinicalOrderItemInvalidStatusException("Clinical order item is already cancelled.");
        }
        if (status != ClinicalOrderItemStatus.PENDING) {
            throw new ClinicalOrderItemInvalidStatusException("Only pending items can be cancelled.");
        }
        String validatedReason = Guard.require(reason, "Cancellation reason");
        if (validatedReason.length() > 500) {
            throw new ValidationException("Cancellation reason must not exceed 500 characters.");
        }
        this.cancelReason = validatedReason;
        this.cancelledBy = by;
        this.cancelledAt = Objects.requireNonNull(at, "Cancellation time is required.");
        status = ClinicalOrderItemStatus.CANCELLED;
        updatedAt = Objects.requireNonNull(at);
    }

    public void cancel(Instant at) {
        cancel("Cancelled without reason", null, at);
    }
}
