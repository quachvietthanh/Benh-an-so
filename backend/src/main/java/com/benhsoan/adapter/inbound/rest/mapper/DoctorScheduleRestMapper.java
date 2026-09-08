package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.appointment.DoctorWeeklyScheduleItemRequest;
import com.benhsoan.adapter.inbound.rest.request.appointment.RegisterDoctorTimeOffRequest;
import com.benhsoan.adapter.inbound.rest.request.appointment.SetDoctorWeeklyScheduleRequest;
import com.benhsoan.adapter.inbound.rest.response.appointment.AffectedAppointmentResponse;
import com.benhsoan.adapter.inbound.rest.response.appointment.DoctorTimeOffResponse;
import com.benhsoan.adapter.inbound.rest.response.appointment.DoctorWeeklyScheduleItemResponse;
import com.benhsoan.adapter.inbound.rest.response.appointment.DoctorWeeklyScheduleResponse;
import com.benhsoan.port.dto.command.appointment.DoctorWeeklyScheduleItemCommand;
import com.benhsoan.port.dto.command.appointment.RegisterDoctorTimeOffCommand;
import com.benhsoan.port.dto.command.appointment.SetDoctorWeeklyScheduleCommand;
import com.benhsoan.port.dto.result.appointment.AffectedAppointmentResult;
import com.benhsoan.port.dto.result.appointment.DoctorTimeOffResult;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyScheduleItemResult;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyScheduleResult;

@Component
public class DoctorScheduleRestMapper {

    public SetDoctorWeeklyScheduleCommand toCommand(SetDoctorWeeklyScheduleRequest request) {
        if (request == null) {
            return null;
        }
        List<DoctorWeeklyScheduleItemCommand> items = request.items() != null
                ? request.items().stream()
                .map(this::toItemCommand)
                .toList()
                : List.of();
        return new SetDoctorWeeklyScheduleCommand(request.doctorId(), items);
    }

    public DoctorWeeklyScheduleItemCommand toItemCommand(DoctorWeeklyScheduleItemRequest request) {
        return new DoctorWeeklyScheduleItemCommand(
                request.dayOfWeek(),
                request.startTime(),
                request.endTime(),
                request.active() == null || request.active()
        );
    }

    public DoctorWeeklyScheduleResponse toResponse(DoctorWeeklyScheduleResult result) {
        if (result == null) {
            return null;
        }
        List<DoctorWeeklyScheduleItemResponse> items = result.items() != null
                ? result.items().stream()
                .map(this::toItemResponse)
                .toList()
                : List.of();
        return new DoctorWeeklyScheduleResponse(result.doctorId(), items);
    }

    public DoctorWeeklyScheduleItemResponse toItemResponse(DoctorWeeklyScheduleItemResult result) {
        return new DoctorWeeklyScheduleItemResponse(
                result.id(),
                result.doctorId(),
                result.dayOfWeek(),
                result.startTime(),
                result.endTime(),
                result.active()
        );
    }

    public RegisterDoctorTimeOffCommand toCommand(RegisterDoctorTimeOffRequest request) {
        if (request == null) {
            return null;
        }
        return new RegisterDoctorTimeOffCommand(
                request.doctorId(),
                request.startTime(),
                request.endTime(),
                request.reason()
        );
    }

    public DoctorTimeOffResponse toResponse(DoctorTimeOffResult result) {
        if (result == null) {
            return null;
        }
        List<AffectedAppointmentResponse> affectedList = result.affectedAppointments() != null
                ? result.affectedAppointments().stream()
                .map(this::toResponse)
                .toList()
                : List.of();

        return new DoctorTimeOffResponse(
                result.id(),
                result.doctorId(),
                result.startTime(),
                result.endTime(),
                result.reason(),
                result.status(),
                result.createdBy(),
                result.createdAt(),
                result.updatedAt(),
                affectedList
        );
    }

    public AffectedAppointmentResponse toResponse(AffectedAppointmentResult result) {
        if (result == null) {
            return null;
        }
        return new AffectedAppointmentResponse(
                result.id(),
                result.appointmentCode(),
                result.patientId(),
                result.patientFullName(),
                result.patientPhone(),
                result.startTime(),
                result.endTime(),
                result.status(),
                result.reason()
        );
    }
}
