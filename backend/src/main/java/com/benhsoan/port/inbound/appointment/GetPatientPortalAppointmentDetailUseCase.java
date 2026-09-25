package com.benhsoan.port.inbound.appointment;

import java.util.UUID;

import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;

public interface GetPatientPortalAppointmentDetailUseCase {

    /**
     * @param appointmentId the appointment to read
     * @param patientId     optional target patient scope (NCL-14-CN-010). {@code null} keeps
     *                      the own-only behaviour; a non-null value selects a linked dependent
     *                      patient and is authorised server-side.
     */
    PatientAppointmentResult getAppointmentDetail(UUID appointmentId, UUID patientId);

}
