package com.benhsoan.port.inbound.appointment;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.appointment.AppointmentSeriesResult;

public interface GetPatientAppointmentSeriesUseCase {

    List<AppointmentSeriesResult> getByPatientId(UUID patientId);
}
