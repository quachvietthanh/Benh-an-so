package com.benhsoan.port.outbound.repository.queue;

import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.queue.DoctorRoomAssignment;

public interface DoctorRoomAssignmentRepository {
    Optional<DoctorRoomAssignment> findByDoctorId(UUID doctorId);

    Optional<DoctorRoomAssignment> findByDoctorIdForUpdate(UUID doctorId);

    Optional<DoctorRoomAssignment> findByRoomId(UUID roomId);

    DoctorRoomAssignment save(DoctorRoomAssignment assignment);

    void deleteByDoctorId(UUID doctorId);
}
