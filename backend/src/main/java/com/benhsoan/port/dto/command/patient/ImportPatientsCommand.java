package com.benhsoan.port.dto.command.patient;

public record ImportPatientsCommand(
        byte[] fileContent,
        String fileName,
        long fileSize,
        boolean skipDuplicates
) {}
