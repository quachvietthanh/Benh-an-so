package com.benhsoan.application.ucservice.patient;

import org.springframework.stereotype.Service;

import com.benhsoan.infrastructure.spreadsheet.ExcelPatientTemplateGenerator;
import com.benhsoan.port.inbound.patient.DownloadPatientImportTemplateUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DownloadPatientImportTemplateService implements DownloadPatientImportTemplateUseCase {

    private final ExcelPatientTemplateGenerator templateGenerator;

    @Override
    public byte[] downloadTemplate() {
        return templateGenerator.generateTemplate();
    }
}
