package com.benhsoan.port.inbound.appointment;

import java.util.List;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;

public interface GetPatientPortalAppointmentsUseCase {

    List<PatientAppointmentResult> getAppointments(AppointmentStatus statusFilter);

}
