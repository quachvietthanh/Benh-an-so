package com.benhsoan.port.inbound.appointment;

import com.benhsoan.port.dto.query.appointment.GetDoctorWeeklyScheduleTableQuery;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyTableResult;

public interface GetDoctorWeeklyScheduleTableUseCase {

    DoctorWeeklyTableResult getWeeklyScheduleTable(GetDoctorWeeklyScheduleTableQuery query);

}
