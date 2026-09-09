package com.benhsoan.port.inbound.appointment;

import com.benhsoan.port.dto.command.appointment.RegisterDoctorTimeOffCommand;
import com.benhsoan.port.dto.result.appointment.DoctorTimeOffResult;

public interface RegisterDoctorTimeOffUseCase {

    DoctorTimeOffResult registerTimeOff(RegisterDoctorTimeOffCommand command);

}
