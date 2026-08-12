package com.benhsoan.persistence.adapterRepository.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAccessLogEntity;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordAccessLogRepository;
import com.benhsoan.persistence.mapper.medicalrecord.MedicalRecordAccessLogPersistenceMapper;
import com.benhsoan.port.dto.command.medicalrecord.GetMedicalRecordAccessLogsQuery;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({MedicalRecordAccessLogRepositoryAdapter.class, MedicalRecordAccessLogPersistenceMapper.class})
class MedicalRecordAccessLogRepositoryAdapterIntegrationTest {

    @Autowired
    private JpaMedicalRecordAccessLogRepository jpaRepository;

    @Autowired
    private MedicalRecordAccessLogRepositoryAdapter adapter;

    @Test
    void searchesByCombinedFiltersAndSortsNewestFirst() {
        UUID actorA = UUID.randomUUID();
        UUID actorB = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID medicalRecordId = UUID.randomUUID();

        jpaRepository.saveAll(List.of(
                accessLog(UUID.randomUUID(), patientId, visitId, medicalRecordId, actorA,
                        MedicalRecordAccessAction.VIEW, Instant.parse("2026-08-11T10:00:00Z")),
                accessLog(UUID.randomUUID(), patientId, visitId, medicalRecordId, actorA,
                        MedicalRecordAccessAction.UPDATE, Instant.parse("2026-08-11T12:00:00Z")),
                accessLog(UUID.randomUUID(), patientId, visitId, medicalRecordId, actorB,
                        MedicalRecordAccessAction.VIEW, Instant.parse("2026-08-11T13:00:00Z"))
        ));

        GetMedicalRecordAccessLogsQuery query = new GetMedicalRecordAccessLogsQuery(
                actorA,
                patientId,
                medicalRecordId,
                visitId,
                Instant.parse("2026-08-11T00:00:00Z"),
                Instant.parse("2026-08-11T23:59:59Z"),
                0,
                20
        );

        var result = adapter.search(query, PageRequest.of(0, 20,
                Sort.by(Sort.Order.desc("accessedAt"), Sort.Order.desc("id"))));

        assertEquals(2, result.getTotalElements());
        assertEquals(MedicalRecordAccessAction.UPDATE, result.getContent().get(0).getAction());
        assertEquals(MedicalRecordAccessAction.VIEW, result.getContent().get(1).getAction());
        assertEquals(actorA, result.getContent().get(0).getAccessedBy());
        assertEquals(actorA, result.getContent().get(1).getAccessedBy());
    }

    private MedicalRecordAccessLogEntity accessLog(
            UUID id,
            UUID patientId,
            UUID visitId,
            UUID medicalRecordId,
            UUID accessedBy,
            MedicalRecordAccessAction action,
            Instant accessedAt
    ) {
        return MedicalRecordAccessLogEntity.builder()
                .id(id)
                .patientId(patientId)
                .visitId(visitId)
                .medicalRecordId(medicalRecordId)
                .accessedBy(accessedBy)
                .action(action)
                .detail(action.name())
                .accessedAt(accessedAt)
                .build();
    }
}
