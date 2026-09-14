package com.benhsoan.persistence.jpaRepository.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.medicalrecord.enums.DiagnosisType;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.persistence.entity.medicalrecord.DiagnosisCatalogEntity;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordDiagnosisEntity;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordEntity;
import com.benhsoan.persistence.entity.visit.VisitEntity;
import com.benhsoan.persistence.jpaRepository.visit.JpaVisitRepository;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class MedicalRecordDiagnosisSuggestionJpaIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-25T00:00:00Z");
    private static final UUID DOCTOR = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    private static final UUID SPECIALTY = UUID.fromString("f0000000-0000-0000-0000-000000000002");
    private static final UUID OTHER_SPECIALTY = UUID.fromString("f0000000-0000-0000-0000-000000000001");

    @Autowired
    private JpaMedicalRecordDiagnosisRepository diagnosisRepository;
    @Autowired
    private JpaDiagnosisCatalogRepository catalogRepository;
    @Autowired
    private JpaMedicalRecordRepository medicalRecordRepository;
    @Autowired
    private JpaVisitRepository visitRepository;

    @Test
    void findsDistinctRecentCatalogIdsOrderedByLatestUsageAndSkipsFreeText() {
        DiagnosisCatalogEntity j029 = catalog("J02.9", "Viêm họng cấp");
        DiagnosisCatalogEntity j00 = catalog("J00", "Cảm lạnh thông thường");
        catalogRepository.saveAll(List.of(j029, j00));

        MedicalRecordEntity record = medicalRecordRepository.save(record(visit().getId()));

        diagnosisRepository.saveAll(List.of(
                diagnosis(record.getId(), j029.getId(), NOW.minusSeconds(10)),
                diagnosis(record.getId(), j00.getId(), NOW.minusSeconds(20)),
                diagnosis(record.getId(), j029.getId(), NOW),
                freeText(record.getId())
        ));

        List<UUID> recent = diagnosisRepository.findRecentCatalogIdsByDoctor(DOCTOR, PageRequest.of(0, 10));

        assertEquals(2, recent.size());
        assertEquals(j029.getId(), recent.get(0));
        assertEquals(j00.getId(), recent.get(1));
    }

    @Test
    void aggregatesPopularCatalogIdsBySpecialtyOrderedByUsageCount() {
        DiagnosisCatalogEntity i10 = catalog("I10", "Tăng huyết áp");
        DiagnosisCatalogEntity j00 = catalog("J00", "Cảm lạnh thông thường");
        catalogRepository.saveAll(List.of(i10, j00));

        MedicalRecordEntity s1RecordA = medicalRecordRepository.save(record(visit(SPECIALTY).getId()));
        MedicalRecordEntity s1RecordB = medicalRecordRepository.save(record(visit(SPECIALTY).getId()));
        MedicalRecordEntity s2Record = medicalRecordRepository.save(record(visit(OTHER_SPECIALTY).getId()));

        diagnosisRepository.saveAll(List.of(
                diagnosis(s1RecordA.getId(), i10.getId(), NOW.minusSeconds(30)),
                diagnosis(s1RecordB.getId(), i10.getId(), NOW.minusSeconds(20)),
                diagnosis(s1RecordA.getId(), i10.getId(), NOW.minusSeconds(10)),
                diagnosis(s1RecordB.getId(), j00.getId(), NOW),
                diagnosis(s2Record.getId(), i10.getId(), NOW.minusSeconds(5))
        ));

        List<UUID> popular = diagnosisRepository.findPopularCatalogIdsBySpecialty(SPECIALTY, PageRequest.of(0, 10));

        assertEquals(2, popular.size());
        assertEquals(i10.getId(), popular.get(0));
        assertEquals(j00.getId(), popular.get(1));
    }

    @Test
    void recentExcludesDiagnosesFromCancelledVisits() {
        DiagnosisCatalogEntity j029 = catalog("J02.9", "Viêm họng cấp");
        DiagnosisCatalogEntity j00 = catalog("J00", "Cảm lạnh thông thường");
        catalogRepository.saveAll(List.of(j029, j00));

        MedicalRecordEntity completedRecord = medicalRecordRepository.save(record(visit(SPECIALTY).getId()));
        MedicalRecordEntity cancelledRecord = medicalRecordRepository.save(record(visit(SPECIALTY, VisitStatus.CANCELLED).getId()));

        diagnosisRepository.saveAll(List.of(
                diagnosis(completedRecord.getId(), j00.getId(), NOW.minusSeconds(20)),
                diagnosis(cancelledRecord.getId(), j029.getId(), NOW)
        ));

        List<UUID> recent = diagnosisRepository.findRecentCatalogIdsByDoctor(DOCTOR, PageRequest.of(0, 10));

        assertEquals(1, recent.size());
        assertEquals(j00.getId(), recent.get(0));
    }

    @Test
    void popularExcludesDiagnosesFromCancelledVisits() {
        DiagnosisCatalogEntity i10 = catalog("I10", "Tăng huyết áp");
        DiagnosisCatalogEntity j00 = catalog("J00", "Cảm lạnh thông thường");
        catalogRepository.saveAll(List.of(i10, j00));

        MedicalRecordEntity cancelledRecord = medicalRecordRepository.save(record(visit(SPECIALTY, VisitStatus.CANCELLED).getId()));
        MedicalRecordEntity completedRecord = medicalRecordRepository.save(record(visit(SPECIALTY).getId()));

        diagnosisRepository.saveAll(List.of(
                diagnosis(cancelledRecord.getId(), i10.getId(), NOW.minusSeconds(30)),
                diagnosis(completedRecord.getId(), j00.getId(), NOW)
        ));

        List<UUID> popular = diagnosisRepository.findPopularCatalogIdsBySpecialty(SPECIALTY, PageRequest.of(0, 10));

        assertEquals(1, popular.size());
        assertEquals(j00.getId(), popular.get(0));
    }

    private DiagnosisCatalogEntity catalog(String code, String name) {
        return DiagnosisCatalogEntity.builder()
                .id(UUID.randomUUID()).code(code).name(name).diseaseGroup("Hệ hô hấp")
                .active(true).createdAt(NOW).build();
    }

    @Test
    void recentQueryHonorsLimit() {
        DiagnosisCatalogEntity c1 = catalog("J00", "Cảm lạnh thông thường");
        DiagnosisCatalogEntity c2 = catalog("J01.9", "Viêm xoang");
        DiagnosisCatalogEntity c3 = catalog("J02.9", "Viêm họng cấp");
        catalogRepository.saveAll(List.of(c1, c2, c3));

        MedicalRecordEntity record = medicalRecordRepository.save(record(visit().getId()));
        diagnosisRepository.saveAll(List.of(
                diagnosis(record.getId(), c1.getId(), NOW.minusSeconds(30)),
                diagnosis(record.getId(), c2.getId(), NOW.minusSeconds(20)),
                diagnosis(record.getId(), c3.getId(), NOW.minusSeconds(10))
        ));

        List<UUID> recent = diagnosisRepository.findRecentCatalogIdsByDoctor(DOCTOR, PageRequest.of(0, 2));

        assertEquals(2, recent.size());
        assertEquals(c3.getId(), recent.get(0));
        assertEquals(c2.getId(), recent.get(1));
    }

    @Test
    void popularQueryHonorsLimit() {
        DiagnosisCatalogEntity c1 = catalog("J00", "Cảm lạnh thông thường");
        DiagnosisCatalogEntity c2 = catalog("J01.9", "Viêm xoang");
        catalogRepository.saveAll(List.of(c1, c2));

        MedicalRecordEntity r1 = medicalRecordRepository.save(record(visit(SPECIALTY).getId()));
        MedicalRecordEntity r2 = medicalRecordRepository.save(record(visit(SPECIALTY).getId()));
        diagnosisRepository.saveAll(List.of(
                diagnosis(r1.getId(), c1.getId(), NOW.minusSeconds(20)),
                diagnosis(r2.getId(), c2.getId(), NOW.minusSeconds(10))
        ));

        List<UUID> popular = diagnosisRepository.findPopularCatalogIdsBySpecialty(SPECIALTY, PageRequest.of(0, 1));

        assertEquals(1, popular.size());
    }

    private VisitEntity visit() {
        return visit(SPECIALTY);
    }

    private VisitEntity visit(UUID specialtyId) {
        return visit(specialtyId, VisitStatus.COMPLETED);
    }

    private VisitEntity visit(UUID specialtyId, VisitStatus status) {
        return visitRepository.save(VisitEntity.builder()
                .id(UUID.randomUUID())
                .visitCode("VIS-" + UUID.randomUUID().toString().substring(0, 6))
                .patientId(UUID.randomUUID())
                .doctorId(DOCTOR)
                .specialtyId(specialtyId)
                .visitType(VisitType.WALK_IN)
                .status(status)
                .visitAt(NOW)
                .reason("Exam")
                .createdBy(DOCTOR)
                .createdAt(NOW)
                .build());
    }

    private MedicalRecordEntity record(UUID visitId) {
        return MedicalRecordEntity.builder()
                .id(UUID.randomUUID())
                .visitId(visitId)
                .status(MedicalRecordStatus.DRAFT)
                .createdBy(DOCTOR)
                .createdAt(NOW)
                .build();
    }

    private MedicalRecordDiagnosisEntity diagnosis(UUID medicalRecordId, UUID catalogId, Instant diagnosedAt) {
        return MedicalRecordDiagnosisEntity.builder()
                .id(UUID.randomUUID())
                .medicalRecordId(medicalRecordId)
                .diagnosisCatalogId(catalogId)
                .diagnosisCode("J02.9")
                .diagnosisName("Viêm họng cấp")
                .diagnosisType(DiagnosisType.PRIMARY)
                .diagnosedBy(DOCTOR)
                .diagnosedAt(diagnosedAt)
                .createdAt(NOW)
                .build();
    }

    private MedicalRecordDiagnosisEntity freeText(UUID medicalRecordId) {
        return MedicalRecordDiagnosisEntity.builder()
                .id(UUID.randomUUID())
                .medicalRecordId(medicalRecordId)
                .diagnosisCatalogId(null)
                .diagnosisCode(null)
                .diagnosisName("Clinical observation")
                .diagnosisType(DiagnosisType.SECONDARY)
                .diagnosedBy(DOCTOR)
                .diagnosedAt(NOW.plusSeconds(60))
                .createdAt(NOW)
                .build();
    }
}
