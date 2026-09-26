package com.benhsoan.port.inbound.appointment;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.appointment.DoctorWeeklyScheduleResult;

public interface GetDoctorScheduleUseCase {

    List<DoctorWeeklyScheduleResult> getWeeklySchedule(UUID doctorId);

}
