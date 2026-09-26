package com.benhsoan.application.ucservice.appointment;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
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
 *
 * <p>NCL-14-CN-010: the appointment's stored {@code patientId} is authoritative. Ownership or
 * guardian scope is always resolved from the authenticated identity plus that stored patient, so
 * the request never needs to supply a patientId and a supplied one can never widen access. When
 * {@code patientId} is supplied it is used only as an extra consistency check against the
 * appointment's real patient; a mismatch is denied.</p>
 *
 * <p>IDOR hardening: a nonexistent appointment and an existing appointment outside the caller's
 * scope are deliberately indistinguishable from the outside (both HTTP 404), matching the
 * project's existing patient-portal detail convention, so the endpoint is not an appointment
 * existence oracle. The denial is still recorded as an ACCESS_DENIED audit before the 404.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientPortalAppointmentDetailService implements GetPatientPortalAppointmentDetailUseCase {

    private final AppointmentRepository appointmentRepository;

    private final PatientAccessGuard patientAccessGuard;

    private final PatientAppointmentResultMapper resultMapper;

    @Override
    public PatientAppointmentResult getAppointmentDetail(UUID appointmentId, UUID patientId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

        UUID appointmentPatientId = appointment.getPatientId();

        // A supplied patientId is never proof of authority: it must match the appointment's
        // authoritative patient, otherwise the request is denied without resolving any scope.
        if (patientId != null && !appointmentPatientId.equals(patientId)) {
            patientAccessGuard.denyPatientAccess(
                    appointmentPatientId, ResourceType.APPOINTMENT, appointment.getId());
            throw new AppointmentNotFoundException(appointmentId);
        }

        try {
            // Scope is derived from the appointment's own patient, which covers both the direct
            // owner and an authorised guardian (family scope).
            patientAccessGuard.requirePatientAccess(
                    appointmentPatientId,
                    ResourceType.APPOINTMENT,
                    appointment.getId());
        } catch (AccessDeniedException exception) {
            // Normalise "exists but not yours" to the same externally observable result as
            // "does not exist" so appointment existence/ownership is not leaked.
            throw new AppointmentNotFoundException(appointmentId);
        }

        return resultMapper.toResult(appointment);
    }
}
