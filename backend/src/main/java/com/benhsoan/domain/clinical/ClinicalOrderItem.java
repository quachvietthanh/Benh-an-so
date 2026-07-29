package com.benhsoan.domain.clinical;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.domain.clinical.exception.ClinicalOrderItemInvalidStatusException;
import com.benhsoan.domain.shared.Guard.Guard;

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

    private ClinicalOrderItem(UUID id, UUID orderId, UUID serviceId, String code, String name, String instruction, ClinicalOrderItemStatus status, Instant created, Instant updated) {
        this.id = Objects.requireNonNull(id);
        clinicalOrderId = Objects.requireNonNull(orderId);
        clinicalServiceId = Objects.requireNonNull(serviceId);
        serviceCode = Guard.require(code, "Service code");
        serviceName = Guard.require(name, "Service name");
        this.instruction = instruction;
        this.status = Objects.requireNonNull(status);
        createdAt = Objects.requireNonNull(created);
        updatedAt = updated;
    }

    public static ClinicalOrderItem create(UUID orderId, UUID serviceId, String code, String name, String instruction) {
        return new ClinicalOrderItem(UUID.randomUUID(), orderId, serviceId, code, name, instruction, ClinicalOrderItemStatus.PENDING, Instant.now(), null);
    }

    public static ClinicalOrderItem restore(UUID id, UUID orderId, UUID serviceId, String code, String name, String instruction, ClinicalOrderItemStatus status, Instant created, Instant updated) {
        return new ClinicalOrderItem(id, orderId, serviceId, code, name, instruction, status, created, updated);
    }

    public void complete(Instant at) {
        if (status != ClinicalOrderItemStatus.PENDING) {
            throw new ClinicalOrderItemInvalidStatusException("Only pending items can be completed.");
        
        }status = ClinicalOrderItemStatus.COMPLETED;
        updatedAt = Objects.requireNonNull(at);
    }

    public void cancel(Instant at) {
        if (status != ClinicalOrderItemStatus.PENDING) {
            throw new ClinicalOrderItemInvalidStatusException("Only pending items can be cancelled.");
        
        }status = ClinicalOrderItemStatus.CANCELLED;
        updatedAt = Objects.requireNonNull(at);
    }
}
