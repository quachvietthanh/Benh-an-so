package com.benhsoan.application.ucservice.appointment;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.dto.result.appointment.AppointmentRescheduleHistoryResult;
import com.benhsoan.port.dto.result.appointment.WaitlistSuggestionResult;

@Component
public class AppointmentResultMapper {

    public AppointmentResult toResult(
            Appointment appointment
    ) {
        return toResult(appointment, List.of());
    }

    public AppointmentResult toResult(
            Appointment appointment,
            List<AppointmentRescheduleHistoryResult> histories
    ) {
        return toResult(appointment, histories, null);
    }

    public AppointmentResult toResult(
            Appointment appointment,
            List<AppointmentRescheduleHistoryResult> histories,
            String confirmedByName
    ) {
        return toResult(appointment, histories, confirmedByName, null);
    }

    public AppointmentResult toResult(
            Appointment appointment,
            List<AppointmentRescheduleHistoryResult> histories,
            String confirmedByName,
            WaitlistSuggestionResult suggestedWaitlistEntry
    ) {
        return AppointmentResult.builder()
                .id(appointment.getId())
                .appointmentCode(appointment.getAppointmentCode())
                .patientId(appointment.getPatientId())
                .doctorId(appointment.getDoctorId())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .status(appointment.getStatus())
                .reason(appointment.getReason())
                .cancelReason(appointment.getCancelReason())
                .checkedInAt(appointment.getCheckedInAt())
                .completedAt(appointment.getCompletedAt())
                .createdBy(appointment.getCreatedBy())
                .createdAt(appointment.getCreatedAt())
                .confirmedAt(appointment.getConfirmedAt())
                .confirmedBy(appointment.getConfirmedBy())
                .confirmedByName(confirmedByName)
                .rescheduleHistories(histories != null ? histories : List.of())
                .suggestedWaitlistEntry(suggestedWaitlistEntry)
                .build();
    }

}