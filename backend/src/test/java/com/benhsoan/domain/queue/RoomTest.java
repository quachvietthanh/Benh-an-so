package com.benhsoan.domain.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.shared.exception.ValidationException;

class RoomTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-02T02:00:00Z");

    @Test
    void createsActiveRoomAndNormalizesCode() {
        Room room = Room.create("  p103  ", "  Phong kham 103  ", CREATED_AT);

        assertEquals("P103", room.getCode());
        assertEquals("Phong kham 103", room.getName());
        assertTrue(room.isActive());
        assertEquals(CREATED_AT, room.getCreatedAt());
    }

    @Test
    void updatesNameWithoutChangingCode() {
        Room room = Room.create("P103", "Phong kham 103", CREATED_AT);
        Instant updatedAt = CREATED_AT.plusSeconds(60);

        room.updateName("  Phong kham Noi tong quat  ", updatedAt);

        assertEquals("P103", room.getCode());
        assertEquals("Phong kham Noi tong quat", room.getName());
        assertEquals(updatedAt, room.getUpdatedAt());
    }

    @Test
    void activatesAndDeactivatesWithoutDeletingRoom() {
        Room room = Room.create("P103", "Phong kham 103", CREATED_AT);
        Instant deactivatedAt = CREATED_AT.plusSeconds(60);
        Instant activatedAt = CREATED_AT.plusSeconds(120);

        room.deactivate(deactivatedAt);
        assertFalse(room.isActive());
        assertEquals(deactivatedAt, room.getUpdatedAt());

        room.activate(activatedAt);
        assertTrue(room.isActive());
        assertEquals(activatedAt, room.getUpdatedAt());
    }

    @Test
    void rejectsBlankOrOversizedRoomData() {
        assertThrows(ValidationException.class, () -> Room.create(" ", "Phong kham", CREATED_AT));
        assertThrows(ValidationException.class, () -> Room.create("P".repeat(31), "Phong kham", CREATED_AT));
        assertThrows(ValidationException.class, () -> Room.create("P103", " ", CREATED_AT));
        assertThrows(ValidationException.class, () -> Room.create("P103", "P".repeat(101), CREATED_AT));
    }
}
