package com.benhsoan.persistence.adapterRepository.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.persistence.entity.medicalrecord.DiagnosisCatalogEntity;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaDiagnosisCatalogRepository;
import com.benhsoan.persistence.mapper.medicalrecord.DiagnosisCatalogPersistenceMapper;

@ExtendWith(MockitoExtension.class)
class DiagnosisCatalogRepositoryAdapterTest {

    @Mock private JpaDiagnosisCatalogRepository jpaRepository;
    @Spy private DiagnosisCatalogPersistenceMapper mapper = new DiagnosisCatalogPersistenceMapper();
    @InjectMocks private DiagnosisCatalogRepositoryAdapter adapter;

    @Test
    void savesCatalogAndNormalizesCodeForExistenceCheck() {
        Instant now = Instant.parse("2026-08-25T00:00:00Z");
        DiagnosisCatalog catalog = DiagnosisCatalog.restore(
                UUID.randomUUID(), " j06.9 ", "Nhiễm trùng hô hấp trên", "Hệ hô hấp", null, true, now, null
        );
        when(jpaRepository.saveAndFlush(any(DiagnosisCatalogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jpaRepository.existsByCode("J06.9")).thenReturn(true);

        DiagnosisCatalog saved = adapter.save(catalog);

        assertEquals("J06.9", saved.getCode());
        assertTrue(adapter.existsByCode(" j06.9 "));
        verify(jpaRepository).existsByCode("J06.9");
    }

    @Test
    void searchesAdministrativeCatalogByKeywordAndStatus() {
        DiagnosisCatalogEntity catalog = DiagnosisCatalogEntity.builder()
                .id(UUID.randomUUID()).code("J00").name("Cảm lạnh thông thường").diseaseGroup("Hệ hô hấp")
                .active(true).createdAt(Instant.parse("2026-08-25T00:00:00Z")).build();
        when(jpaRepository.search("cảm lạnh", true)).thenReturn(List.of(catalog));

        List<DiagnosisCatalog> result = adapter.search("  cảm lạnh  ", true);

        assertEquals(1, result.size());
        assertEquals("Hệ hô hấp", result.getFirst().getDiseaseGroup());
    }
}
