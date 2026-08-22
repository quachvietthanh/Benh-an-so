package com.benhsoan.persistence.adapterRepository.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordEntity;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordRepository;
import com.benhsoan.persistence.mapper.medicalrecord.MedicalRecordPersistenceMapper;

@ExtendWith(MockitoExtension.class)
class MedicalRecordRepositoryAdapterTest {

    @Mock
    private JpaMedicalRecordRepository jpaRepository;
    @Mock
    private MedicalRecordCascadeDeleter cascadeDeleter;
    @Spy
    private MedicalRecordPersistenceMapper mapper = new MedicalRecordPersistenceMapper();
    @InjectMocks
    private MedicalRecordRepositoryAdapter adapter;

    @Test
    void findsMedicalRecordByVisitId() {
        UUID visitId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        when(jpaRepository.findByVisitId(visitId)).thenReturn(Optional.of(MedicalRecordEntity.builder()
                .id(recordId).visitId(visitId).status(MedicalRecordStatus.DRAFT)
                .createdBy(UUID.randomUUID()).createdAt(Instant.parse("2026-08-20T02:00:00Z"))
                .build()));

        var result = adapter.findByVisitId(visitId);

        assertEquals(recordId, result.orElseThrow().getId());
        assertEquals(visitId, result.orElseThrow().getVisitId());
    }

    @Test
    void findsMedicalRecordForUpdate() {
        UUID recordId = UUID.randomUUID();
        when(jpaRepository.findByIdForUpdate(recordId)).thenReturn(Optional.of(MedicalRecordEntity.builder()
                .id(recordId).visitId(UUID.randomUUID()).status(MedicalRecordStatus.DRAFT)
                .createdBy(UUID.randomUUID()).createdAt(Instant.parse("2026-08-20T02:00:00Z"))
                .build()));

        var result = adapter.findByIdForUpdate(recordId);

        assertEquals(recordId, result.orElseThrow().getId());
        verify(jpaRepository).findByIdForUpdate(recordId);
    }
}
