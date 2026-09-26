package com.benhsoan.port.inbound.appointment;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.appointment.DoctorTimeOffResult;

public interface GetDoctorTimeOffsUseCase {

    List<DoctorTimeOffResult> getTimeOffs(UUID doctorId);

}
