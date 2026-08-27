package com.benhsoan.port.inbound.appointment;

import java.util.UUID;

import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;

public interface GetPatientPortalAppointmentDetailUseCase {

    PatientAppointmentResult getAppointmentDetail(UUID appointmentId);

}
