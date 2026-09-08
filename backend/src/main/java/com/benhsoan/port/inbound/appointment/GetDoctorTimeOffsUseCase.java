package com.benhsoan.port.inbound.appointment;

import java.util.List;

import com.benhsoan.port.dto.query.appointment.GetDoctorTimeOffsQuery;
import com.benhsoan.port.dto.result.appointment.DoctorTimeOffResult;

public interface GetDoctorTimeOffsUseCase {

    List<DoctorTimeOffResult> getTimeOffs(GetDoctorTimeOffsQuery query);

}
