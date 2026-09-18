package com.benhsoan.application.ucservice.auditlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.port.dto.command.auditlog.AdminOperationLogQuery;
import com.benhsoan.port.dto.result.auditlog.AdminOperationLogResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

@ExtendWith(MockitoExtension.class)
class GetAdminOperationLogsServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;

    private GetAdminOperationLogsService service;

    @BeforeEach
    void setUp() {
        service = new GetAdminOperationLogsService(auditLogRepository, userRepository);
    }

    @Test
    void passesActorAndResourceTypeFiltersAndEnrichesActorName() {
        UUID actorId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-01T00:00:00Z");
        var query = new AdminOperationLogQuery(actorId, ResourceType.SERVICE_PRICE, from, to);
        var pageable = PageRequest.of(0, 20);

        AuditLog log = AuditLog.create(actorId, ActionType.UPDATE, ResourceType.SERVICE_PRICE,
                resourceId, "{\"before\":null,\"after\":{\"price\":100}}", null,
                Instant.parse("2026-01-15T00:00:00Z"));
        when(auditLogRepository.findAdminOperationLogs(actorId, ResourceType.SERVICE_PRICE, from, to, pageable))
                .thenReturn(new PageImpl<>(List.of(log), pageable, 1));
        User actor = User.restore(actorId, "admin", "hash", "System Administrator", "a@x.com", null,
                UUID.randomUUID(), true, null, Instant.now());
        when(userRepository.findAllById(any())).thenReturn(List.of(actor));

        Page<AdminOperationLogResult> result = service.getLogs(query, pageable);

        verify(auditLogRepository).findAdminOperationLogs(actorId, ResourceType.SERVICE_PRICE, from, to, pageable);
        assertEquals(1, result.getTotalElements());
        assertEquals("System Administrator", result.getContent().get(0).actorName());
        assertEquals(actorId, result.getContent().get(0).actorId());
    }
}
