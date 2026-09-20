package com.benhsoan.port.outbound.repository.specialty;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DoctorSpecialtyRepository {

    void assignDoctors(UUID specialtyId, Collection<UUID> doctorIds, UUID assignedBy, Instant assignedAt);

    void removeAssignmentsBySpecialtyId(UUID specialtyId);

    List<UUID> findDoctorIdsBySpecialtyId(UUID specialtyId);

    List<UUID> findSpecialtyIdsByDoctorId(UUID doctorId);

    long countBySpecialtyId(UUID specialtyId);
}
