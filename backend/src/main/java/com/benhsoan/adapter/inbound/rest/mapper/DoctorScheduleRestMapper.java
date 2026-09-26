package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.appointment.ConfigureDoctorWeeklyScheduleRequest;
import com.benhsoan.adapter.inbound.rest.request.appointment.RegisterDoctorTimeOffRequest;
import com.benhsoan.adapter.inbound.rest.response.appointment.AffectedAppointmentResponse;
import com.benhsoan.adapter.inbound.rest.response.appointment.DoctorTimeOffResponse;
import com.benhsoan.adapter.inbound.rest.response.appointment.DoctorWeeklyScheduleResponse;
import com.benhsoan.port.dto.command.appointment.ConfigureDoctorWeeklyScheduleCommand;
import com.benhsoan.port.dto.command.appointment.RegisterDoctorTimeOffCommand;
import com.benhsoan.port.dto.command.appointment.WeeklyScheduleItem;
import com.benhsoan.port.dto.result.appointment.AffectedAppointmentResult;
import com.benhsoan.port.dto.result.appointment.DoctorTimeOffResult;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyScheduleResult;

@Component
public class DoctorScheduleRestMapper {

    public ConfigureDoctorWeeklyScheduleCommand toCommand(UUID doctorId, ConfigureDoctorWeeklyScheduleRequest request) {
        if (request == null || request.schedules() == null) {
            return new ConfigureDoctorWeeklyScheduleCommand(doctorId, List.of());
        }
        List<WeeklyScheduleItem> items = request.schedules().stream()
                .map(req -> new WeeklyScheduleItem(
                        req.dayOfWeek(),
                        req.startTime(),
                        req.endTime(),
                        req.isActive()
                ))
                .toList();
        return new ConfigureDoctorWeeklyScheduleCommand(doctorId, items);
    }

    public RegisterDoctorTimeOffCommand toCommand(UUID doctorId, RegisterDoctorTimeOffRequest request) {
        return new RegisterDoctorTimeOffCommand(
                doctorId,
                request.startTime(),
                request.endTime(),
                request.reason()
        );
    }

    public DoctorWeeklyScheduleResponse toResponse(DoctorWeeklyScheduleResult result) {
        if (result == null) {
            return null;
        }
        return new DoctorWeeklyScheduleResponse(
                result.id(),
                result.doctorId(),
                result.dayOfWeek(),
                result.startTime(),
                result.endTime(),
                result.active()
        );
    }

    public List<DoctorWeeklyScheduleResponse> toWeeklyResponseList(List<DoctorWeeklyScheduleResult> results) {
        if (results == null) {
            return List.of();
        }
        return results.stream().map(this::toResponse).toList();
    }

    public AffectedAppointmentResponse toResponse(AffectedAppointmentResult result) {
        if (result == null) {
            return null;
        }
        return new AffectedAppointmentResponse(
                result.id(),
                result.appointmentCode(),
                result.patientId(),
                result.startTime(),
                result.endTime(),
                result.status(),
                result.reason()
        );
    }

    public DoctorTimeOffResponse toResponse(DoctorTimeOffResult result) {
        if (result == null) {
            return null;
        }
        List<AffectedAppointmentResponse> affected = result.affectedAppointments() == null
                ? List.of()
                : result.affectedAppointments().stream().map(this::toResponse).toList();

        return new DoctorTimeOffResponse(
                result.id(),
                result.doctorId(),
                result.startTime(),
                result.endTime(),
                result.reason(),
                result.status(),
                result.createdBy(),
                result.createdAt(),
                affected
        );
    }

    public List<DoctorTimeOffResponse> toTimeOffResponseList(List<DoctorTimeOffResult> results) {
        if (results == null) {
            return List.of();
        }
        return results.stream().map(this::toResponse).toList();
    }
}
