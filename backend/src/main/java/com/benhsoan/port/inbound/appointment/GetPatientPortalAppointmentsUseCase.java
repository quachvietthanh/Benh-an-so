package com.benhsoan.port.inbound.appointment;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;

public interface GetPatientPortalAppointmentsUseCase {

    /**
     * @param statusFilter optional status filter ({@code null} = active statuses)
     * @param patientId    optional target patient (NCL-14-CN-010). {@code null} keeps the
     *                     own-profile behaviour; a non-null value selects a linked dependent
     *                     patient and is authorised server-side.
     */
    List<PatientAppointmentResult> getAppointments(AppointmentStatus statusFilter, UUID patientId);

}
