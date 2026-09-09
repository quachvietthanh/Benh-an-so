package com.benhsoan.domain.appointment.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class AppointmentStatusTest {

    @Test
    void activeStatusesAreDefinedCorrectly() {
        // Active non-terminal statuses: SCHEDULED, CONFIRMED, CHECKED_IN, IN_PROGRESS
        assertTrue(AppointmentStatus.SCHEDULED.isActive());
        assertTrue(AppointmentStatus.CONFIRMED.isActive());
        assertTrue(AppointmentStatus.CHECKED_IN.isActive());
        assertTrue(AppointmentStatus.IN_PROGRESS.isActive());

        assertFalse(AppointmentStatus.SCHEDULED.isTerminal());
        assertFalse(AppointmentStatus.CONFIRMED.isTerminal());
        assertFalse(AppointmentStatus.CHECKED_IN.isTerminal());
        assertFalse(AppointmentStatus.IN_PROGRESS.isTerminal());
    }

    @Test
    void terminalStatusesAreDefinedCorrectly() {
        // Terminal statuses: COMPLETED, CANCELLED, NO_SHOW
        assertTrue(AppointmentStatus.COMPLETED.isTerminal());
        assertTrue(AppointmentStatus.CANCELLED.isTerminal());
        assertTrue(AppointmentStatus.NO_SHOW.isTerminal());

        assertFalse(AppointmentStatus.COMPLETED.isActive());
        assertFalse(AppointmentStatus.CANCELLED.isActive());
        assertFalse(AppointmentStatus.NO_SHOW.isActive());
    }

    @Test
    void everyStatusIsEitherActiveOrTerminalWithoutOverlap() {
        Set<AppointmentStatus> allStatuses = Set.of(AppointmentStatus.values());
        Set<AppointmentStatus> combined = new HashSet<>(AppointmentStatus.ACTIVE_STATUSES);
        combined.addAll(AppointmentStatus.TERMINAL_STATUSES);

        assertEquals(allStatuses, combined);
        assertEquals(allStatuses.size(), AppointmentStatus.ACTIVE_STATUSES.size() + AppointmentStatus.TERMINAL_STATUSES.size(),
                "Active and terminal statuses must not have overlapping members");
    }
}
