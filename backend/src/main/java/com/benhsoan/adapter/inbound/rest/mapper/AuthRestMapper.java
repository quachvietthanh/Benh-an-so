package com.benhsoan.adapter.inbound.rest.mapper;

import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.auth.LoginRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.PatientForgotPasswordRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.PatientLoginRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.PatientRegistrationRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.PatientResetPasswordRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.PatientVerifyRecoveryCodeRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.RefreshTokenRequest;
import com.benhsoan.adapter.inbound.rest.response.auth.LoginResponse;
import com.benhsoan.adapter.inbound.rest.response.auth.PatientLoginResponse;
import com.benhsoan.adapter.inbound.rest.response.auth.PatientRegistrationResponse;
import com.benhsoan.domain.patient.PatientAnonymizer;
import com.benhsoan.port.dto.command.auth.LoginCommand;
import com.benhsoan.port.dto.command.auth.PatientPortalRegistrationCommand;
import com.benhsoan.port.dto.command.auth.RefreshTokenCommand;
import com.benhsoan.port.dto.result.LoginResult;
import com.benhsoan.port.dto.result.PatientLoginResult;
import com.benhsoan.port.dto.result.PatientPortalRegistrationResult;

@Component
public class AuthRestMapper {

    private final AnonymizationModeState anonymizationModeState;

    public AuthRestMapper(
            AnonymizationModeState anonymizationModeState) {
        this.anonymizationModeState = anonymizationModeState;
    }

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
                result.expiredAt(),
                result.mustChangePassword()
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
                request.identityNumber(),
                request.email(),
                request.consentAgreed(),
                request.consentVersion()
        );
    }

    public PatientRegistrationResponse toResponse(PatientPortalRegistrationResult result) {

        return new PatientRegistrationResponse(
                result.userId(),
                result.patientId(),
                result.patientCode(),
                anonymizationModeState.isEnabled() ? PatientAnonymizer.maskPhone(result.phone()) : result.phone(),
                anonymizationModeState.isEnabled() ? PatientAnonymizer.maskFullName(result.patientCode()) : result.fullName(),
                result.accessToken(),
                result.refreshToken(),
                result.tokenType()
        );
    }

    public com.benhsoan.port.dto.command.auth.PatientForgotPasswordCommand toCommand(
            PatientForgotPasswordRequest request, String ipAddress, String userAgent) {
        return new com.benhsoan.port.dto.command.auth.PatientForgotPasswordCommand(
                request.phone(), ipAddress, userAgent);
    }

    public com.benhsoan.adapter.inbound.rest.response.auth.PatientForgotPasswordResponse toResponse(
            com.benhsoan.port.dto.result.PatientForgotPasswordResult result) {
        return new com.benhsoan.adapter.inbound.rest.response.auth.PatientForgotPasswordResponse(
                result.message(), result.expiresInSeconds());
    }

    public com.benhsoan.port.dto.command.auth.PatientVerifyRecoveryCodeCommand toCommand(
            PatientVerifyRecoveryCodeRequest request) {
        return new com.benhsoan.port.dto.command.auth.PatientVerifyRecoveryCodeCommand(
                request.phone(), request.code());
    }

    public com.benhsoan.port.dto.command.auth.PatientResetPasswordCommand toCommand(
            PatientResetPasswordRequest request, String ipAddress, String userAgent) {
        return new com.benhsoan.port.dto.command.auth.PatientResetPasswordCommand(
                request.phone(), request.code(), request.newPassword(), ipAddress, userAgent);
    }

    public com.benhsoan.adapter.inbound.rest.response.auth.PatientResetPasswordResponse toResponse(
            com.benhsoan.port.dto.result.PatientResetPasswordResult result) {
        return new com.benhsoan.adapter.inbound.rest.response.auth.PatientResetPasswordResponse(
                result.message());
    }
}
