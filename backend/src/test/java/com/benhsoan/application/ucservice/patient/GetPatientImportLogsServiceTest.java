package com.benhsoan.application.ucservice.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.PatientImportLog;
import com.benhsoan.domain.patient.enums.ImportStatus;
import com.benhsoan.domain.patient.exception.PatientImportLogNotFoundException;
import com.benhsoan.port.dto.result.patient.PatientImportLogResult;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.patient.PatientImportLogRepository;

class GetPatientImportLogsServiceTest {

    private PatientImportLogRepository importLogRepository;
    private UserRepository userRepository;

    private GetPatientImportLogsService service;

    @BeforeEach
    void setUp() {
        importLogRepository = mock(PatientImportLogRepository.class);
        userRepository = mock(UserRepository.class);

        service = new GetPatientImportLogsService(importLogRepository, userRepository);
    }

    @Test
    @DisplayName("Should return paginated import logs with importer names")
    void shouldReturnLogsPage() {
        UUID userId = UUID.randomUUID();
        UUID logId = UUID.randomUUID();

        PatientImportLog log = PatientImportLog.reconstitute(
                logId, "test.xlsx", 1024, 10, 8, 2, 0,
                ImportStatus.PARTIAL, userId, Instant.now(), List.of()
        );

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);
        when(mockUser.getFullName()).thenReturn("Admin User");

        when(importLogRepository.findAll(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(log)));
        when(userRepository.findAllById(List.of(userId)))
                .thenReturn(List.of(mockUser));

        Page<PatientImportLogResult> page = service.getLogs(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).id()).isEqualTo(logId);
        assertThat(page.getContent().get(0).importedByName()).isEqualTo("Admin User");
    }

    @Test
    @DisplayName("Should throw PatientImportLogNotFoundException when log not found")
    void shouldThrowWhenNotFound() {
        UUID missingId = UUID.randomUUID();
        when(importLogRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLogById(missingId))
                .isInstanceOf(PatientImportLogNotFoundException.class);
    }
}
