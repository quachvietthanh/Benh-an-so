package com.benhsoan.port.inbound.medicalrecord;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.command.medicalrecord.SearchMedicalRecordTemplateQuery;
import com.benhsoan.port.dto.result.MedicalRecordTemplateResult;

public interface MedicalRecordTemplateQueryUseCase {

    List<MedicalRecordTemplateResult> search(SearchMedicalRecordTemplateQuery query);

    MedicalRecordTemplateResult getById(UUID templateId);
}
