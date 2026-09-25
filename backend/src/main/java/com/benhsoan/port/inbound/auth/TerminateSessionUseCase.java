package com.benhsoan.port.inbound.auth;

import com.benhsoan.port.dto.command.auth.TerminateSessionCommand;

public interface TerminateSessionUseCase {

    void terminateSession(TerminateSessionCommand command);
}
