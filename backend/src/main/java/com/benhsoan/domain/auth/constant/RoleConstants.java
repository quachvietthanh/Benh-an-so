package com.benhsoan.domain.auth.constant;

import java.util.UUID;

public final class RoleConstants {

    private RoleConstants() {
    }

    public static final UUID ADMIN =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    public static final UUID MANAGER =
            UUID.fromString("66666666-6666-6666-6666-666666666666");

    public static final UUID DOCTOR =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    public static final UUID NURSE =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    public static final UUID RECEPTIONIST =
            UUID.fromString("44444444-4444-4444-4444-444444444444");

    public static final UUID PHARMACIST =
            UUID.fromString("55555555-5555-5555-5555-555555555555");
}
