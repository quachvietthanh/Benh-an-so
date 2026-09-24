package com.benhsoan.port.inbound.session;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.port.dto.result.session.SessionSummaryResult;

public interface ListSessionsUseCase {

    Page<SessionSummaryResult> list(Pageable pageable);
}