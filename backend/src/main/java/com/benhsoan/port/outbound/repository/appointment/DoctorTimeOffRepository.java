package com.benhsoan.port.outbound.repository.appointment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.appointment.DoctorTimeOff;
import com.benhsoan.domain.appointment.enums.TimeOffStatus;

public interface DoctorTimeOffRepository {

    Optional<DoctorTimeOff> findById(UUID id);

    DoctorTimeOff save(DoctorTimeOff timeOff);

    List<DoctorTimeOff> findOverlappingActiveTimeOffs(UUID doctorId, Instant startTime, Instant endTime);

    List<DoctorTimeOff> search(UUID doctorId, TimeOffStatus status, Instant fromTime, Instant toTime);

}
