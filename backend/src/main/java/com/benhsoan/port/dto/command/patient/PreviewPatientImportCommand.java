package com.benhsoan.port.dto.command.patient;

public record PreviewPatientImportCommand(
        byte[] fileContent,
        String fileName,
        long fileSize
) {}
