package com.benhsoan.port.inbound.auth;

import com.benhsoan.port.dto.command.auth.LoginCommand;
import com.benhsoan.port.dto.result.LoginResult;


public interface LoginUseCase {

    LoginResult login(LoginCommand command);

}