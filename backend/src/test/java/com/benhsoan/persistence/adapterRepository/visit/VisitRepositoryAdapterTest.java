package com.benhsoan.persistence.adapterRepository.visit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.persistence.entity.visit.VisitEntity;
import com.benhsoan.persistence.jpaRepository.visit.JpaVisitRepository;
import com.benhsoan.persistence.mapper.visit.VisitPersistenceMapper;

@ExtendWith(MockitoExtension.class)
class VisitRepositoryAdapterTest {

    @Mock
    private JpaVisitRepository jpaRepository;
    @Spy
    private VisitPersistenceMapper mapper = new VisitPersistenceMapper();
    @InjectMocks
    private VisitRepositoryAdapter adapter;

    @Test
    void mapsVisitFoundById() {
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        when(jpaRepository.findById(visitId)).thenReturn(Optional.of(VisitEntity.builder()
                .id(visitId).visitCode("VIS-001").patientId(patientId).doctorId(UUID.randomUUID())
                .visitType(VisitType.WALK_IN).status(VisitStatus.WAITING)
                .visitAt(Instant.parse("2026-08-20T02:00:00Z")).reason("Consultation")
                .createdBy(UUID.randomUUID()).createdAt(Instant.parse("2026-08-20T02:00:00Z"))
                .build()));

        var result = adapter.findById(visitId);

        assertEquals(patientId, result.orElseThrow().getPatientId());
    }
}
