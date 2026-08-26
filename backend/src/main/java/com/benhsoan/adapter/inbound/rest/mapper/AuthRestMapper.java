package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.auth.LoginRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.PatientRegistrationRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.RefreshTokenRequest;
import com.benhsoan.adapter.inbound.rest.response.auth.LoginResponse;
import com.benhsoan.adapter.inbound.rest.response.auth.PatientLoginResponse;
import com.benhsoan.adapter.inbound.rest.response.auth.PatientRegistrationResponse;
import com.benhsoan.port.dto.command.auth.LoginCommand;
import com.benhsoan.port.dto.command.auth.PatientPortalRegistrationCommand;
import com.benhsoan.port.dto.command.auth.RefreshTokenCommand;
import com.benhsoan.port.dto.result.LoginResult;
import com.benhsoan.port.dto.result.PatientLoginResult;
import com.benhsoan.port.dto.result.PatientPortalRegistrationResult;

@Component
public class AuthRestMapper {

    public LoginCommand toCommand(LoginRequest request) {

        return new LoginCommand(
                request.username(),
                request.password()
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

    public PatientLoginResponse toResponse(PatientLoginResult result) {

        return new PatientLoginResponse(
                result.userId(),
                result.username(),
                result.accessToken(),
                result.refreshToken(),
                result.role(),
                result.expiredAt(),
                result.patientId()
        );
    }

    public PatientPortalRegistrationCommand toCommand(PatientRegistrationRequest request) {

        return new PatientPortalRegistrationCommand(
                request.phone(),
                request.password(),
                request.fullName(),
                request.dateOfBirth(),
                request.gender(),
                request.identityNumber()
        );
    }

    public PatientRegistrationResponse toResponse(PatientPortalRegistrationResult result) {

        return new PatientRegistrationResponse(
                result.userId(),
                result.patientId(),
                result.phone(),
                result.fullName()
        );
    }
}
