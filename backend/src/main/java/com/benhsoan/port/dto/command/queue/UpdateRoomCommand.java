package com.benhsoan.port.dto.command.queue;

import java.util.UUID;

public record UpdateRoomCommand(UUID roomId, String name) {
}
