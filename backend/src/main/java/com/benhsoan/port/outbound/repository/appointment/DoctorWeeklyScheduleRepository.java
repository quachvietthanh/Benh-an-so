package com.benhsoan.port.outbound.repository.appointment;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.appointment.DoctorWeeklySchedule;

public interface DoctorWeeklyScheduleRepository {

    List<DoctorWeeklySchedule> findByDoctorId(UUID doctorId);

    List<DoctorWeeklySchedule> findByDoctorIdAndDayOfWeek(UUID doctorId, DayOfWeek dayOfWeek);

    DoctorWeeklySchedule save(DoctorWeeklySchedule schedule);

    List<DoctorWeeklySchedule> saveAll(List<DoctorWeeklySchedule> schedules);

    void deleteByDoctorId(UUID doctorId);

}
