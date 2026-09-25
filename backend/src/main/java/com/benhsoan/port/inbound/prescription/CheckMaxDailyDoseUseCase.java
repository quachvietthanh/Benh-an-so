package com.benhsoan.port.inbound.prescription;

import com.benhsoan.port.dto.command.prescription.CheckMaxDailyDoseCommand;
import com.benhsoan.port.dto.result.MaxDailyDoseCheckResult;

public interface CheckMaxDailyDoseUseCase {

    MaxDailyDoseCheckResult check(CheckMaxDailyDoseCommand command);
}
