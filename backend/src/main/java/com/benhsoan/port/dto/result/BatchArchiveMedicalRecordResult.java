package com.benhsoan.port.dto.result;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchArchiveMedicalRecordResult {
    private int totalRequested;
    private int archivedCount;
    private int skippedCount;
    private List<UUID> archivedMedicalRecordIds;
    private List<UUID> skippedMedicalRecordIds;
}
