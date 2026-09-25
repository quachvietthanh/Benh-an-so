package com.benhsoan.domain.prescription.exception;

import java.util.List;

import com.benhsoan.domain.prescription.MaxDailyDoseWarning;
import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PrescriptionMaxDailyDoseConfirmationRequiredException extends PrescriptionException {

    private final List<MaxDailyDoseWarning> warnings;

    public PrescriptionMaxDailyDoseConfirmationRequiredException(List<MaxDailyDoseWarning> warnings) {
        super(
                DomainErrorCode.MAX_DAILY_DOSE_CONFIRMATION_REQUIRED,
                "Tổng liều hoạt chất trong ngày vượt ngưỡng tối đa; cần xác nhận và ghi lý do."
        );
        this.warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public List<MaxDailyDoseWarning> getWarnings() {
        return warnings;
    }
}
