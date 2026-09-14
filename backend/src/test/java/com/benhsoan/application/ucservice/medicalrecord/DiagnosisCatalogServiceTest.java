package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogNotFoundException;
import com.benhsoan.port.dto.result.DiagnosisCatalogResult;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;

@DisplayName("DiagnosisCatalogService Tests")
@ExtendWith(MockitoExtension.class)
class DiagnosisCatalogServiceTest {

    @Mock
    private DiagnosisCatalogRepository repository;
    @Spy
    private DiagnosisCatalogResultMapper resultMapper = new DiagnosisCatalogResultMapper();

    @InjectMocks
    private DiagnosisCatalogService service;

    private final UUID id = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-08-25T00:00:00Z");

    private DiagnosisCatalog catalog(String code, String name) {
        return DiagnosisCatalog.restore(UUID.randomUUID(), code, name, "Hệ hô hấp", null, true, now, null);
    }

    @Test
    @DisplayName("Blank query and group returns empty without touching the repository")
    void blankQueryAndGroupReturnsEmpty() {
        assertTrue(service.search(null, (String) null).isEmpty());
        assertTrue(service.search("", "").isEmpty());
        assertTrue(service.search("   ", "   ").isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Normalizes keyword and delegates to the bounded active search")
    void keywordSearchDelegatesToSearchActive() {
        when(repository.searchActive("viem hong", null, 50))
                .thenReturn(List.of(catalog("J02.9", "Viêm họng cấp")));

        List<DiagnosisCatalogResult> results = service.search("Viêm họng", (String) null);

        assertEquals(1, results.size());
        assertEquals("J02.9", results.getFirst().code());
        verify(repository).searchActive("viem hong", null, 50);
        verify(repository, never()).findAllByActive(anyBoolean());
    }

    @Test
    @DisplayName("Delegates to disease-group listing when only a group is supplied")
    void groupOnlySearchDelegatesToGroupLookup() {
        when(repository.findByActiveAndDiseaseGroup("Hệ hô hấp", 50))
                .thenReturn(List.of(catalog("J00", "Cảm lạnh thông thường")));

        List<DiagnosisCatalogResult> results = service.search(null, "Hệ hô hấp");

        assertEquals(1, results.size());
        assertEquals("J00", results.getFirst().code());
        verify(repository).findByActiveAndDiseaseGroup("Hệ hô hấp", 50);
    }

    @Test
    @DisplayName("Combines keyword and disease-group into a single bounded query")
    void keywordAndGroupAreCombined() {
        when(repository.searchActive("viem hong", "Hệ hô hấp", 50))
                .thenReturn(List.of(catalog("J02.9", "Viêm họng cấp")));

        List<DiagnosisCatalogResult> results = service.search("viem hong", "Hệ hô hấp");

        assertEquals(1, results.size());
        verify(repository).searchActive("viem hong", "Hệ hô hấp", 50);
    }

    @Test
    @DisplayName("Management search can include inactive catalog entries")
    void managementSearchUsesRequestedActiveFilter() {
        var inactiveCatalog = DiagnosisCatalog.restore(
                id, "J00", "Common cold", "Respiratory", "Test description", false, now, null
        );
        when(repository.search(null, false)).thenReturn(List.of(inactiveCatalog));

        List<DiagnosisCatalogResult> results = service.search(null, false);

        assertEquals(1, results.size());
        assertFalse(results.getFirst().active());
        verify(repository).search(null, false);
    }

    @Test
    @DisplayName("Management get by id reports catalog not found")
    void managementGetByIdReportsNotFound() {
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(DiagnosisCatalogNotFoundException.class, () -> service.getById(id));
    }
}

