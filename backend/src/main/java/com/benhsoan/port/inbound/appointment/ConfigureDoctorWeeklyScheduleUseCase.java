package com.benhsoan.port.inbound.appointment;

import java.util.List;

import com.benhsoan.port.dto.command.appointment.ConfigureDoctorWeeklyScheduleCommand;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyScheduleResult;

public interface ConfigureDoctorWeeklyScheduleUseCase {

    List<DoctorWeeklyScheduleResult> configureWeeklySchedule(ConfigureDoctorWeeklyScheduleCommand command);

}
