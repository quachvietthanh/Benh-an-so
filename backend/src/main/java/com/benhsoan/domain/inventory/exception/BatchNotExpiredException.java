package com.benhsoan.domain.inventory.exception;

import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

public class BatchNotExpiredException extends DomainException {

    public BatchNotExpiredException(UUID batchId, LocalDate expiryDate) {
        super(DomainErrorCode.BATCH_NOT_EXPIRED,
                "Không thể hủy lô thuốc chưa hết hạn sử dụng. Hạn dùng của lô " + batchId + " là " + expiryDate);
    }
}
