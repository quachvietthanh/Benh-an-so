package com.benhsoan.port.inbound.auth;

import com.benhsoan.port.dto.command.auth.LogoutCommand;


public interface LogoutUseCase {

    void logout(LogoutCommand command);

}