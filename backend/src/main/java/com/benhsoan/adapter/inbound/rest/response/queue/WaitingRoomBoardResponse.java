package com.benhsoan.adapter.inbound.rest.response.queue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record WaitingRoomBoardResponse(
        LocalDate date,
        Instant updatedAt,
        List<RoomQueueDisplayResponse> rooms
) {}
