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
    @DisplayName("Should return empty list when query is blank")
    void searchBlankReturnsEmpty() {
        assertTrue(service.search(null).isEmpty());
        assertTrue(service.search("").isEmpty());
        assertTrue(service.search("   ").isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should search Vietnamese name without diacritics")
    void searchMatchesVietnameseNameWithoutDiacritics() {
        when(repository.findAllByActive(true)).thenReturn(List.of(catalog("J02.9", "Viêm họng cấp")));

        List<DiagnosisCatalogResult> results = service.search("viem hong");

        assertEquals(1, results.size());
        assertEquals("J02.9", results.getFirst().code());
    }

    @Test
    @DisplayName("Should search code case-insensitively")
    void searchMatchesCodeCaseInsensitively() {
        when(repository.findAllByActive(true)).thenReturn(List.of(catalog("J02.9", "Viêm họng cấp")));

        List<DiagnosisCatalogResult> results = service.search("j02");

        assertEquals(1, results.size());
        assertEquals("J02.9", results.getFirst().code());
    }

    @Test
    @DisplayName("Should rank exact code, then prefix, then substring deterministically")
    void searchRanksByMatchLevel() {
        var exactCode = catalog("J02", "Alpha");
        var codePrefix = catalog("J02.9", "Beta");
        var namePrefix = catalog("K00", "J02 something");
        var nameSubstring = catalog("K01", "Contains j02 inside");
        var noMatch = catalog("Z00", "Unrelated");
        when(repository.findAllByActive(true))
                .thenReturn(List.of(noMatch, nameSubstring, codePrefix, exactCode, namePrefix));

        List<DiagnosisCatalogResult> results = service.search("j02");

        assertEquals(4, results.size());
        assertEquals("J02", results.get(0).code());
        assertEquals("J02.9", results.get(1).code());
        assertEquals("K00", results.get(2).code());
        assertEquals("K01", results.get(3).code());
    }

    @Test
    @DisplayName("Should order equal relevance by code ascending")
    void searchOrdersEqualRankByCode() {
        var first = catalog("J01.9", "Viêm xoang");
        var second = catalog("J02.9", "Viêm họng cấp");
        when(repository.findAllByActive(true)).thenReturn(List.of(second, first));

        List<DiagnosisCatalogResult> results = service.search("viem");

        assertEquals(2, results.size());
        assertEquals("J01.9", results.get(0).code());
        assertEquals("J02.9", results.get(1).code());
    }

    @Test
    @DisplayName("Should search by clinical abbreviation independently of code")
    void searchMatchesAbbreviation() {
        var catalog = DiagnosisCatalog.restore(
                UUID.randomUUID(), "I10", "Tăng huyết áp", "THA", "Hệ tuần hoàn", null, true, now, null);
        when(repository.findAllByActive(true)).thenReturn(List.of(catalog));

        List<DiagnosisCatalogResult> results = service.search("tha");

        assertEquals(1, results.size());
        assertEquals("I10", results.getFirst().code());
        assertEquals("THA", results.getFirst().abbreviation());
    }

    @Test
    @DisplayName("Should only look up active catalog entries")
    void searchUsesActiveCatalogLookup() {
        when(repository.findAllByActive(true)).thenReturn(List.of());

        service.search("viem");

        verify(repository).findAllByActive(true);
        verify(repository, never()).search(anyString(), any());
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

