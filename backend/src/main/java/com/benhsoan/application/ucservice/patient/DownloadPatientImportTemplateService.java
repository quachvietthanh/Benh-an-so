package com.benhsoan.application.ucservice.patient;

import org.springframework.stereotype.Service;

import com.benhsoan.port.inbound.patient.DownloadPatientImportTemplateUseCase;
import com.benhsoan.port.outbound.spreadsheet.PatientSpreadsheetTemplatePort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DownloadPatientImportTemplateService implements DownloadPatientImportTemplateUseCase {

    private final PatientSpreadsheetTemplatePort templateGenerator;

    @Override
    public byte[] downloadTemplate() {
        return templateGenerator.generateTemplate();
    }
}
