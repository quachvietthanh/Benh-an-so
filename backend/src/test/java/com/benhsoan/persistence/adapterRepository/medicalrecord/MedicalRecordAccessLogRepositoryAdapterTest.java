package com.benhsoan.persistence.adapterRepository.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.medicalrecord.MedicalRecordAccessLog;
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
                UUID.randomUUID(), UUID.randomUUID(), "History viewed", "127.0.0.1",
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
}
