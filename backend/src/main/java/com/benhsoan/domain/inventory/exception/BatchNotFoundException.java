package com.benhsoan.domain.inventory.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

public class BatchNotFoundException extends DomainException {

    public BatchNotFoundException(UUID batchId) {
        super(DomainErrorCode.BATCH_NOT_FOUND, "Không tìm thấy lô thuốc với mã: " + batchId);
    }
}
