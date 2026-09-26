package com.benhsoan.domain.appointment.enums;

import java.util.Set;

public enum AppointmentStatus {

    SCHEDULED,

    CONFIRMED,

    CHECKED_IN,

    IN_PROGRESS,

    COMPLETED,

    CANCELLED,

    NO_SHOW;

    public static final Set<AppointmentStatus> ACTIVE_STATUSES = Set.of(
            SCHEDULED,
            CONFIRMED,
            CHECKED_IN,
            IN_PROGRESS
    );

    public static final Set<AppointmentStatus> TERMINAL_STATUSES = Set.of(
            COMPLETED,
            CANCELLED,
            NO_SHOW
    );

    public boolean isActive() {
        return ACTIVE_STATUSES.contains(this);
    }

    public boolean isTerminal() {
        return TERMINAL_STATUSES.contains(this);
    }

}
