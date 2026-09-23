package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.appointment.AddToWaitlistRequest;
import com.benhsoan.adapter.inbound.rest.request.appointment.CancelWaitlistRequest;
import com.benhsoan.adapter.inbound.rest.response.appointment.AppointmentWaitlistResponse;
import com.benhsoan.adapter.inbound.rest.response.appointment.WaitlistSuggestionResponse;
import com.benhsoan.port.dto.command.appointment.AddToWaitlistCommand;
import com.benhsoan.port.dto.command.appointment.CancelWaitlistEntryCommand;
import com.benhsoan.port.dto.result.appointment.AppointmentWaitlistResult;
import com.benhsoan.port.dto.result.appointment.WaitlistSuggestionResult;

@Component
public class AppointmentWaitlistRestMapper {

    public AddToWaitlistCommand toCommand(AddToWaitlistRequest request) {
        if (request == null) {
            return null;
        }

        return AddToWaitlistCommand.builder()
                .patientId(request.patientId())
                .doctorId(request.doctorId())
                .desiredDate(request.desiredDate())
                .timePreference(request.timePreference())
                .note(request.note())
                .build();
    }

    public CancelWaitlistEntryCommand toCommand(UUID id, CancelWaitlistRequest request) {
        if (request == null) {
            return null;
        }

        return CancelWaitlistEntryCommand.builder()
                .id(id)
                .reason(request.reason())
                .build();
    }

    public AppointmentWaitlistResponse toResponse(AppointmentWaitlistResult result) {
        if (result == null) {
            return null;
        }

        return AppointmentWaitlistResponse.builder()
                .id(result.id())
                .patientId(result.patientId())
                .patientName(result.patientName())
                .patientPhone(result.patientPhone())
                .doctorId(result.doctorId())
                .doctorName(result.doctorName())
                .desiredDate(result.desiredDate())
                .timePreference(result.timePreference())
                .status(result.status())
                .note(result.note())
                .cancelReason(result.cancelReason())
                .bookedAppointmentId(result.bookedAppointmentId())
                .createdBy(result.createdBy())
                .createdByName(result.createdByName())
                .createdAt(result.createdAt())
                .updatedAt(result.updatedAt())
                .build();
    }

    public List<AppointmentWaitlistResponse> toResponse(List<AppointmentWaitlistResult> results) {
        if (results == null) {
            return List.of();
        }

        return results.stream()
                .map(this::toResponse)
                .toList();
    }

    public WaitlistSuggestionResponse toResponse(WaitlistSuggestionResult result) {
        if (result == null) {
            return null;
        }

        return WaitlistSuggestionResponse.builder()
                .waitlistId(result.waitlistId())
                .patientId(result.patientId())
                .patientName(result.patientName())
                .patientPhone(result.patientPhone())
                .doctorId(result.doctorId())
                .doctorName(result.doctorName())
                .desiredDate(result.desiredDate())
                .timePreference(result.timePreference())
                .note(result.note())
                .createdAt(result.createdAt())
                .build();
    }
}
