package com.benhsoan.adapter.inbound.rest.request.queue;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
public record AssignDoctorRoomRequest(@NotNull UUID roomId) { }
