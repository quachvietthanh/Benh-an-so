package com.benhsoan.port.inbound.vitalsign;

import java.util.UUID;

import com.benhsoan.port.dto.command.vitalsign.UpdateVitalSignCommand;
import com.benhsoan.port.dto.result.vitalsign.VitalSignResult;

public interface UpdateVitalSignUseCase {

    VitalSignResult update(UUID id, UpdateVitalSignCommand command);
}
