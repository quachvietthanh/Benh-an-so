package com.benhsoan.port.inbound.appointment;

import java.util.UUID;

import com.benhsoan.port.dto.result.appointment.DoctorTimeOffResult;

public interface CancelDoctorTimeOffUseCase {

    DoctorTimeOffResult cancelTimeOff(UUID doctorId, UUID timeOffId);

}
