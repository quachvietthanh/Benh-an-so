package com.benhsoan.adapter.inbound.rest.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.ArchiveMedicalRecordRestMapper;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.BatchArchiveMedicalRecordRequest;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.ArchivedMedicalRecordResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.BatchArchiveMedicalRecordResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.EligibleForArchiveMedicalRecordResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.query.SearchArchivedMedicalRecordsQuery;
import com.benhsoan.port.inbound.medicalrecord.BatchArchiveMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetEligibleForArchiveMedicalRecordsUseCase;
import com.benhsoan.port.inbound.medicalrecord.SearchArchivedMedicalRecordsUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/medical-records/archive")
@RequiredArgsConstructor
public class MedicalRecordArchiveController {

    private final GetEligibleForArchiveMedicalRecordsUseCase getEligibleForArchiveMedicalRecordsUseCase;
    private final BatchArchiveMedicalRecordUseCase batchArchiveMedicalRecordUseCase;
    private final SearchArchivedMedicalRecordsUseCase searchArchivedMedicalRecordsUseCase;
    private final ArchiveMedicalRecordRestMapper archiveMapper;

    @GetMapping("/eligible")
    @RequirePermission("MEDICAL_RECORD_ARCHIVE_MANAGE")
    public Page<EligibleForArchiveMedicalRecordResponse> getEligibleForArchive(
            @PageableDefault(size = 20) Pageable pageable) {
        return getEligibleForArchiveMedicalRecordsUseCase.getEligibleRecords(pageable)
                .map(archiveMapper::toEligibleResponse);
    }

    @GetMapping
    @RequirePermission("MEDICAL_RECORD_ARCHIVE_READ")
    public Page<ArchivedMedicalRecordResponse> searchArchivedRecords(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) UUID doctorId,
            @PageableDefault(size = 20) Pageable pageable) {
        var query = new SearchArchivedMedicalRecordsQuery(keyword, fromDate, toDate, doctorId);
        return searchArchivedMedicalRecordsUseCase.search(query, pageable)
                .map(archiveMapper::toArchivedResponse);
    }

    @PostMapping("/batch")
    @RequirePermission("MEDICAL_RECORD_ARCHIVE_MANAGE")
    public BatchArchiveMedicalRecordResponse batchArchive(
            @RequestBody BatchArchiveMedicalRecordRequest request) {
        var result = batchArchiveMedicalRecordUseCase.batchArchive(
                request != null ? request.medicalRecordIds() : null,
                request != null && Boolean.TRUE.equals(request.archiveAllEligible())
        );
        return archiveMapper.toBatchResponse(result);
    }
}
