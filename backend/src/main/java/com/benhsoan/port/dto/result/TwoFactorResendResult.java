package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

public record TwoFactorResendResult(

        UUID twoFactorToken,

        Instant expiresAt

) {}
