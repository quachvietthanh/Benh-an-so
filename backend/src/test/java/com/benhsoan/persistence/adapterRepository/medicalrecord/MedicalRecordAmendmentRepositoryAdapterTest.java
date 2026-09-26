package com.benhsoan.persistence.adapterRepository.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAmendmentEntity;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordAmendmentRepository;
import com.benhsoan.persistence.mapper.medicalrecord.MedicalRecordAmendmentPersistenceMapper;

@ExtendWith(MockitoExtension.class)
class MedicalRecordAmendmentRepositoryAdapterTest {

    @Mock
    private JpaMedicalRecordAmendmentRepository jpaRepository;
    @Spy
    private MedicalRecordAmendmentPersistenceMapper mapper = new MedicalRecordAmendmentPersistenceMapper();
    @InjectMocks
    private MedicalRecordAmendmentRepositoryAdapter adapter;

    @Test
    void returnsAmendmentsForMedicalRecord() {
        UUID recordId = UUID.randomUUID();
        MedicalRecordAmendmentEntity amendment = MedicalRecordAmendmentEntity.builder()
                .id(UUID.randomUUID()).medicalRecordId(recordId).content("Correction")
                .reason("Clarification").amendedBy(UUID.randomUUID())
                .amendedAt(Instant.parse("2026-08-20T02:00:00Z"))
                .build();
        when(jpaRepository.findByMedicalRecordIdOrderByAmendedAtDesc(recordId))
                .thenReturn(List.of(amendment));

        var result = adapter.findByMedicalRecordId(recordId);

        assertEquals(1, result.size());
        assertEquals("Correction", result.getFirst().getContent());
    }
}
