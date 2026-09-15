package com.benhsoan.application.ucservice.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.clinical.ClinicalReferenceRange;
import com.benhsoan.domain.clinical.ClinicalServiceCatalog;
import com.benhsoan.domain.clinical.enums.ClinicalResultDataType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;
import com.benhsoan.domain.clinical.exception.ClinicalServiceCatalogNotFoundException;
import com.benhsoan.domain.clinical.exception.ClinicalServiceCodeAlreadyExistsException;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.port.dto.command.clinical.CreateClinicalServiceCommand;
import com.benhsoan.port.dto.command.clinical.UpdateClinicalServiceCommand;
import com.benhsoan.port.dto.result.ClinicalServiceManagementResult;
import com.benhsoan.port.outbound.repository.clinical.ClinicalReferenceRangeRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalServiceCatalogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class ClinicalServiceManagementServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T01:00:00Z");
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID SERVICE_CATALOG_ID = UUID.randomUUID();

    @Mock private ClinicalServiceCatalogRepository serviceRepository;
    @Mock private ClinicalReferenceRangeRepository referenceRangeRepository;
    @Mock private ClinicalServiceAuditService auditService;
    @Spy private ClinicalServiceManagementResultMapper resultMapper = new ClinicalServiceManagementResultMapper();
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;

    @InjectMocks
    private ClinicalServiceManagementService service;

    @BeforeEach
    void setUp() {
        lenient().when(clockPort.now()).thenReturn(NOW);
        lenient().when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR);
    }

    @Test
    void createsClinicalService() {
        when(serviceRepository.existsByServiceCode("LAB-GLU")).thenReturn(false);
        when(serviceRepository.save(any(ClinicalServiceCatalog.class))).thenAnswer(inv -> inv.getArgument(0));

        ClinicalServiceManagementResult result = service.create(command());

        assertEquals("LAB-GLU", result.serviceCode());
        assertEquals("Blood glucose", result.serviceName());
        assertTrue(result.active());
        verify(auditService).record(ACTOR, ActionType.CREATE, result.id(), "Clinical service created: LAB-GLU", NOW);
    }

    @Test
    void rejectsDuplicateServiceCode() {
        when(serviceRepository.existsByServiceCode("LAB-GLU")).thenReturn(true);
        assertThrows(ClinicalServiceCodeAlreadyExistsException.class, () -> service.create(command()));
    }
    @Test
    void updatesClinicalService() {
        UUID id = UUID.randomUUID();
        ClinicalServiceCatalog catalog = catalog(id);
        when(serviceRepository.findById(id)).thenReturn(Optional.of(catalog));
        when(serviceRepository.save(catalog)).thenReturn(catalog);
        when(referenceRangeRepository.findByClinicalServiceId(id)).thenReturn(List.of());

        ClinicalServiceManagementResult result = service.update(id,
                new UpdateClinicalServiceCommand("Glucose", ClinicalServiceType.LAB_TEST, ClinicalResultDataType.NUMBER,
                        "mmol/L", "3.9-5.5", "Updated"));

        assertEquals("Glucose", result.serviceName());
        verify(auditService).record(ACTOR, ActionType.UPDATE, id, "Clinical service updated: LAB-GLU", NOW);
    }

    @Test
    void deactivatesClinicalService() {
        UUID id = UUID.randomUUID();
        ClinicalServiceCatalog catalog = catalog(id);
        when(serviceRepository.findById(id)).thenReturn(Optional.of(catalog));
        when(serviceRepository.save(catalog)).thenReturn(catalog);
        when(referenceRangeRepository.findByClinicalServiceId(id)).thenReturn(List.of());

        ClinicalServiceManagementResult result = service.updateStatus(id, false);

        assertFalse(result.active());
        verify(auditService).record(ACTOR, ActionType.DEACTIVATE, id, "Clinical service status changed: LAB-GLU", NOW);
    }

    @Test
    void activatesClinicalService() {
        UUID id = UUID.randomUUID();
        ClinicalServiceCatalog catalog = catalog(id);
        catalog.deactivate(NOW);
        when(serviceRepository.findById(id)).thenReturn(Optional.of(catalog));
        when(serviceRepository.save(catalog)).thenReturn(catalog);
        when(referenceRangeRepository.findByClinicalServiceId(id)).thenReturn(List.of());

        ClinicalServiceManagementResult result = service.updateStatus(id, true);

        assertTrue(result.active());
        verify(auditService).record(ACTOR, ActionType.ACTIVATE, id, "Clinical service status changed: LAB-GLU", NOW);
    }

    @Test
    void getByIdReportsNotFound() {
        UUID id = UUID.randomUUID();
        when(serviceRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ClinicalServiceCatalogNotFoundException.class, () -> service.getById(id));
    }

    @Test
    void searchBatchLoadsReferenceRangesInsteadOfOneQueryPerService() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        ClinicalServiceCatalog c1 = catalog(id1);
        ClinicalServiceCatalog c2 = catalog(id2);
        Page<ClinicalServiceCatalog> page = new PageImpl<>(List.of(c1, c2), PageRequest.of(0, 20), 2);
        ClinicalReferenceRange range1 = ClinicalReferenceRange.restore(UUID.randomUUID(), id1,
                Gender.MALE, 18, 64, new BigDecimal("5"), new BigDecimal("10"), true, NOW, null);

        when(serviceRepository.search(any(), any(), any())).thenReturn(page);
        when(referenceRangeRepository.findByClinicalServiceIdIn(any()))
                .thenReturn(List.of(range1));

        Page<ClinicalServiceManagementResult> result = service.search(null, null, PageRequest.of(0, 20));

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().get(0).referenceRanges().size());
        assertEquals(0, result.getContent().get(1).referenceRanges().size());

        verify(referenceRangeRepository).findByClinicalServiceIdIn(any());
        verify(referenceRangeRepository, never()).findByClinicalServiceId(any(UUID.class));
    }

    private CreateClinicalServiceCommand command() {
        return new CreateClinicalServiceCommand(SERVICE_CATALOG_ID, "LAB-GLU", "Blood glucose",
                ClinicalServiceType.LAB_TEST, ClinicalResultDataType.NUMBER, "mmol/L", "3.9-5.5", "Fasting glucose");
    }

    private ClinicalServiceCatalog catalog(UUID id) {
        return ClinicalServiceCatalog.restore(id, SERVICE_CATALOG_ID, "LAB-GLU", "Blood glucose",
                ClinicalServiceType.LAB_TEST, ClinicalResultDataType.NUMBER, "mmol/L", "3.9-5.5", "Fasting glucose",
                true, NOW, null);
    }
}
