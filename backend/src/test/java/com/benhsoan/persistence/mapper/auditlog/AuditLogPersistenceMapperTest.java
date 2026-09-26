package com.benhsoan.persistence.mapper.auditlog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;

class AuditLogPersistenceMapperTest {

    private final AuditLogPersistenceMapper mapper = new AuditLogPersistenceMapper();

    @Test
    void serializesPlainTextDetailAsJsonString() {
        String detail = "Service created: POSTMAN-CBC-001";
        var entity = mapper.toEntity(audit(detail));

        assertEquals("\"Service created: POSTMAN-CBC-001\"", entity.getDetail());
        assertEquals(detail, mapper.toDomain(entity).getDetail());
    }

    @Test
    void preservesStructuredJsonDetail() {
        String detail = "{\"serviceCode\":\"POSTMAN-CBC-001\"}";

        var entity = mapper.toEntity(audit(detail));

        assertEquals(detail, entity.getDetail());
        assertEquals(detail, mapper.toDomain(entity).getDetail());
    }

    private AuditLog audit(String detail) {
        return AuditLog.create(
                UUID.randomUUID(),
                ActionType.CREATE,
                ResourceType.SERVICE_CATALOG,
                UUID.randomUUID(),
                detail,
                null,
                Instant.parse("2026-08-18T08:00:00Z")
        );
    }
}
