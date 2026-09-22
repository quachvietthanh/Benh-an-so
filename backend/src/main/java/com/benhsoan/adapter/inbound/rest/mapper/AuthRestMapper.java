package com.benhsoan.adapter.inbound.rest.mapper;

import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.auth.LoginRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.PatientForgotPasswordRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.PatientRegistrationRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.PatientResetPasswordRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.PatientVerifyRecoveryCodeRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.RefreshTokenRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.ResendTwoFactorRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.VerifyTwoFactorRequest;
import com.benhsoan.adapter.inbound.rest.response.auth.LoginResponse;
import com.benhsoan.adapter.inbound.rest.response.auth.PatientLoginResponse;
import com.benhsoan.adapter.inbound.rest.response.auth.PatientRegistrationResponse;
import com.benhsoan.adapter.inbound.rest.response.auth.TwoFactorConfigurationResponse;
import com.benhsoan.adapter.inbound.rest.response.auth.TwoFactorResendResponse;
import com.benhsoan.domain.patient.PatientAnonymizer;
import com.benhsoan.port.dto.command.auth.LoginCommand;
import com.benhsoan.port.dto.command.auth.PatientPortalRegistrationCommand;
import com.benhsoan.port.dto.command.auth.RefreshTokenCommand;
import com.benhsoan.port.dto.command.auth.ResendTwoFactorCommand;
import com.benhsoan.port.dto.command.auth.VerifyTwoFactorCommand;
import com.benhsoan.port.dto.result.LoginResult;
import com.benhsoan.port.dto.result.PatientLoginResult;
import com.benhsoan.port.dto.result.PatientPortalRegistrationResult;
import com.benhsoan.port.dto.result.TwoFactorConfigurationResult;
import com.benhsoan.port.dto.result.TwoFactorResendResult;

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
                                request.password());
        }

        public RefreshTokenCommand toCommand(RefreshTokenRequest request) {

                return new RefreshTokenCommand(
                                request.refreshToken());
        }

        public VerifyTwoFactorCommand toCommand(VerifyTwoFactorRequest request) {

                return new VerifyTwoFactorCommand(
                                request.twoFactorToken(),
                                request.code());
        }

        public ResendTwoFactorCommand toCommand(ResendTwoFactorRequest request) {

                return new ResendTwoFactorCommand(
                                request.twoFactorToken());
        }

        public LoginResponse toResponse(LoginResult result) {

                return new LoginResponse(
                                result.userId(),
                                result.username(),
                                result.accessToken(),
                                result.refreshToken(),
                                result.role(),
                                result.expiredAt(),
                                result.mustChangePassword(),
                                result.twoFactorRequired(),
                                result.twoFactorToken(),
                                result.twoFactorExpiresAt());
        }

        public TwoFactorResendResponse toResponse(TwoFactorResendResult result) {

                return new TwoFactorResendResponse(
                                result.twoFactorToken(),
                                result.expiresAt());
        }

        public TwoFactorConfigurationResponse toResponse(TwoFactorConfigurationResult result) {

                return new TwoFactorConfigurationResponse(
                                result.roleName(),
                                result.twoFactorRequired());
        }

        public PatientLoginResponse toResponse(PatientLoginResult result) {

                return new PatientLoginResponse(
                                result.userId(),
                                result.username(),
                                result.accessToken(),
                                result.refreshToken(),
                                result.role(),
                                result.expiredAt(),
                                result.patientId());
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
                                request.consentVersion());
        }

        public PatientRegistrationResponse toResponse(PatientPortalRegistrationResult result) {

                return new PatientRegistrationResponse(
                                result.userId(),
                                result.patientId(),
                                result.patientCode(),
                                anonymizationModeState.isEnabled() ? PatientAnonymizer.maskPhone(result.phone())
                                                : result.phone(),
                                anonymizationModeState.isEnabled()
                                                ? PatientAnonymizer.maskFullName(result.patientCode())
                                                : result.fullName(),
                                result.accessToken(),
                                result.refreshToken(),
                                result.tokenType());
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
