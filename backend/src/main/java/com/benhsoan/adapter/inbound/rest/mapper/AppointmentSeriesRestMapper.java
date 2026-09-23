package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.appointment.CreateAppointmentSeriesRequest;
import com.benhsoan.adapter.inbound.rest.request.appointment.PreviewAppointmentSeriesRequest;
import com.benhsoan.adapter.inbound.rest.response.appointment.AppointmentResponse;
import com.benhsoan.adapter.inbound.rest.response.appointment.AppointmentSeriesPreviewResponse;
import com.benhsoan.adapter.inbound.rest.response.appointment.AppointmentSeriesResponse;
import com.benhsoan.adapter.inbound.rest.response.appointment.AppointmentSeriesSessionPreviewResponse;
import com.benhsoan.port.dto.command.appointment.CreateAppointmentSeriesCommand;
import com.benhsoan.port.dto.command.appointment.PreviewAppointmentSeriesCommand;
import com.benhsoan.port.dto.result.appointment.AppointmentSeriesPreviewResult;
import com.benhsoan.port.dto.result.appointment.AppointmentSeriesResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppointmentSeriesRestMapper {

    private final AppointmentRestMapper appointmentRestMapper;

    public PreviewAppointmentSeriesCommand toCommand(PreviewAppointmentSeriesRequest request) {
        return PreviewAppointmentSeriesCommand.builder()
                .patientId(request.patientId())
                .doctorId(request.doctorId())
                .firstSessionStartTime(request.firstSessionStartTime())
                .sessionDurationMinutes(request.sessionDurationMinutes())
                .totalSessions(request.totalSessions())
                .intervalDays(request.intervalDays())
                .build();
    }

    public AppointmentSeriesPreviewResponse toResponse(AppointmentSeriesPreviewResult result) {
        if (result == null) {
            return null;
        }
        List<AppointmentSeriesSessionPreviewResponse> sessionResponses = result.sessions() != null
                ? result.sessions().stream()
                        .map(s -> AppointmentSeriesSessionPreviewResponse.builder()
                                .sequenceNumber(s.sequenceNumber())
                                .startTime(s.startTime())
                                .endTime(s.endTime())
                                .status(s.status())
                                .conflictReason(s.conflictReason())
                                .build())
                        .toList()
                : List.of();

        return AppointmentSeriesPreviewResponse.builder()
                .totalSessions(result.totalSessions())
                .intervalDays(result.intervalDays())
                .allAvailable(result.allAvailable())
                .conflictCount(result.conflictCount())
                .sessions(sessionResponses)
                .build();
    }

    public CreateAppointmentSeriesCommand toCommand(CreateAppointmentSeriesRequest request) {
        List<CreateAppointmentSeriesCommand.AppointmentSeriesSessionCommand> sessionCommands = request.sessions() != null
                ? request.sessions().stream()
                        .map(s -> new CreateAppointmentSeriesCommand.AppointmentSeriesSessionCommand(
                                s.sequenceNumber(),
                                s.startTime(),
                                s.endTime()
                        ))
                        .toList()
                : List.of();

        return CreateAppointmentSeriesCommand.builder()
                .patientId(request.patientId())
                .doctorId(request.doctorId())
                .medicalRecordId(request.medicalRecordId())
                .title(request.title())
                .notes(request.notes())
                .totalSessions(request.totalSessions())
                .intervalDays(request.intervalDays())
                .sessions(sessionCommands)
                .build();
    }

    public AppointmentSeriesResponse toResponse(AppointmentSeriesResult result) {
        if (result == null) {
            return null;
        }
        List<AppointmentResponse> appointmentResponses = result.appointments() != null
                ? result.appointments().stream()
                        .map(appointmentRestMapper::toResponse)
                        .toList()
                : List.of();

        return AppointmentSeriesResponse.builder()
                .id(result.id())
                .seriesCode(result.seriesCode())
                .patientId(result.patientId())
                .doctorId(result.doctorId())
                .medicalRecordId(result.medicalRecordId())
                .totalSessions(result.totalSessions())
                .intervalDays(result.intervalDays())
                .title(result.title())
                .notes(result.notes())
                .status(result.status())
                .createdBy(result.createdBy())
                .createdAt(result.createdAt())
                .updatedAt(result.updatedAt())
                .appointments(appointmentResponses)
                .build();
    }

    public List<AppointmentSeriesResponse> toResponseList(List<AppointmentSeriesResult> results) {
        if (results == null) {
            return List.of();
        }
        return results.stream()
                .map(this::toResponse)
                .toList();
    }
}
