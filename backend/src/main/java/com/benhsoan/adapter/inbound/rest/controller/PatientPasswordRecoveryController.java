package com.benhsoan.adapter.inbound.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.AuthRestMapper;
import com.benhsoan.adapter.inbound.rest.request.auth.PatientForgotPasswordRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.PatientResetPasswordRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.PatientVerifyRecoveryCodeRequest;
import com.benhsoan.adapter.inbound.rest.response.auth.PatientForgotPasswordResponse;
import com.benhsoan.adapter.inbound.rest.response.auth.PatientResetPasswordResponse;
import com.benhsoan.adapter.inbound.rest.response.auth.PatientVerifyRecoveryCodeResponse;
import com.benhsoan.port.inbound.auth.PatientForgotPasswordUseCase;
import com.benhsoan.port.inbound.auth.PatientResetPasswordUseCase;
import com.benhsoan.port.inbound.auth.PatientVerifyRecoveryCodeUseCase;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller handling patient portal password recovery endpoints (NCL-14-CN-006 / QTN-28).
 * Separated from AuthController to maintain Single Responsibility Principle (SRP) and
 * preserve backward-compatibility of existing AuthController test suites.
 */
@RestController
@RequestMapping("/auth/patient")
@RequiredArgsConstructor
@Validated
public class PatientPasswordRecoveryController {

    private final PatientForgotPasswordUseCase patientForgotPasswordUseCase;
    private final PatientVerifyRecoveryCodeUseCase patientVerifyRecoveryCodeUseCase;
    private final PatientResetPasswordUseCase patientResetPasswordUseCase;
    private final AuthRestMapper authRestMapper;

    @PostMapping("/forgot-password")
    public ResponseEntity<PatientForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody PatientForgotPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        var result = patientForgotPasswordUseCase.forgotPassword(
                authRestMapper.toCommand(request, resolveIp(httpRequest), httpRequest.getHeader("User-Agent")));
        return ResponseEntity.ok(authRestMapper.toResponse(result));
    }

    @PostMapping("/verify-recovery-code")
    public ResponseEntity<PatientVerifyRecoveryCodeResponse> verifyRecoveryCode(
            @Valid @RequestBody PatientVerifyRecoveryCodeRequest request
    ) {
        patientVerifyRecoveryCodeUseCase.verifyCode(authRestMapper.toCommand(request));
        return ResponseEntity.ok(new PatientVerifyRecoveryCodeResponse(true, "Mã xác thực hợp lệ."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<PatientResetPasswordResponse> resetPassword(
            @Valid @RequestBody PatientResetPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        var result = patientResetPasswordUseCase.resetPassword(
                authRestMapper.toCommand(request, resolveIp(httpRequest), httpRequest.getHeader("User-Agent")));
        return ResponseEntity.ok(authRestMapper.toResponse(result));
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
