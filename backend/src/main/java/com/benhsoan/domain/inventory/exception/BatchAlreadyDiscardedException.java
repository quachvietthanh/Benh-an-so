package com.benhsoan.domain.inventory.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

public class BatchAlreadyDiscardedException extends DomainException {

    public BatchAlreadyDiscardedException(UUID batchId) {
        super(DomainErrorCode.BATCH_ALREADY_DISCARDED,
                "Lô thuốc đã bị hủy hoặc không còn tồn kho để hủy: " + batchId);
    }
}
