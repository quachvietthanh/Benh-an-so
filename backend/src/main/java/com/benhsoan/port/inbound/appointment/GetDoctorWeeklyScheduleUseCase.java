package com.benhsoan.port.inbound.appointment;

import com.benhsoan.port.dto.query.appointment.GetDoctorWeeklyScheduleQuery;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyScheduleResult;

public interface GetDoctorWeeklyScheduleUseCase {

    DoctorWeeklyScheduleResult getWeeklySchedule(GetDoctorWeeklyScheduleQuery query);

}
