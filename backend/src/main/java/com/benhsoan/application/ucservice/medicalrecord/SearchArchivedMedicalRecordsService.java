package com.benhsoan.application.ucservice.medicalrecord;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.query.SearchArchivedMedicalRecordsQuery;
import com.benhsoan.port.dto.result.ArchivedMedicalRecordResult;
import com.benhsoan.port.inbound.medicalrecord.SearchArchivedMedicalRecordsUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.ArchiveMedicalRecordQueryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchArchivedMedicalRecordsService implements SearchArchivedMedicalRecordsUseCase {

    private final ArchiveMedicalRecordQueryRepository archiveQueryRepository;
    private final MedicalRecordAuthorizationService authorizationService;

    @Override
    public Page<ArchivedMedicalRecordResult> search(SearchArchivedMedicalRecordsQuery query, Pageable pageable) {
        authorizationService.requireArchiveReadAccess();

        String keyword = query != null ? query.keyword() : null;
        var fromDate = query != null ? query.fromDate() : null;
        var toDate = query != null ? query.toDate() : null;
        var doctorId = query != null ? query.doctorId() : null;

        return archiveQueryRepository.searchArchivedRecords(keyword, fromDate, toDate, doctorId, pageable);
    }
}
