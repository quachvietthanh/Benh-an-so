package com.benhsoan.application.ucservice.appointment;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;
import com.benhsoan.port.inbound.appointment.GetPatientPortalAppointmentDetailUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-004 Issue 1: returns a single appointment for the authenticated
 * patient, enforcing cross-patient ownership (TC-03 / QTN-23).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientPortalAppointmentDetailService implements GetPatientPortalAppointmentDetailUseCase {

    private final AppointmentRepository appointmentRepository;

    private final PatientAccessGuard patientAccessGuard;

    private final PatientAppointmentResultMapper resultMapper;

    @Override
    public PatientAppointmentResult getAppointmentDetail(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

        patientAccessGuard.requirePatientOwnership(
                appointment.getPatientId(),
                ResourceType.APPOINTMENT,
                appointment.getId());

        return resultMapper.toResult(appointment);
    }
}
