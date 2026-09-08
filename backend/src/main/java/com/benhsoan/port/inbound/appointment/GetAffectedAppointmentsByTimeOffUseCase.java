package com.benhsoan.port.inbound.appointment;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.appointment.AffectedAppointmentResult;

public interface GetAffectedAppointmentsByTimeOffUseCase {

    List<AffectedAppointmentResult> getAffectedAppointments(UUID timeOffId);

}
