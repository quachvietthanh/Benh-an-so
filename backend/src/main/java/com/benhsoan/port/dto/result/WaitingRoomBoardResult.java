package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record WaitingRoomBoardResult(
        LocalDate date,
        Instant updatedAt,
        List<RoomQueueDisplayResult> rooms
) {}
