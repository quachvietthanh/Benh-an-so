package com.benhsoan.persistence.jpaRepository.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.shared.VietnameseTextNormalizer;
import com.benhsoan.persistence.entity.medicalrecord.DiagnosisCatalogEntity;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class DiagnosisCatalogSearchJpaIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-25T00:00:00Z");

    @Autowired
    private JpaDiagnosisCatalogRepository repository;

    @Test
    void ranksExactCodeBeforeCodePrefix() {
        repository.saveAll(List.of(
                catalog("J02", "Alpha", null, "Hệ hô hấp", true),
                catalog("J02.9", "Beta", null, "Hệ hô hấp", true)
        ));

        List<DiagnosisCatalogEntity> results = repository.searchActiveByKeyword(
                "j02", null, PageRequest.of(0, 50));

        assertEquals(2, results.size());
        assertEquals("J02", results.get(0).getCode());
        assertEquals("J02.9", results.get(1).getCode());
    }

    @Test
    void matchesVietnameseNameWithoutDiacritics() {
        repository.save(catalog("J02.9", "Viêm họng cấp", null, "Hệ hô hấp", true));

        List<DiagnosisCatalogEntity> results = repository.searchActiveByKeyword(
                "viem hong", null, PageRequest.of(0, 50));

        assertEquals(1, results.size());
        assertEquals("J02.9", results.getFirst().getCode());
    }

    @Test
    void matchesAccentedKeywordAfterNormalization() {
        repository.save(catalog("J02.9", "Viêm họng cấp", null, "Hệ hô hấp", true));

        List<DiagnosisCatalogEntity> results = repository.searchActiveByKeyword(
                VietnameseTextNormalizer.normalize("Viêm họng cấp"), null, PageRequest.of(0, 50));

        assertEquals(1, results.size());
        assertEquals("J02.9", results.getFirst().getCode());
    }

    @Test
    void matchesAbbreviationExactlyAndByPrefix() {
        repository.save(catalog("I10", "Tăng huyết áp", "THA", "Hệ tuần hoàn", true));

        List<DiagnosisCatalogEntity> exact = repository.searchActiveByKeyword(
                "tha", null, PageRequest.of(0, 50));
        List<DiagnosisCatalogEntity> prefix = repository.searchActiveByKeyword(
                "t", null, PageRequest.of(0, 50));

        assertEquals(1, exact.size());
        assertEquals("I10", exact.getFirst().getCode());
        assertEquals(1, prefix.size());
        assertEquals("I10", prefix.getFirst().getCode());
    }

    @Test
    void returnsEmptyForUnknownKeyword() {
        repository.save(catalog("J02.9", "Viêm họng cấp", null, "Hệ hô hấp", true));

        assertTrue(repository.searchActiveByKeyword("xyz", null, PageRequest.of(0, 50)).isEmpty());
    }

    @Test
    void limitsResultsInTheDatabase() {
        repository.saveAll(List.of(
                catalog("J00", "Cảm lạnh thông thường", null, "Hệ hô hấp", true),
                catalog("J01.9", "Viêm xoang", null, "Hệ hô hấp", true),
                catalog("J02.9", "Viêm họng cấp", null, "Hệ hô hấp", true)
        ));

        List<DiagnosisCatalogEntity> results = repository.searchActiveByKeyword(
                "viem", null, PageRequest.of(0, 2));

        assertEquals(2, results.size());
    }

    @Test
    void filtersByDiseaseGroupAndActiveOnly() {
        repository.saveAll(List.of(
                catalog("J00", "Cảm lạnh thông thường", null, "Hệ hô hấp", true),
                catalog("J02.9", "Viêm họng cấp", null, "Hệ hô hấp", false),
                catalog("I10", "Tăng huyết áp", null, "Hệ tuần hoàn", true)
        ));

        List<DiagnosisCatalogEntity> respiratory = repository.findByActiveTrueAndDiseaseGroupOrderByCodeAsc(
                "Hệ hô hấp", PageRequest.of(0, 50));

        assertEquals(1, respiratory.size());
        assertEquals("J00", respiratory.getFirst().getCode());

        assertTrue(repository.findByActiveTrueAndDiseaseGroupOrderByCodeAsc(
                "Nhóm không tồn tại", PageRequest.of(0, 50)).isEmpty());
    }

    @Test
    void combinesKeywordAndDiseaseGroup() {
        repository.saveAll(List.of(
                catalog("J02.9", "Viêm họng cấp", null, "Hệ hô hấp", true),
                catalog("I10", "Tăng huyết áp", null, "Hệ tuần hoàn", true)
        ));

        List<DiagnosisCatalogEntity> results = repository.searchActiveByKeyword(
                "viem", "Hệ hô hấp", PageRequest.of(0, 50));

        assertEquals(1, results.size());
        assertEquals("J02.9", results.getFirst().getCode());
    }

    @Test
    void adminSearchIncludesAbbreviation() {
        repository.save(catalog("I10", "Tăng huyết áp", "THA", "Hệ tuần hoàn", true));

        List<DiagnosisCatalogEntity> byAbbreviation = repository.search("THA", null);
        List<DiagnosisCatalogEntity> byCode = repository.search("i10", null);
        List<DiagnosisCatalogEntity> byName = repository.search("huyết", null);

        assertEquals(1, byAbbreviation.size());
        assertEquals(1, byCode.size());
        assertEquals(1, byName.size());
    }

    private DiagnosisCatalogEntity catalog(
            String code, String name, String abbreviation, String diseaseGroup, boolean active) {
        return DiagnosisCatalogEntity.builder()
                .id(UUID.randomUUID())
                .code(code)
                .name(name)
                .nameNorm(VietnameseTextNormalizer.normalize(name))
                .abbreviation(abbreviation)
                .abbreviationNorm(VietnameseTextNormalizer.normalize(abbreviation))
                .diseaseGroup(diseaseGroup)
                .active(active)
                .createdAt(NOW)
                .build();
    }
}
