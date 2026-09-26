package com.benhsoan.application.ucservice.medicalrecord;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.medicalrecord.MedicalRecordTemplateVersion.SectionDefinition;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.medicalrecord.MedicalRecordTemplateSectionCommand;

@Component
public class MedicalRecordTemplateCommandMapper {

    public List<SectionDefinition> toSectionDefinitions(List<MedicalRecordTemplateSectionCommand> sections) {
        if (sections == null) {
            return List.of();
        }
        return sections.stream().map(section -> {
            if (section == null) {
                throw new ValidationException("Template section is required.");
            }
            return new SectionDefinition(section.fieldCode(), section.label(), section.required(), section.displayOrder());
        }).toList();
    }
}
