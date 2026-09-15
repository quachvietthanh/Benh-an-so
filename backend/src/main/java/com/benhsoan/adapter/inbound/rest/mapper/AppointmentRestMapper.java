package com.benhsoan.adapter.inbound.rest.mapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.appointment.CancelAppointmentRequest;
import com.benhsoan.adapter.inbound.rest.request.appointment.CreateAppointmentRequest;
import com.benhsoan.adapter.inbound.rest.request.appointment.RescheduleAppointmentRequest;
import com.benhsoan.adapter.inbound.rest.response.appointment.AppointmentRescheduleHistoryResponse;
import com.benhsoan.adapter.inbound.rest.response.appointment.AppointmentResponse;
import com.benhsoan.port.dto.command.appointment.CancelAppointmentCommand;
import com.benhsoan.port.dto.command.appointment.CreateAppointmentCommand;
import com.benhsoan.port.dto.command.appointment.MarkAppointmentNoShowCommand;
import com.benhsoan.port.dto.command.appointment.RescheduleAppointmentCommand;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.dto.result.appointment.AppointmentRescheduleHistoryResult;

@Component
public class AppointmentRestMapper {

    public CreateAppointmentCommand toCommand(
            CreateAppointmentRequest request
    ) {

        return CreateAppointmentCommand.builder()
                .patientId(request.patientId())
                .doctorId(request.doctorId())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .reason(request.reason())
                .build();

    }

    public RescheduleAppointmentCommand toCommand(
            RescheduleAppointmentRequest request
    ) {
        return RescheduleAppointmentCommand.builder()
                .newDoctorId(request.newDoctorId())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .reason(request.reason())
                .build();
    }

    public AppointmentResponse toResponse(
            AppointmentResult result
    ) {

        List<AppointmentRescheduleHistoryResponse> histories = result.rescheduleHistories() != null
                ? result.rescheduleHistories().stream()
                        .map(this::toResponse)
                        .toList()
                : List.of();

        return AppointmentResponse.builder()
                .id(result.id())
                .appointmentCode(result.appointmentCode())
                .patientId(result.patientId())
                .doctorId(result.doctorId())
                .startTime(result.startTime())
                .endTime(result.endTime())
                .status(result.status())
                .reason(result.reason())
                .cancelReason(result.cancelReason())
                .checkedInAt(result.checkedInAt())
                .completedAt(result.completedAt())
                .createdAt(result.createdAt())
                .confirmedAt(result.confirmedAt())
                .confirmedBy(result.confirmedBy())
                .confirmedByName(result.confirmedByName())
                .rescheduleHistories(histories)
                .build();

    }

    public AppointmentRescheduleHistoryResponse toResponse(
            AppointmentRescheduleHistoryResult history
    ) {
        return AppointmentRescheduleHistoryResponse.builder()
                .id(history.id())
                .appointmentId(history.appointmentId())
                .oldDoctorId(history.oldDoctorId())
                .newDoctorId(history.newDoctorId())
                .oldDoctorName(history.oldDoctorName())
                .newDoctorName(history.newDoctorName())
                .oldStartTime(history.oldStartTime())
                .oldEndTime(history.oldEndTime())
                .newStartTime(history.newStartTime())
                .newEndTime(history.newEndTime())
                .reason(history.reason())
                .rescheduledBy(history.rescheduledBy())
                .rescheduledByName(history.rescheduledByName())
                .rescheduledAt(history.rescheduledAt())
                .build();
    }

    public CancelAppointmentCommand toCommand(CancelAppointmentRequest request) {
        return CancelAppointmentCommand.builder()
                .cancelReason(request.cancelReason())
                .build();

    }

    public MarkAppointmentNoShowCommand toCommand(UUID appointmentId) {
        return MarkAppointmentNoShowCommand.builder()
                .appointmentId(appointmentId)
                .markedAt(Instant.now())
                .build();
    }

    public Page<AppointmentResponse> toResponse(Page<AppointmentResult> results) {
        return results.map(this::toResponse);
    }

    public com.benhsoan.adapter.inbound.rest.response.appointment.DoctorWeeklyTableResponse toResponse(
            com.benhsoan.port.dto.result.appointment.DoctorWeeklyTableResult result
    ) {
        if (result == null) {
            return null;
        }
        return com.benhsoan.adapter.inbound.rest.response.appointment.DoctorWeeklyTableResponse.builder()
                .weekStartDate(result.weekStartDate())
                .weekEndDate(result.weekEndDate())
                .clinicStartTime(result.clinicStartTime())
                .clinicEndTime(result.clinicEndTime())
                .timeSlots(result.timeSlots())
                .doctors(result.doctors() != null
                        ? result.doctors().stream().map(this::toResponse).toList()
                        : List.of())
                .days(result.days() != null
                        ? result.days().stream().map(this::toResponse).toList()
                        : List.of())
                .build();
    }

