package com.benhsoan.persistence.jpaRepository.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.benhsoan.domain.medicalrecord.enums.DiagnosisType;
import com.benhsoan.persistence.entity.medicalrecord.DiagnosisCatalogEntity;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordDiagnosisEntity;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class DiagnosisCatalogJpaIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-25T00:00:00Z");

    @Autowired private JpaDiagnosisCatalogRepository diagnosisCatalogRepository;
    @Autowired private JpaMedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;

    @Test
    void searchesCatalogByKeywordAndStatus() {
        diagnosisCatalogRepository.save(catalog("J00", "Cảm lạnh thông thường", "Hệ hô hấp", true));
        diagnosisCatalogRepository.save(catalog("J02.9", "Viêm họng cấp", "Hệ hô hấp", false));
        diagnosisCatalogRepository.save(catalog("K30", "Khó tiêu", "Hệ tiêu hóa", true));

        var activeRespiratory = diagnosisCatalogRepository.search("J", true);
        var allCatalogs = diagnosisCatalogRepository.search(null, null);

        assertEquals(1, activeRespiratory.size());
        assertEquals("J00", activeRespiratory.getFirst().getCode());
        assertEquals(3, allCatalogs.size());
        assertTrue(diagnosisCatalogRepository.existsByCode("J00"));
        assertFalse(diagnosisCatalogRepository.existsByCode("J99"));
    }

    @Test
    void detectsDiagnosisCatalogUsage() {
        DiagnosisCatalogEntity catalog = diagnosisCatalogRepository.save(catalog("J00", "Cảm lạnh thông thường", "Hệ hô hấp", true));
        UUID unusedCatalogId = UUID.randomUUID();
        medicalRecordDiagnosisRepository.save(MedicalRecordDiagnosisEntity.builder()
                .id(UUID.randomUUID()).medicalRecordId(UUID.randomUUID()).diagnosisCatalogId(catalog.getId())
                .diagnosisCode(catalog.getCode()).diagnosisName(catalog.getName()).diagnosisType(DiagnosisType.PRIMARY)
                .diagnosedBy(UUID.randomUUID()).diagnosedAt(NOW).createdAt(NOW).build());

        assertTrue(medicalRecordDiagnosisRepository.existsByDiagnosisCatalogId(catalog.getId()));
        assertFalse(medicalRecordDiagnosisRepository.existsByDiagnosisCatalogId(unusedCatalogId));
    }

    @Test
    void enforcesUniqueDiagnosisCode() {
        diagnosisCatalogRepository.save(catalog("J00", "Common cold", "Respiratory", true));
        diagnosisCatalogRepository.save(catalog("J00", "Another name", "Respiratory", true));

        assertThrows(DataIntegrityViolationException.class, diagnosisCatalogRepository::flush);
    }

    private DiagnosisCatalogEntity catalog(String code, String name, String diseaseGroup, boolean active) {
        return DiagnosisCatalogEntity.builder()
                .id(UUID.randomUUID()).code(code).name(name).diseaseGroup(diseaseGroup).active(active).createdAt(NOW)
                .build();
    }
}
