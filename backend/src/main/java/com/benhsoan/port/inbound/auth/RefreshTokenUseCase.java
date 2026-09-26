package com.benhsoan.port.inbound.auth;

import com.benhsoan.port.dto.command.auth.RefreshTokenCommand;
import com.benhsoan.port.dto.result.LoginResult;

public interface RefreshTokenUseCase {

    LoginResult refreshToken( RefreshTokenCommand command);
}