    public com.benhsoan.adapter.inbound.rest.response.appointment.DoctorWeeklyTableResponse.DoctorSummaryResponse toResponse(
            com.benhsoan.port.dto.result.appointment.DoctorWeeklyTableResult.DoctorSummaryResult result
    ) {
        if (result == null) return null;
        return com.benhsoan.adapter.inbound.rest.response.appointment.DoctorWeeklyTableResponse.DoctorSummaryResponse.builder()
                .id(result.id())
                .fullName(result.fullName())
                .username(result.username())
                .specialtyName(result.specialtyName())
                .build();
    }

    public com.benhsoan.adapter.inbound.rest.response.appointment.DoctorWeeklyTableResponse.DoctorDayScheduleResponse toResponse(
            com.benhsoan.port.dto.result.appointment.DoctorWeeklyTableResult.DoctorDayScheduleResult result
    ) {
        if (result == null) return null;
        return com.benhsoan.adapter.inbound.rest.response.appointment.DoctorWeeklyTableResponse.DoctorDayScheduleResponse.builder()
                .date(result.date())
                .dayOfWeek(result.dayOfWeek())
                .doctorSchedules(result.doctorSchedules() != null
                        ? result.doctorSchedules().stream().map(this::toResponse).toList()
                        : List.of())
                .build();
    }

    public com.benhsoan.adapter.inbound.rest.response.appointment.DoctorWeeklyTableResponse.DoctorScheduleDayResponse toResponse(
            com.benhsoan.port.dto.result.appointment.DoctorWeeklyTableResult.DoctorScheduleDayResult result
    ) {
        if (result == null) return null;
        return com.benhsoan.adapter.inbound.rest.response.appointment.DoctorWeeklyTableResponse.DoctorScheduleDayResponse.builder()
                .doctorId(result.doctorId())
                .doctorName(result.doctorName())
                .workingDay(result.workingDay())
                .workingStartTime(result.workingStartTime())
                .workingEndTime(result.workingEndTime())
                .slots(result.slots() != null
                        ? result.slots().stream().map(this::toResponse).toList()
                        : List.of())
                .timeOffs(result.timeOffs() != null
                        ? result.timeOffs().stream().map(this::toResponse).toList()
                        : List.of())
                .build();
    }

    public com.benhsoan.adapter.inbound.rest.response.appointment.DoctorWeeklyTableResponse.DoctorScheduleSlotResponse toResponse(
            com.benhsoan.port.dto.result.appointment.DoctorWeeklyTableResult.DoctorScheduleSlotResult result
    ) {
        if (result == null) return null;
        return com.benhsoan.adapter.inbound.rest.response.appointment.DoctorWeeklyTableResponse.DoctorScheduleSlotResponse.builder()
                .startTime(result.startTime())
                .endTime(result.endTime())
                .slotStartTime(result.slotStartTime())
                .slotEndTime(result.slotEndTime())
                .status(result.status())
                .isBookable(result.isBookable())
                .appointment(toResponse(result.appointment()))
                .timeOffReason(result.timeOffReason())
                .build();
    }

    public com.benhsoan.adapter.inbound.rest.response.appointment.DoctorWeeklyTableResponse.AppointmentSummaryResponse toResponse(
            com.benhsoan.port.dto.result.appointment.DoctorWeeklyTableResult.AppointmentSummaryResult result
    ) {
        if (result == null) return null;
        return com.benhsoan.adapter.inbound.rest.response.appointment.DoctorWeeklyTableResponse.AppointmentSummaryResponse.builder()
                .id(result.id())
                .appointmentCode(result.appointmentCode())
                .patientId(result.patientId())
                .patientName(result.patientName())
                .patientPhone(result.patientPhone())
                .status(result.status())
                .reason(result.reason())
                .build();
    }

    public com.benhsoan.adapter.inbound.rest.response.appointment.DoctorWeeklyTableResponse.DoctorTimeOffSummaryResponse toResponse(
            com.benhsoan.port.dto.result.appointment.DoctorWeeklyTableResult.DoctorTimeOffSummaryResult result
    ) {
        if (result == null) return null;
        return com.benhsoan.adapter.inbound.rest.response.appointment.DoctorWeeklyTableResponse.DoctorTimeOffSummaryResponse.builder()
                .id(result.id())
                .startTime(result.startTime())
                .endTime(result.endTime())
                .reason(result.reason())
                .build();
    }
}
