package com.benhsoan.domain.prescription.exception;

import java.util.List;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.port.dto.result.MaxDailyDoseWarningResult;

public class PrescriptionMaxDailyDoseConfirmationRequiredException extends PrescriptionException {

    private final List<MaxDailyDoseWarningResult> warnings;

    public PrescriptionMaxDailyDoseConfirmationRequiredException(List<MaxDailyDoseWarningResult> warnings) {
        super(
                DomainErrorCode.MAX_DAILY_DOSE_CONFIRMATION_REQUIRED,
                "Tổng liều hoạt chất trong ngày vượt ngưỡng tối đa; cần xác nhận và ghi lý do."
        );
        this.warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public List<MaxDailyDoseWarningResult> getWarnings() {
        return warnings;
    }
}
