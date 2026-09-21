package com.benhsoan.port.outbound.repository.specialty;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RoomSpecialtyRepository {

    void assignRooms(UUID specialtyId, Collection<UUID> roomIds, Instant assignedAt);

    void removeAssignmentsBySpecialtyId(UUID specialtyId);

    List<UUID> findRoomIdsBySpecialtyId(UUID specialtyId);

    List<UUID> findSpecialtyIdsByRoomId(UUID roomId);

    long countBySpecialtyId(UUID specialtyId);
}
