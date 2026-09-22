package com.benhsoan.adapter.inbound.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.AuthRestMapper;
import com.benhsoan.adapter.inbound.rest.request.auth.ConfigureTwoFactorAuthenticationRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.ResendTwoFactorRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.VerifyTwoFactorRequest;
import com.benhsoan.adapter.inbound.rest.response.auth.LoginResponse;
import com.benhsoan.adapter.inbound.rest.response.auth.TwoFactorConfigurationResponse;
import com.benhsoan.adapter.inbound.rest.response.auth.TwoFactorResendResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.command.auth.ConfigureTwoFactorAuthenticationCommand;
import com.benhsoan.port.inbound.auth.TwoFactorAuthenticationConfigurationUseCase;
import com.benhsoan.port.inbound.auth.TwoFactorResendUseCase;
import com.benhsoan.port.inbound.auth.TwoFactorVerificationUseCase;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Validated
public class TwoFactorAuthenticationController {

    private final TwoFactorVerificationUseCase twoFactorVerificationUseCase;
    private final TwoFactorResendUseCase twoFactorResendUseCase;
    private final TwoFactorAuthenticationConfigurationUseCase configurationUseCase;
    private final AuthRestMapper authRestMapper;

    @PostMapping("/auth/2fa/verify")
    public ResponseEntity<LoginResponse> verify(
            @Valid @RequestBody VerifyTwoFactorRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(
                authRestMapper.toResponse(
                        twoFactorVerificationUseCase.verify(
                                authRestMapper.toCommand(request, resolveIp(httpRequest)))));
    }

    @PostMapping("/auth/2fa/resend")
    public ResponseEntity<TwoFactorResendResponse> resend(
            @Valid @RequestBody ResendTwoFactorRequest request
    ) {
        return ResponseEntity.ok(
                authRestMapper.toResponse(
                        twoFactorResendUseCase.resend(authRestMapper.toCommand(request))));
    }

    @PatchMapping("/admin/two-factor-auth/roles/{roleName}")
    @RequirePermission("TWO_FACTOR_AUTH_MANAGE")
    public ResponseEntity<TwoFactorConfigurationResponse> configure(
            @PathVariable String roleName,
            @Valid @RequestBody ConfigureTwoFactorAuthenticationRequest request
    ) {
        return ResponseEntity.ok(
                authRestMapper.toResponse(
                        configurationUseCase.configure(
                                new ConfigureTwoFactorAuthenticationCommand(roleName, request.enabled()))));
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
