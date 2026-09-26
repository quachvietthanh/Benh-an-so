package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.specialty.CreateSpecialtyRequest;
import com.benhsoan.adapter.inbound.rest.request.specialty.UpdateSpecialtyRequest;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.SpecialtyResponse;
import com.benhsoan.adapter.inbound.rest.response.queue.RoomResponse;
import com.benhsoan.adapter.inbound.rest.response.specialty.SpecialtyDetailResponse;
import com.benhsoan.port.dto.command.specialty.CreateSpecialtyCommand;
import com.benhsoan.port.dto.command.specialty.UpdateSpecialtyCommand;
import com.benhsoan.port.dto.result.SpecialtyResult;
import com.benhsoan.port.dto.result.specialty.SpecialtyDetailResult;

import java.util.UUID;

@Component
public class SpecialtyRestMapper {

    public CreateSpecialtyCommand toCommand(CreateSpecialtyRequest request) {
        if (request == null) {
            return null;
        }
        return CreateSpecialtyCommand.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .doctorIds(request.doctorIds())
                .roomIds(request.roomIds())
                .build();
    }

    public UpdateSpecialtyCommand toCommand(UUID id, UpdateSpecialtyRequest request) {
        if (request == null) {
            return null;
        }
        return UpdateSpecialtyCommand.builder()
                .id(id)
                .name(request.name())
                .description(request.description())
                .doctorIds(request.doctorIds())
                .roomIds(request.roomIds())
                .build();
    }

    public SpecialtyResponse toResponse(SpecialtyResult result) {
        if (result == null) {
            return null;
        }
        return new SpecialtyResponse(
                result.id(),
                result.code(),
                result.name(),
                result.description(),
                result.active()
        );
    }

    public SpecialtyDetailResponse toResponse(SpecialtyDetailResult result) {
        if (result == null) {
            return null;
        }
        return SpecialtyDetailResponse.builder()
                .id(result.id())
                .code(result.code())
                .name(result.name())
                .description(result.description())
                .active(result.active())
                .doctors(result.doctors() == null ? java.util.List.of() : result.doctors().stream()
                        .map(d -> SpecialtyDetailResponse.AssignedDoctorResponse.builder()
                                .id(d.id())
                                .username(d.username())
                                .fullName(d.fullName())
                                .email(d.email())
                                .phone(d.phone())
                                .build())
                        .toList())
                .rooms(result.rooms() == null ? java.util.List.of() : result.rooms().stream()
                        .map(r -> new RoomResponse(r.id(), r.code(), r.name(), r.active(), r.createdAt(), r.updatedAt()))
                        .toList())
                .activeTemplateCount(result.activeTemplateCount())
                .createdAt(result.createdAt())
                .updatedAt(result.updatedAt())
                .build();
    }
}
