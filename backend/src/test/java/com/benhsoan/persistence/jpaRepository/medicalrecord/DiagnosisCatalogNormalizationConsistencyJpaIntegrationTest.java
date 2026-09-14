package com.benhsoan.persistence.jpaRepository.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.persistence.adapterRepository.medicalrecord.DiagnosisCatalogRepositoryAdapter;
import com.benhsoan.persistence.entity.medicalrecord.DiagnosisCatalogEntity;
import com.benhsoan.persistence.mapper.medicalrecord.DiagnosisCatalogPersistenceMapper;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class DiagnosisCatalogNormalizationConsistencyJpaIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-25T00:00:00Z");

    @Autowired private JpaDiagnosisCatalogRepository repository;
    @Autowired private TestEntityManager entityManager;

    private final DiagnosisCatalogPersistenceMapper mapper = new DiagnosisCatalogPersistenceMapper();

    private DiagnosisCatalogRepositoryAdapter adapter() {
        return new DiagnosisCatalogRepositoryAdapter(repository, mapper);
    }

    @Test
    void renameUpdatesPersistedNameNormSoOnlyNewNameMatches() {
        DiagnosisCatalogRepositoryAdapter adapter = adapter();
        DiagnosisCatalog created = adapter.save(DiagnosisCatalog.create(
                UUID.randomUUID(), "J02.9", "Viêm họng cấp", "VHC", "Hệ hô hấp", null, NOW));

        created.updateInformation("Viêm amidan cấp", "VHC", "Hệ hô hấp", null, NOW);
        adapter.save(created);

        entityManager.clear();

        DiagnosisCatalogEntity persisted = repository.findById(created.getId()).orElseThrow();
        assertEquals("Viêm amidan cấp", persisted.getName());
        assertEquals("viem amidan cap", persisted.getNameNorm());

        List<DiagnosisCatalogEntity> byNewName = repository.searchActiveByKeyword(
                "viem amidan", null, PageRequest.of(0, 50));
        assertEquals(1, byNewName.size());
        assertEquals(created.getId(), byNewName.getFirst().getId());

        assertTrue(repository.searchActiveByKeyword("viem hong", null, PageRequest.of(0, 50)).isEmpty());
    }

    @Test
    void abbreviationUpdateUpdatesPersistedAbbreviationNorm() {
        DiagnosisCatalogRepositoryAdapter adapter = adapter();
        DiagnosisCatalog created = adapter.save(DiagnosisCatalog.create(
                UUID.randomUUID(), "I10", "Tăng huyết áp", "THA", "Hệ tuần hoàn", null, NOW));

        created.updateInformation("Tăng huyết áp", "HA", "Hệ tuần hoàn", null, NOW);
        adapter.save(created);

        entityManager.clear();

        DiagnosisCatalogEntity persisted = repository.findById(created.getId()).orElseThrow();
        assertEquals("HA", persisted.getAbbreviation());
        assertEquals("ha", persisted.getAbbreviationNorm());

        assertEquals(1, repository.searchActiveByKeyword("ha", null, PageRequest.of(0, 50)).size());
        assertTrue(repository.searchActiveByKeyword("tha", null, PageRequest.of(0, 50)).isEmpty());
    }

    @Test
    void nullAbbreviationPersistsNullAbbreviationNormWithoutBreakingSearch() {
        DiagnosisCatalogRepositoryAdapter adapter = adapter();
        DiagnosisCatalog created = adapter.save(DiagnosisCatalog.create(
                UUID.randomUUID(), "I10", "Tăng huyết áp", "THA", "Hệ tuần hoàn", null, NOW));

        created.updateInformation("Tăng huyết áp", null, "Hệ tuần hoàn", null, NOW);
        adapter.save(created);

        entityManager.clear();

        DiagnosisCatalogEntity persisted = repository.findById(created.getId()).orElseThrow();
        assertNull(persisted.getAbbreviation());
        assertNull(persisted.getAbbreviationNorm());

        assertEquals(1, repository.searchActiveByKeyword("tang huyet", null, PageRequest.of(0, 50)).size());
    }
}
