package com.benhsoan.application.ucservice.appointment;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;
import com.benhsoan.port.inbound.appointment.GetPatientPortalAppointmentDetailUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-004 Issue 1: returns a single appointment for the authenticated
 * patient, enforcing cross-patient ownership (TC-03 / QTN-23).
 *
 * <p>NCL-14-CN-010 CV-02: an optional {@code patientId} selects a linked dependent patient.
 * The appointment is then required to belong to the authorised scope, so a client-supplied
 * {@code patientId} can never widen access to a third party's appointment (IDOR guard).</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientPortalAppointmentDetailService implements GetPatientPortalAppointmentDetailUseCase {

    private static final String SCOPE_MISMATCH_MESSAGE =
            "Appointment does not belong to the authorised patient.";

    private final AppointmentRepository appointmentRepository;

    private final PatientAccessGuard patientAccessGuard;

    private final PatientAppointmentResultMapper resultMapper;

    @Override
    public PatientAppointmentResult getAppointmentDetail(UUID appointmentId, UUID patientId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

        Patient authorised = patientId == null
                ? patientAccessGuard.requirePatientOwnership(
                        appointment.getPatientId(),
                        ResourceType.APPOINTMENT,
                        appointment.getId())
                : patientAccessGuard.requirePatientAccess(
                        patientId,
                        ResourceType.APPOINTMENT,
                        appointment.getId());

        if (!appointment.getPatientId().equals(authorised.getId())) {
            patientAccessGuard.denyPatientAccess(
                    appointment.getPatientId(),
                    ResourceType.APPOINTMENT,
                    appointment.getId());
            throw new AccessDeniedException(SCOPE_MISMATCH_MESSAGE);
        }

        return resultMapper.toResult(appointment);
    }
}
