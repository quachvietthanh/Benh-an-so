package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.auth.LoginRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.RefreshTokenRequest;
import com.benhsoan.adapter.inbound.rest.response.auth.LoginResponse;
import com.benhsoan.port.dto.command.auth.LoginCommand;
import com.benhsoan.port.dto.command.auth.RefreshTokenCommand;
import com.benhsoan.port.dto.result.LoginResult;

@Component
public class AuthRestMapper {

    public LoginCommand toCommand(LoginRequest request) {

        return new LoginCommand(
                request.username(),
                request.password(),
                request.ipAddress()
        );
    }

    public RefreshTokenCommand toCommand(RefreshTokenRequest request) {

        return new RefreshTokenCommand(
                request.refreshToken()
        );
    }

    public LoginResponse toResponse(LoginResult result) {

        return new LoginResponse(
                result.userId(),
                result.username(),
                result.accessToken(),
                result.refreshToken(),
                result.role(),
                result.expiredAt()
        );
    }
}
