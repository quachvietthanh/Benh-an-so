package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordAccessLog;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.port.dto.command.medicalrecord.GetMedicalRecordAccessLogsQuery;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordAccessLogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;

@ExtendWith(MockitoExtension.class)
class GetMedicalRecordAccessLogsServiceTest {

    @Mock
    private MedicalRecordAccessLogRepository accessLogRepository;
    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    @Mock
    private MedicalRecordAuthorizationService authorizationService;
    @Spy
    private MedicalRecordResultMapper resultMapper = new MedicalRecordResultMapper();

    @InjectMocks
    private GetMedicalRecordAccessLogsService service;

    @Test
    @DisplayName("searches using multi-filter query with fixed sorting")
    void searchesUsingDynamicFilters() {
        UUID actorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID medicalRecordId = UUID.randomUUID();
        GetMedicalRecordAccessLogsQuery query = new GetMedicalRecordAccessLogsQuery(
                actorId,
                patientId,
                medicalRecordId,
                null,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-12T23:59:59Z"),
                0,
                20
        );
        MedicalRecordAccessLog log = MedicalRecordAccessLog.createRecordAccess(
                patientId,
                UUID.randomUUID(),
                medicalRecordId,
                actorId,
                MedicalRecordAccessAction.VIEW,
                "Medical record viewed",
                Instant.parse("2026-08-10T10:00:00Z")
        );
        when(medicalRecordRepository.findById(medicalRecordId)).thenReturn(Optional.of(org.mockito.Mockito.mock(MedicalRecord.class)));
        when(accessLogRepository.search(any(GetMedicalRecordAccessLogsQuery.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        var result = service.getAccessLogs(query);

        assertEquals(1, result.getTotalElements());
        verify(authorizationService).requireAuditReadAccess();
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(accessLogRepository).search(any(GetMedicalRecordAccessLogsQuery.class), pageableCaptor.capture());
        assertEquals("accessedAt: DESC,id: DESC", pageableCaptor.getValue().getSort().toString());
    }

    @Test
    @DisplayName("throws when filtering by unknown medical record")
    void throwsWhenMedicalRecordDoesNotExist() {
        UUID medicalRecordId = UUID.randomUUID();
        GetMedicalRecordAccessLogsQuery query = new GetMedicalRecordAccessLogsQuery(
                null, null, medicalRecordId, null, null, null, 0, 20
        );
        when(medicalRecordRepository.findById(medicalRecordId)).thenReturn(Optional.empty());

        assertThrows(MedicalRecordNotFoundException.class, () -> service.getAccessLogs(query));
    }

    @Test
    @DisplayName("passes through filter-only query without medical record existence lookup")
    void passesThroughFilterOnlyQuery() {
        UUID actorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        GetMedicalRecordAccessLogsQuery query = new GetMedicalRecordAccessLogsQuery(
                actorId,
                patientId,
                null,
                visitId,
                Instant.parse("2026-08-05T00:00:00Z"),
                Instant.parse("2026-08-12T00:00:00Z"),
                1,
                10
        );
        when(accessLogRepository.search(eq(query), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var result = service.getAccessLogs(query);

        assertEquals(0, result.getTotalElements());
        verify(accessLogRepository).search(eq(query), any(Pageable.class));
        verifyNoInteractions(medicalRecordRepository);
    }

    @Test
    @DisplayName("stops before repository search when audit permission is denied")
    void stopsWhenAuditPermissionDenied() {
        GetMedicalRecordAccessLogsQuery query = new GetMedicalRecordAccessLogsQuery(
                null, null, null, null, null, null, 0, 20
        );
        when(authorizationService.requireAuditReadAccess())
                .thenThrow(new com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException());

        assertThrows(com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException.class,
                () -> service.getAccessLogs(query));

        verifyNoInteractions(accessLogRepository, medicalRecordRepository);
    }
}
