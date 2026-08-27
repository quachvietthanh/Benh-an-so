package com.benhsoan.application.ucservice.appointment;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;
import com.benhsoan.port.inbound.appointment.GetPatientPortalAppointmentsUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-004 Issue 1: lists the authenticated patient's appointments. When no
 * status filter is supplied, only the active upcoming statuses are returned
 * (SCHEDULED, CONFIRMED), ordered by appointment start time ascending.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientPortalAppointmentsService implements GetPatientPortalAppointmentsUseCase {

    private static final List<AppointmentStatus> DEFAULT_ACTIVE_STATUSES =
            List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED);

    private final AppointmentRepository appointmentRepository;

    private final PatientRepository patientRepository;

    private final CurrentUserPort currentUserPort;

    private final PatientAppointmentResultMapper resultMapper;

    @Override
    public List<PatientAppointmentResult> getAppointments(AppointmentStatus statusFilter) {
        Patient patient = patientRepository.findByUserId(currentUserPort.getCurrentUserId())
                .orElseThrow(() -> new AccessDeniedException(
                        "No patient profile is linked to the authenticated user."));

        List<AppointmentStatus> statuses = statusFilter == null
                ? DEFAULT_ACTIVE_STATUSES
                : List.of(statusFilter);

        return appointmentRepository.findByPatientIdAndStatusInOrderByStartTimeAsc(
                        patient.getId(),
                        statuses
                ).stream()
                .map(resultMapper::toResult)
                .toList();
    }
}
