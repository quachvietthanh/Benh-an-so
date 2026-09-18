package com.benhsoan.persistence.mapper.visit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.visit.VisitHandover;
import com.benhsoan.persistence.entity.visit.VisitHandoverEntity;

class VisitHandoverPersistenceMapperTest {

    private final VisitHandoverPersistenceMapper mapper = new VisitHandoverPersistenceMapper();

    @Test
    @DisplayName("toEntity with valid domain should map all fields correctly")
    void toEntity_withValidDomain_shouldMapAllFieldsCorrectly() {
        UUID id = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID fromDoctorId = UUID.randomUUID();
        UUID toDoctorId = UUID.randomUUID();
        String reason = "End of shift handover";
        Instant handedOverAt = Instant.parse("2026-09-17T08:30:00Z");
        UUID createdBy = fromDoctorId;
        Instant createdAt = handedOverAt;

        VisitHandover domain = VisitHandover.restore(
                id, visitId, fromDoctorId, toDoctorId, reason, handedOverAt, createdBy, createdAt
        );

        VisitHandoverEntity entity = mapper.toEntity(domain);

        assertNotNull(entity);
        assertEquals(id, entity.getId());
        assertEquals(visitId, entity.getVisitId());
        assertEquals(fromDoctorId, entity.getFromDoctorId());
        assertEquals(toDoctorId, entity.getToDoctorId());
        assertEquals(reason, entity.getReason());
        assertEquals(handedOverAt, entity.getHandedOverAt());
        assertEquals(createdBy, entity.getCreatedBy());
        assertEquals(createdAt, entity.getCreatedAt());
    }

    @Test
    @DisplayName("toDomain with valid entity should restore domain aggregate correctly")
    void toDomain_withValidEntity_shouldRestoreDomainAggregate() {
        UUID id = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID fromDoctorId = UUID.randomUUID();
        UUID toDoctorId = UUID.randomUUID();
        String reason = "Transfer to specialist";
        Instant handedOverAt = Instant.parse("2026-09-17T09:00:00Z");
        UUID createdBy = fromDoctorId;
        Instant createdAt = handedOverAt;

        VisitHandoverEntity entity = VisitHandoverEntity.builder()
                .id(id)
                .visitId(visitId)
                .fromDoctorId(fromDoctorId)
                .toDoctorId(toDoctorId)
                .reason(reason)
                .handedOverAt(handedOverAt)
                .createdBy(createdBy)
                .createdAt(createdAt)
                .build();

        VisitHandover domain = mapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(id, domain.getId());
        assertEquals(visitId, domain.getVisitId());
        assertEquals(fromDoctorId, domain.getFromDoctorId());
        assertEquals(toDoctorId, domain.getToDoctorId());
        assertEquals(reason, domain.getReason());
        assertEquals(handedOverAt, domain.getHandedOverAt());
        assertEquals(createdBy, domain.getCreatedBy());
        assertEquals(createdAt, domain.getCreatedAt());
    }

    @Test
    @DisplayName("toEntity and toDomain with null input should safely return null")
    void toEntity_and_toDomain_withNull_shouldReturnNull() {
        assertNull(mapper.toEntity(null));
        assertNull(mapper.toDomain(null));
    }
}
