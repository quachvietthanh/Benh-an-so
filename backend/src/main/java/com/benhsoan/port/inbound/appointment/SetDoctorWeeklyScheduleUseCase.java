package com.benhsoan.port.inbound.appointment;

import com.benhsoan.port.dto.command.appointment.SetDoctorWeeklyScheduleCommand;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyScheduleResult;

public interface SetDoctorWeeklyScheduleUseCase {

    DoctorWeeklyScheduleResult setWeeklySchedule(SetDoctorWeeklyScheduleCommand command);

}
