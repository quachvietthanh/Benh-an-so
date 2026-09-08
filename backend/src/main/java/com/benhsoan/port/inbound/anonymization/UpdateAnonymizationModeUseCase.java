package com.benhsoan.port.inbound.anonymization;

import com.benhsoan.port.dto.command.anonymization.UpdateAnonymizationModeCommand;
import com.benhsoan.port.dto.result.anonymization.AnonymizationModeResult;

public interface UpdateAnonymizationModeUseCase {

    AnonymizationModeResult update(UpdateAnonymizationModeCommand command);
}
