package com.benhsoan.port.dto.result;

import java.util.List;
import java.util.UUID;

public record RoomQueueDisplayResult(
        UUID roomId,
        String roomNumber,
        String roomName,
        UUID doctorId,
        String doctorName,
        QueueDisplayItemResult currentCalling,
        List<QueueDisplayItemResult> waitingList
) {}
