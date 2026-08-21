package com.benhsoan.application.ucservice.clinic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.clinic.UpdateClinicConfigurationCommand;
import com.benhsoan.port.dto.result.clinic.ClinicConfigurationResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class UpdateClinicConfigurationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-21T00:00:00Z");
    private static final UUID ACTOR = UUID.randomUUID();

    @Mock
    private ClinicConfigurationRepository clinicConfigurationRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private CurrentUserPort currentUserPort;
    @Mock
    private ClockPort clockPort;

    private UpdateClinicConfigurationService service() {
        return new UpdateClinicConfigurationService(
                clinicConfigurationRepository,
                auditLogRepository,
                currentUserPort,
                clockPort,
                new ClinicConfigurationResultMapper()
        );
    }

    private static UpdateClinicConfigurationCommand command(Integer retentionYears) {
        return new UpdateClinicConfigurationCommand(
                "Phong kham Benh So An",
                "Thai Nguyen",
                "0345678910",
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                retentionYears
        );
    }

    private static ClinicConfiguration existing(int retentionYears) {
        return ClinicConfiguration.create(
                "Phong kham Benh So An",
                "Thai Nguyen",
                "0345678910",
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                retentionYears,
                NOW
        );
    }

    @Test
    void persistsRetentionAndWritesAuditWithBeforeAfterValues() {
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR);
        when(clinicConfigurationRepository.find()).thenReturn(Optional.of(existing(10)));
        when(clinicConfigurationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClinicConfigurationResult result = service().update(command(15));

        assertEquals(15, result.retentionYears());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertEquals(ActionType.UPDATE, audit.getActionType());
        assertEquals(ResourceType.CONFIGURATION, audit.getResourceType());
        assertEquals(ACTOR, audit.getUserId());
        assertTrue(audit.getDetail().contains("10 to 15"), audit.getDetail());
    }

    @Test
    void rejectsRetentionBelowMinimum() {
        when(clockPort.now()).thenReturn(NOW);
        when(clinicConfigurationRepository.find()).thenReturn(Optional.of(existing(10)));

        assertThrows(ValidationException.class, () -> service().update(command(5)));

        verify(auditLogRepository, never()).save(any());
    }
}
