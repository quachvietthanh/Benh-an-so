package com.benhsoan.application.ucservice.backup;

import java.util.UUID;

/**
 * The seeded {@code system} account (V2) used as the actor for scheduled,
 * background-initiated backup runs, where no authenticated user context exists.
 */
public final class BackupSystemActor {

    public static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private BackupSystemActor() {
    }
}
