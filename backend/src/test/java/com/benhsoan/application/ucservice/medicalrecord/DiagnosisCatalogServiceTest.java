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

    private DiagnosisCatalog sampleDiagnosis(String code, String name) {
        return DiagnosisCatalog.restore(id, code, name, "Respiratory", "Test description", true, Instant.now(), null);
    }

    @Test
    @DisplayName("Should return empty list when query is blank")
    void searchBlankReturnsEmpty() {
        assertTrue(service.search(null).isEmpty());
        assertTrue(service.search("").isEmpty());
        assertTrue(service.search("   ").isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should search by code or name")
    void searchReturnsResults() {
        var catalog = sampleDiagnosis("J00", "Common cold");
        when(repository.search("cold", true))
                .thenReturn(List.of(catalog));

        List<DiagnosisCatalogResult> results = service.search("cold");

        assertEquals(1, results.size());
        assertEquals("J00", results.getFirst().code());
        assertEquals("Common cold", results.getFirst().name());
        verify(repository).search("cold", true);
    }

    @Test
    @DisplayName("Should return multiple results")
    void searchReturnsMultiple() {
        var c1 = sampleDiagnosis("J00", "Common cold");
        var c2 = sampleDiagnosis("J06.9", "Acute URTI");
        when(repository.search("J", true))
                .thenReturn(List.of(c1, c2));

        List<DiagnosisCatalogResult> results = service.search("J");

        assertEquals(2, results.size());
        verify(repository).search("J", true);
    }

    @Test
    @DisplayName("Management search can include inactive catalog entries")
    void managementSearchUsesRequestedActiveFilter() {
        var inactiveCatalog = DiagnosisCatalog.restore(
                id, "J00", "Common cold", "Respiratory", "Test description", false, Instant.now(), null
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
