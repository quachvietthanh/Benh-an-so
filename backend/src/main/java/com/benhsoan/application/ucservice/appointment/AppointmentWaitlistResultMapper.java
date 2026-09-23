package com.benhsoan.application.ucservice.appointment;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.appointment.AppointmentWaitlist;
import com.benhsoan.port.dto.result.appointment.AppointmentWaitlistResult;
import com.benhsoan.port.dto.result.appointment.WaitlistSuggestionResult;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppointmentWaitlistResultMapper {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    public AppointmentWaitlistResult toResult(AppointmentWaitlist waitlist) {
        if (waitlist == null) {
            return null;
        }

        String patientName = null;
        String patientPhone = null;
        var patientOpt = patientRepository.findById(waitlist.getPatientId());
        if (patientOpt.isPresent()) {
            var patient = patientOpt.get();
            patientName = patient.getFullName();
            patientPhone = patient.getPhone();
        }

        String doctorName = null;
        var doctorOpt = userRepository.findById(waitlist.getDoctorId());
        if (doctorOpt.isPresent()) {
            doctorName = doctorOpt.get().getFullName();
        }

        String createdByName = null;
        if (waitlist.getCreatedBy() != null) {
            var creatorOpt = userRepository.findById(waitlist.getCreatedBy());
            if (creatorOpt.isPresent()) {
                createdByName = creatorOpt.get().getFullName();
            }
        }

        return AppointmentWaitlistResult.builder()
                .id(waitlist.getId())
                .patientId(waitlist.getPatientId())
                .patientName(patientName)
                .patientPhone(patientPhone)
                .doctorId(waitlist.getDoctorId())
                .doctorName(doctorName)
                .desiredDate(waitlist.getDesiredDate())
                .timePreference(waitlist.getTimePreference())
                .status(waitlist.getStatus())
                .note(waitlist.getNote())
                .cancelReason(waitlist.getCancelReason())
                .bookedAppointmentId(waitlist.getBookedAppointmentId())
                .createdBy(waitlist.getCreatedBy())
                .createdByName(createdByName)
                .createdAt(waitlist.getCreatedAt())
                .updatedAt(waitlist.getUpdatedAt())
                .build();
    }

    public WaitlistSuggestionResult toSuggestionResult(AppointmentWaitlist waitlist) {
        if (waitlist == null) {
            return null;
        }

        String patientName = null;
        String patientPhone = null;
        var patientOpt = patientRepository.findById(waitlist.getPatientId());
        if (patientOpt.isPresent()) {
            var patient = patientOpt.get();
            patientName = patient.getFullName();
            patientPhone = patient.getPhone();
        }

        String doctorName = null;
        var doctorOpt = userRepository.findById(waitlist.getDoctorId());
        if (doctorOpt.isPresent()) {
            doctorName = doctorOpt.get().getFullName();
        }

        return WaitlistSuggestionResult.builder()
                .waitlistId(waitlist.getId())
                .patientId(waitlist.getPatientId())
                .patientName(patientName)
                .patientPhone(patientPhone)
                .doctorId(waitlist.getDoctorId())
                .doctorName(doctorName)
                .desiredDate(waitlist.getDesiredDate())
                .timePreference(waitlist.getTimePreference())
                .note(waitlist.getNote())
                .createdAt(waitlist.getCreatedAt())
                .build();
    }
}
