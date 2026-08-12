package com.benhsoan.persistence.adapterRepository.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.benhsoan.domain.medicalrecord.MedicalRecordAccessLog;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAccessLogEntity;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordAccessLogRepository;
import com.benhsoan.persistence.mapper.medicalrecord.MedicalRecordAccessLogPersistenceMapper;
import com.benhsoan.port.dto.command.medicalrecord.GetMedicalRecordAccessLogsQuery;

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
    void mapsAccessLogsWithDynamicSearchFilters() {
        UUID accessedBy = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID medicalRecordId = UUID.randomUUID();
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-31T23:59:59Z");
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("accessedAt"), Sort.Order.desc("id")));
        MedicalRecordAccessLogEntity entity = MedicalRecordAccessLogEntity.builder()
                .id(UUID.randomUUID()).patientId(patientId).medicalRecordId(medicalRecordId).accessedBy(accessedBy)
                .action(MedicalRecordAccessAction.VIEW_HISTORY).accessedAt(from.plusSeconds(1))
                .build();
        GetMedicalRecordAccessLogsQuery query = new GetMedicalRecordAccessLogsQuery(
                accessedBy, patientId, medicalRecordId, null, from, to, 0, 20
        );
        when(jpaRepository.findAll(
                argThat((Specification<MedicalRecordAccessLogEntity> specification) -> specification != null),
                any(PageRequest.class)
        ))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.search(query, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(MedicalRecordAccessAction.VIEW_HISTORY, result.getContent().getFirst().getAction());
        verify(jpaRepository).findAll(
                argThat((Specification<MedicalRecordAccessLogEntity> specification) -> specification != null),
                any(PageRequest.class)
        );
    }
}
