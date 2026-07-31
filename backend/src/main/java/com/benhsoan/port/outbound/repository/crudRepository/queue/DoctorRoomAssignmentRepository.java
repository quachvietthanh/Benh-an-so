package com.benhsoan.port.outbound.repository.crudRepository.queue;

import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.queue.DoctorRoomAssignment;

public interface DoctorRoomAssignmentRepository { Optional<DoctorRoomAssignment> findByDoctorId(UUID doctorId); }
