package com.benhsoan.port.inbound.auth;

import com.benhsoan.port.dto.command.auth.ChangePasswordCommand;

public interface ChangePasswordUseCase {

    void changePassword(ChangePasswordCommand command);
}
