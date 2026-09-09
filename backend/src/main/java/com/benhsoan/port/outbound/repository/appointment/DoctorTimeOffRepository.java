package com.benhsoan.port.outbound.repository.appointment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.appointment.DoctorTimeOff;

public interface DoctorTimeOffRepository {

    DoctorTimeOff save(DoctorTimeOff timeOff);

    Optional<DoctorTimeOff> findById(UUID id);

    List<DoctorTimeOff> findByDoctorId(UUID doctorId);

    List<DoctorTimeOff> findActiveOverlapping(UUID doctorId, Instant startTime, Instant endTime);

    boolean existsActiveOverlapping(UUID doctorId, Instant startTime, Instant endTime);

}
