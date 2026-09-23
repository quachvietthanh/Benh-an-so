package com.benhsoan.adapter.inbound.rest.response.auth;

import java.time.Instant;
import java.util.UUID;

public record TwoFactorResendResponse(

        UUID twoFactorToken,

        Instant expiresAt

) {}
