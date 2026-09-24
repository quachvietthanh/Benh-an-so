package com.benhsoan.adapter.inbound.rest.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.SessionRestMapper;
import com.benhsoan.adapter.inbound.rest.response.session.SessionStatusResponse;
import com.benhsoan.adapter.inbound.rest.response.session.SessionSummaryResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.command.session.TerminateSessionCommand;
import com.benhsoan.port.inbound.session.ExtendSessionUseCase;
import com.benhsoan.port.inbound.session.GetCurrentSessionUseCase;
import com.benhsoan.port.inbound.session.ListSessionsUseCase;
import com.benhsoan.port.inbound.session.TerminateSessionUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final GetCurrentSessionUseCase getCurrentSessionUseCase;
    private final ExtendSessionUseCase extendSessionUseCase;
    private final ListSessionsUseCase listSessionsUseCase;
    private final TerminateSessionUseCase terminateSessionUseCase;
    private final SessionRestMapper mapper;

    @GetMapping("/current")
    public SessionStatusResponse getCurrentSession() {
        return mapper.toStatusResponse(getCurrentSessionUseCase.getCurrentSession());
    }

    @PostMapping("/current/extend")
    public SessionStatusResponse extend() {
        return mapper.toStatusResponse(extendSessionUseCase.extend());
    }

    @GetMapping
    @RequirePermission("SESSION_READ")
    public Page<SessionSummaryResponse> list(
            @PageableDefault(size = 20, sort = "lastUsedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return mapper.toSummaryResponse(listSessionsUseCase.list(pageable));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("SESSION_TERMINATE")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void terminate(@PathVariable UUID id, @RequestParam(required = false) String reason) {
        terminateSessionUseCase.terminate(new TerminateSessionCommand(id, reason));
    }
}