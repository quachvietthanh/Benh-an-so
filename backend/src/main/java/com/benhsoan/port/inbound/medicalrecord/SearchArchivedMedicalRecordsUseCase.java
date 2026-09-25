package com.benhsoan.port.inbound.medicalrecord;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.port.dto.query.SearchArchivedMedicalRecordsQuery;
import com.benhsoan.port.dto.result.ArchivedMedicalRecordResult;

public interface SearchArchivedMedicalRecordsUseCase {

    Page<ArchivedMedicalRecordResult> search(SearchArchivedMedicalRecordsQuery query, Pageable pageable);
}
