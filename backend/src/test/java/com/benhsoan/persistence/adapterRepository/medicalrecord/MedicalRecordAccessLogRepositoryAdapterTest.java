package com.benhsoan.persistence.adapterRepository.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.medicalrecord.MedicalRecordAccessLog;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAccessLogEntity;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordAccessLogRepository;
import com.benhsoan.persistence.mapper.medicalrecord.MedicalRecordAccessLogPersistenceMapper;

@ExtendWith(MockitoExtension.class)
class MedicalRecordAccessLogRepositoryAdapterTest {

    @Mock
    private JpaMedicalRecordAccessLogRepository jpaRepository;

    @Spy
    private MedicalRecordAccessLogPersistenceMapper mapper = new MedicalRecordAccessLogPersistenceMapper();

    @InjectMocks
    private MedicalRecordAccessLogRepositoryAdapter adapter;

    @Test
    void persistsHistoryAuditWithoutIpAddress() {
        MedicalRecordAccessLog accessLog = MedicalRecordAccessLog.createHistoryView(
                UUID.randomUUID(), UUID.randomUUID(), "History viewed",
                java.time.Instant.parse("2026-08-20T02:00:00Z")
        );
        when(jpaRepository.save(any(MedicalRecordAccessLogEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MedicalRecordAccessLog saved = adapter.save(accessLog);

        ArgumentCaptor<MedicalRecordAccessLogEntity> entityCaptor = ArgumentCaptor.forClass(MedicalRecordAccessLogEntity.class);
        verify(jpaRepository).save(entityCaptor.capture());
        assertNull(entityCaptor.getValue().getIpAddress());
        assertNull(saved.getIpAddress());
        assertSame(accessLog.getAction(), saved.getAction());
    }

    @Test
    void mapsAccessLogsFilteredByPatient() {
        UUID patientId = UUID.randomUUID();
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-31T23:59:59Z");
        PageRequest pageable = PageRequest.of(0, 20);
        MedicalRecordAccessLogEntity entity = MedicalRecordAccessLogEntity.builder()
                .id(UUID.randomUUID()).patientId(patientId).accessedBy(UUID.randomUUID())
                .action(MedicalRecordAccessAction.VIEW_HISTORY).accessedAt(from.plusSeconds(1))
                .build();
        when(jpaRepository.findByPatientId(patientId, from, to, pageable))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findByPatientId(patientId, from, to, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(MedicalRecordAccessAction.VIEW_HISTORY, result.getContent().getFirst().getAction());
    }
}
