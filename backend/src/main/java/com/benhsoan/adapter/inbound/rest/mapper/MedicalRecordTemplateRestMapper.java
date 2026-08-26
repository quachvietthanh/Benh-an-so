package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.medicalrecord.CreateMedicalRecordTemplateRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.MedicalRecordTemplateSectionRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.UpdateMedicalRecordTemplateRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.UpdateMedicalRecordTemplateStatusRequest;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordTemplateResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordTemplateSectionResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordTemplateSummaryResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.SpecialtyResponse;
import com.benhsoan.port.dto.command.medicalrecord.CreateMedicalRecordTemplateCommand;
import com.benhsoan.port.dto.command.medicalrecord.MedicalRecordTemplateSectionCommand;
import com.benhsoan.port.dto.command.medicalrecord.UpdateMedicalRecordTemplateCommand;
import com.benhsoan.port.dto.command.medicalrecord.UpdateMedicalRecordTemplateStatusCommand;
import com.benhsoan.port.dto.result.MedicalRecordTemplateResult;
import com.benhsoan.port.dto.result.SpecialtyResult;

@Component
public class MedicalRecordTemplateRestMapper {

    public CreateMedicalRecordTemplateCommand toCommand(CreateMedicalRecordTemplateRequest request) {
        return new CreateMedicalRecordTemplateCommand(request.specialtyId(), request.name(), Boolean.TRUE.equals(request.makeDefault()),
                toSectionCommands(request.sections()));
    }

    public UpdateMedicalRecordTemplateCommand toCommand(UUID templateId, UpdateMedicalRecordTemplateRequest request) {
        return new UpdateMedicalRecordTemplateCommand(templateId, request.name(), toSectionCommands(request.sections()),
                request.changeNote());
    }

    public UpdateMedicalRecordTemplateStatusCommand toCommand(UUID templateId,
            UpdateMedicalRecordTemplateStatusRequest request) {
        return new UpdateMedicalRecordTemplateStatusCommand(templateId, request.active(), request.replacementTemplateId());
    }

    public MedicalRecordTemplateResponse toResponse(MedicalRecordTemplateResult result) {
        return new MedicalRecordTemplateResponse(result.id(), toResponse(result.specialty()), result.name(), result.active(),
                result.defaultTemplate(), result.currentVersionNo(), result.sections().stream()
                        .map(section -> new MedicalRecordTemplateSectionResponse(section.fieldCode(), section.label(),
                                section.required(), section.displayOrder()))
                        .toList(),
                result.createdAt(), result.updatedAt());
    }

    public MedicalRecordTemplateSummaryResponse toSummaryResponse(MedicalRecordTemplateResult result) {
        return new MedicalRecordTemplateSummaryResponse(result.id(), toResponse(result.specialty()), result.name(),
                result.active(), result.defaultTemplate(), result.currentVersionNo(), result.createdAt(), result.updatedAt());
    }

    public SpecialtyResponse toResponse(SpecialtyResult result) {
        return new SpecialtyResponse(result.id(), result.code(), result.name(), result.active());
    }

    private List<MedicalRecordTemplateSectionCommand> toSectionCommands(
            List<MedicalRecordTemplateSectionRequest> sections) {
        return sections.stream().map(section -> new MedicalRecordTemplateSectionCommand(section.fieldCode(), section.label(),
                section.required(), section.displayOrder())).toList();
    }
}
