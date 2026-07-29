package com.benhsoan.domain.clinical;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.clinical.enums.ClinicalOrderStatus;
import com.benhsoan.domain.clinical.exception.ClinicalOrderAlreadyCancelledException;
import com.benhsoan.domain.clinical.exception.ClinicalOrderAlreadyCompletedException;
import com.benhsoan.domain.clinical.exception.ClinicalOrderInvalidStatusException;
import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClinicalOrder {

    private UUID id, visitId, medicalRecordId, patientId, orderedBy;
    private String orderCode, clinicalReason;
    private ClinicalOrderStatus status;
    private Instant orderedAt, completedAt, createdAt, updatedAt;

    private ClinicalOrder(UUID id, String code, UUID visit, UUID record, UUID patient, UUID by, String reason, ClinicalOrderStatus status, Instant ordered, Instant completed, Instant created, Instant updated) {
        this.id = Objects.requireNonNull(id);
        orderCode = Guard.require(code, "Order code");
        visitId = Objects.requireNonNull(visit);
        medicalRecordId = Objects.requireNonNull(record);
        patientId = Objects.requireNonNull(patient);
        orderedBy = Objects.requireNonNull(by);
        clinicalReason = reason;
        this.status = Objects.requireNonNull(status);
        orderedAt = Objects.requireNonNull(ordered);
        completedAt = completed;
        createdAt = Objects.requireNonNull(created);
        updatedAt = updated;
    }

    public static ClinicalOrder create(String code, UUID visit, UUID record, UUID patient, UUID by, String reason, Instant at) {
        return new ClinicalOrder(UUID.randomUUID(), code, visit, record, patient, by, reason, ClinicalOrderStatus.ORDERED, at, null, at, null);
    }

    public static ClinicalOrder restore(UUID id, String code, UUID visit, UUID record, UUID patient, UUID by, String reason, ClinicalOrderStatus status, Instant ordered, Instant completed, Instant created, Instant updated) {
        return new ClinicalOrder(id, code, visit, record, patient, by, reason, status, ordered, completed, created, updated);
    }

    public void start(Instant at) {
        require(ClinicalOrderStatus.ORDERED);
        status = ClinicalOrderStatus.IN_PROGRESS;
        updatedAt = Objects.requireNonNull(at);
    }

    public void markPartiallyCompleted(Instant at) {
        require(ClinicalOrderStatus.IN_PROGRESS);
        status = ClinicalOrderStatus.PARTIALLY_COMPLETED;
        updatedAt = Objects.requireNonNull(at);
    }

    public void complete(Instant at) {
        if (status != ClinicalOrderStatus.IN_PROGRESS && status != ClinicalOrderStatus.PARTIALLY_COMPLETED) {
            conflict("Only active orders can be completed.");
        
        }if (at.isBefore(orderedAt)) {
            throw new ValidationException("Completion time is invalid.");
        
        }status = ClinicalOrderStatus.COMPLETED;
        completedAt = at;
        updatedAt = at;
    }

    public void cancel(Instant at) {
        if (status == ClinicalOrderStatus.COMPLETED) {
            throw new ClinicalOrderAlreadyCompletedException();
        
        }if (status == ClinicalOrderStatus.CANCELLED) {
            throw new ClinicalOrderAlreadyCancelledException();
        
        }status = ClinicalOrderStatus.CANCELLED;
        updatedAt = Objects.requireNonNull(at);
    }

    public boolean isCompleted() {
        return status == ClinicalOrderStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return status == ClinicalOrderStatus.CANCELLED;
    }

    private void require(ClinicalOrderStatus expected) {
        if (status != expected) {
            conflict("Invalid clinical order status transition.");
    
        }}

    private void conflict(String m) {
        if (status == ClinicalOrderStatus.COMPLETED) {
            throw new ClinicalOrderAlreadyCompletedException();
        
        }if (status == ClinicalOrderStatus.CANCELLED) {
            throw new ClinicalOrderAlreadyCancelledException();
        
        }throw new ClinicalOrderInvalidStatusException(m);
    }
}
