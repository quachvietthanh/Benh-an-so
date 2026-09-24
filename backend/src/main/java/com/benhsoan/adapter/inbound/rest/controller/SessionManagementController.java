package com.benhsoan.adapter.inbound.rest.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.SessionRestMapper;
import com.benhsoan.adapter.inbound.rest.response.auth.ActiveSessionResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.inbound.auth.GetActiveSessionsUseCase;
import com.benhsoan.port.inbound.auth.TerminateSessionUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/sessions")
@RequiredArgsConstructor
@Validated
public class SessionManagementController {

    private final GetActiveSessionsUseCase getActiveSessionsUseCase;
    private final TerminateSessionUseCase terminateSessionUseCase;
    private final SessionRestMapper sessionRestMapper;

    @GetMapping
    @RequirePermission("SESSION_READ")
    public ResponseEntity<Page<ActiveSessionResponse>> getActiveSessions(Pageable pageable) {
        Page<ActiveSessionResponse> responsePage = getActiveSessionsUseCase
                .getActiveSessions(pageable)
                .map(sessionRestMapper::toResponse);

        return ResponseEntity.ok(responsePage);
    }

    @PostMapping("/{id}/terminate")
    @RequirePermission("SESSION_TERMINATE")
    public ResponseEntity<Void> terminateSession(@PathVariable("id") UUID id) {
        terminateSessionUseCase.terminateSession(sessionRestMapper.toTerminateCommand(id));
        return ResponseEntity.noContent().build();
    }
}
