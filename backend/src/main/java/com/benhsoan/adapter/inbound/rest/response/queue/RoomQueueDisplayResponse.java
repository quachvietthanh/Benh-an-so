package com.benhsoan.adapter.inbound.rest.response.queue;

import java.util.List;
import java.util.UUID;

public record RoomQueueDisplayResponse(
        UUID roomId,
        String roomNumber,
        String roomName,
        UUID doctorId,
        String doctorName,
        QueueDisplayItemResponse currentCalling,
        List<QueueDisplayItemResponse> waitingList
) {}
