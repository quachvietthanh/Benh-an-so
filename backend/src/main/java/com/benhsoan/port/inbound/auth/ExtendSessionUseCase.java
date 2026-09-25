package com.benhsoan.port.inbound.auth;

import com.benhsoan.port.dto.result.auth.ExtendSessionResult;

public interface ExtendSessionUseCase {

    ExtendSessionResult extendCurrentSession();
}
