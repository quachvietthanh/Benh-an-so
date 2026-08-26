package com.benhsoan.adapter.inbound.rest.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.AuthRestMapper;
import com.benhsoan.adapter.inbound.rest.request.auth.LoginRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.PatientLoginRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.PatientRegistrationRequest;
import com.benhsoan.adapter.inbound.rest.request.auth.RefreshTokenRequest;
import com.benhsoan.adapter.inbound.rest.response.auth.LoginResponse;
import com.benhsoan.adapter.inbound.rest.response.auth.PatientLoginResponse;
import com.benhsoan.adapter.inbound.rest.response.auth.PatientRegistrationResponse;
import com.benhsoan.port.dto.result.LoginResult;
import com.benhsoan.port.dto.result.PatientLoginResult;
import com.benhsoan.port.dto.result.PatientPortalRegistrationResult;
import com.benhsoan.port.dto.command.auth.LogoutCommand;
import com.benhsoan.port.dto.command.auth.PatientLoginCommand;
import com.benhsoan.port.inbound.auth.LoginUseCase;
import com.benhsoan.port.inbound.auth.LogoutUseCase;
import com.benhsoan.port.inbound.auth.PatientLoginUseCase;
import com.benhsoan.port.inbound.auth.PatientPortalRegistrationUseCase;
import com.benhsoan.port.inbound.auth.RefreshTokenUseCase;
import com.benhsoan.domain.auth.exception.TokenInvalidException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final PatientLoginUseCase patientLoginUseCase;
    private final PatientPortalRegistrationUseCase patientPortalRegistrationUseCase;

    private final AuthRestMapper authRestMapper;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        LoginResult result =
                loginUseCase.login(
                        authRestMapper.toCommand(request));

        return ResponseEntity.ok(
                authRestMapper.toResponse(result));
    }

    @PostMapping("/patient/login")
    public ResponseEntity<PatientLoginResponse> patientLogin(
            @Valid @RequestBody PatientLoginRequest request,
            HttpServletRequest httpRequest
    ) {

        PatientLoginResult result =
                patientLoginUseCase.login(
                        new PatientLoginCommand(
                                request.phone(),
                                request.password(),
                                resolveIp(httpRequest),
                                httpRequest.getHeader("User-Agent")));

        return ResponseEntity.ok(
                authRestMapper.toResponse(result));
    }

    @PostMapping("/patient/register")
    public ResponseEntity<PatientRegistrationResponse> patientRegister(
            @Valid @RequestBody PatientRegistrationRequest request
    ) {

        PatientPortalRegistrationResult result =
                patientPortalRegistrationUseCase.register(
                        authRestMapper.toCommand(request));

        return ResponseEntity
                .status(org.springframework.http.HttpStatus.CREATED)
                .body(authRestMapper.toResponse(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {

        logoutUseCase.logout(
                new LogoutCommand(extractBearerToken(authorization)));

        return ResponseEntity.noContent().build();
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new TokenInvalidException();
        }

        String accessToken = authorization.substring(7);
        if (accessToken.isBlank()) {
            throw new TokenInvalidException();
        }

        return accessToken;
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {

        LoginResult result =
                refreshTokenUseCase.refreshToken(
                        authRestMapper.toCommand(request));

        return ResponseEntity.ok(
                authRestMapper.toResponse(result));
    }
}
