package com.benhsoan.application.ucservice.appointment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.DoctorTimeOff;
import com.benhsoan.domain.appointment.enums.TimeOffStatus;
import com.benhsoan.domain.appointment.exception.DoctorTimeOffNotFoundException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.dto.result.appointment.AffectedAppointmentResult;
import com.benhsoan.port.inbound.appointment.GetAffectedAppointmentsByTimeOffUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAffectedAppointmentsByTimeOffService implements GetAffectedAppointmentsByTimeOffUseCase {

    private final DoctorTimeOffRepository doctorTimeOffRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;

    @Override
    public List<AffectedAppointmentResult> getAffectedAppointments(UUID timeOffId) {
        DoctorTimeOff timeOff = doctorTimeOffRepository.findById(timeOffId)
                .orElseThrow(() -> new DoctorTimeOffNotFoundException(timeOffId));

        if (timeOff.getStatus() == TimeOffStatus.CANCELLED) {
            return List.of();
        }

        List<Appointment> conflictingAppointments = appointmentRepository.findActiveAppointmentsForDoctorBetween(
                timeOff.getDoctorId(),
                timeOff.getStartTime(),
                timeOff.getEndTime()
        );

        List<AffectedAppointmentResult> affectedList = new ArrayList<>();
        for (Appointment appt : conflictingAppointments) {
            String patientName = null;
            String patientPhone = null;
            if (appt.getPatientId() != null) {
                Patient patient = patientRepository.findById(appt.getPatientId()).orElse(null);
                if (patient != null) {
                    patientName = patient.getFullName();
                    patientPhone = patient.getPhone();
                }
            }

            affectedList.add(new AffectedAppointmentResult(
                    appt.getId(),
                    appt.getAppointmentCode(),
                    appt.getPatientId(),
                    patientName,
                    patientPhone,
                    appt.getStartTime(),
                    appt.getEndTime(),
                    appt.getStatus(),
                    appt.getReason()
            ));
        }

        return affectedList;
    }
}
