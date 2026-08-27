package com.benhsoan.port.outbound.repository.appointment;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.appointment.DoctorSchedule;

public interface DoctorScheduleRepository {

    Optional<DoctorSchedule> findByDoctorIdAndScheduleDate(UUID doctorId, LocalDate scheduleDate);

    Optional<DoctorSchedule> findByDoctorIdAndScheduleDateForUpdate(UUID doctorId, LocalDate scheduleDate);

    DoctorSchedule save(DoctorSchedule schedule);

}
