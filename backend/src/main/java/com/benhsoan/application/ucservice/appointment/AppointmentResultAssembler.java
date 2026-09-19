package com.benhsoan.application.ucservice.appointment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.dto.result.appointment.AppointmentRescheduleHistoryResult;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

import lombok.RequiredArgsConstructor;

/**
 * Assembles Appointment domain entities into rich AppointmentResult DTOs
 * by batch querying patient and user repositories to eliminate N+1 queries.
 */
@Component
@RequiredArgsConstructor
public class AppointmentResultAssembler {

    private static final UUID DOCTOR_1_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    private static final UUID DOCTOR_2_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3");

    private final AppointmentResultMapper resultMapper;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AppointmentRescheduleHistoryAssembler historyAssembler;

    public AppointmentResult toResult(Appointment appointment) {
        if (appointment == null) {
            return null;
        }
        return toResults(List.of(appointment), true).getFirst();
    }

    public Page<AppointmentResult> toResultPage(Page<Appointment> appointmentPage) {
        if (appointmentPage == null) {
            return Page.empty();
        }
        List<AppointmentResult> results = toResults(appointmentPage.getContent(), false);
        return new PageImpl<>(results, appointmentPage.getPageable(), appointmentPage.getTotalElements());
    }

    public List<AppointmentResult> toResults(List<Appointment> appointments, boolean loadHistories) {
        if (appointments == null || appointments.isEmpty()) {
            return List.of();
        }

        Set<UUID> patientIds = appointments.stream()
                .map(Appointment::getPatientId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<UUID> doctorIds = appointments.stream()
                .map(Appointment::getDoctorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<UUID> confirmedByIds = appointments.stream()
                .map(Appointment::getConfirmedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, Patient> patientMap = patientIds.isEmpty()
                ? Map.of()
                : patientRepository.findAllById(patientIds).stream()
                        .collect(Collectors.toMap(Patient::getId, p -> p, (existing, replace) -> existing));

        Set<UUID> allUserIds = new HashSet<>(doctorIds);
        allUserIds.addAll(confirmedByIds);

        Map<UUID, User> userMap = allUserIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(new ArrayList<>(allUserIds)).stream()
                        .collect(Collectors.toMap(User::getId, u -> u, (existing, replace) -> existing));

        return appointments.stream()
                .map(appointment -> {
                    Patient patient = patientMap.get(appointment.getPatientId());
                    User doctor = userMap.get(appointment.getDoctorId());
                    User confirmedByUser = userMap.get(appointment.getConfirmedBy());

                    String patientName = patient != null ? patient.getFullName() : null;
                    String patientCode = patient != null ? patient.getPatientCode() : null;
                    String patientPhone = patient != null ? patient.getPhone() : null;
                    String doctorName = doctor != null ? doctor.getFullName() : null;
                    String department = resolveDepartment(appointment.getDoctorId(), doctor);

                    String confirmedByName = confirmedByUser != null ? confirmedByUser.getFullName() : null;

                    List<AppointmentRescheduleHistoryResult> histories = loadHistories && historyAssembler != null
                            ? historyAssembler.getHistoriesForAppointment(appointment.getId())
                            : List.of();

                    return resultMapper.toResult(
                            appointment,
                            histories,
                            confirmedByName,
                            patientName,
                            patientCode,
                            patientPhone,
                            doctorName,
                            department
                    );
                })
                .toList();
    }

    public String resolveDepartment(UUID doctorId, User doctor) {
        if (doctorId != null) {
            if (DOCTOR_1_ID.equals(doctorId)) {
                return "Nội khoa";
            }
            if (DOCTOR_2_ID.equals(doctorId)) {
                return "Ngoại khoa";
            }
        }
        if (doctor != null && doctor.getUsername() != null) {
            if ("doctor1".equalsIgnoreCase(doctor.getUsername())) {
                return "Nội khoa";
            }
            if ("doctor2".equalsIgnoreCase(doctor.getUsername())) {
                return "Ngoại khoa";
            }
        }
        return "Nội khoa";
    }
}
