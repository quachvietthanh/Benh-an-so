package com.benhsoan.application.ucservice.backup;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.backup.BackupRecord;
import com.benhsoan.port.outbound.repository.backup.BackupRecordRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BackupCodeGenerator {

    private static final String PREFIX = "BKP";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final BackupRecordRepository backupRecordRepository;
    private final ClockPort clockPort;

    public String generate() {
        String datePart = LocalDate.ofInstant(clockPort.now(), ZoneOffset.UTC).format(DATE_FORMATTER);
        String prefix = PREFIX + "-" + datePart + "-";

        return backupRecordRepository.findTopByOrderByBackupCodeDesc()
                .map(BackupRecord::getBackupCode)
                .filter(code -> code.startsWith(prefix))
                .map(code -> {
                    int number = Integer.parseInt(code.substring(prefix.length()));
                    return prefix + String.format("%04d", number + 1);
                })
                .orElse(prefix + "0001");
    }
}
