package com.benhsoan.domain.inventory.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

public class BatchStateConflictException extends DomainException {

    public BatchStateConflictException(UUID batchId, String message) {
        super(DomainErrorCode.BATCH_STATE_CONFLICT, message + " (Mã lô: " + batchId + ")");
    }
}
