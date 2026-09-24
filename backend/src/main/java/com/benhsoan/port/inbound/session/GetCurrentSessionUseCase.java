package com.benhsoan.port.inbound.session;

import com.benhsoan.port.dto.result.session.SessionStatusResult;

public interface GetCurrentSessionUseCase {

    SessionStatusResult getCurrentSession();
}