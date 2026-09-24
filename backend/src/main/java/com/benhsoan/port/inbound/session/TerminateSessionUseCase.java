package com.benhsoan.port.inbound.session;

import com.benhsoan.port.dto.command.session.TerminateSessionCommand;

public interface TerminateSessionUseCase {

    void terminate(TerminateSessionCommand command);
}