package com.benhsoan.port.inbound.vitalsign;

import com.benhsoan.port.dto.command.vitalsign.RecordVitalSignCommand;
import com.benhsoan.port.dto.result.vitalsign.VitalSignResult;

public interface RecordVitalSignUseCase {

    VitalSignResult record(RecordVitalSignCommand command);
}
