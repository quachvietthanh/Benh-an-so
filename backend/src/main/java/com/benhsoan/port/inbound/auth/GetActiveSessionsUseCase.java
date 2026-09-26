package com.benhsoan.port.inbound.auth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.port.dto.result.auth.ActiveSessionResult;

public interface GetActiveSessionsUseCase {

    Page<ActiveSessionResult> getActiveSessions(Pageable pageable);
}
