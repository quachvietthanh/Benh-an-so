package com.benhsoan.application.ucservice.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.clinical.ClinicalReferenceRange;
import com.benhsoan.domain.clinical.ClinicalServiceCatalog;
import com.benhsoan.domain.clinical.enums.ClinicalResultDataType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;
import com.benhsoan.domain.clinical.exception.ClinicalReferenceRangeOverlapException;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.clinical.CreateClinicalReferenceRangeCommand;
import com.benhsoan.port.dto.command.clinical.UpdateClinicalReferenceRangeCommand;
import com.benhsoan.port.dto.result.ClinicalReferenceRangeResult;
import com.benhsoan.port.outbound.repository.clinical.ClinicalReferenceRangeRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalServiceCatalogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class ClinicalReferenceRangeServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T01:00:00Z");
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID SERVICE_ID = UUID.randomUUID();
    private static final UUID SERVICE_CATALOG_ID = UUID.randomUUID();

    @Mock private ClinicalServiceCatalogRepository serviceRepository;
    @Mock private ClinicalReferenceRangeRepository referenceRangeRepository;
    @Mock private ClinicalServiceAuditService auditService;
    @Spy private ClinicalServiceManagementResultMapper resultMapper = new ClinicalServiceManagementResultMapper();
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;

    @InjectMocks
    private ClinicalReferenceRangeService service;

    @BeforeEach
    void setUp() {
        lenient().when(clockPort.now()).thenReturn(NOW);
        lenient().when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR);
        lenient().when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service()));
    }

    @Test
    void createsReferenceRange() {
        when(referenceRangeRepository.findActiveByClinicalServiceId(SERVICE_ID)).thenReturn(List.of());
        when(referenceRangeRepository.save(any(ClinicalReferenceRange.class))).thenAnswer(inv -> inv.getArgument(0));

        ClinicalReferenceRangeResult result = service.create(SERVICE_ID,
                new CreateClinicalReferenceRangeCommand(Gender.MALE, 18, 64, new BigDecimal("5"), new BigDecimal("10")));

        assertEquals(Gender.MALE, result.gender());
        assertEquals(new BigDecimal("5"), result.lowerBound());
        verify(auditService).record(ACTOR, ActionType.CREATE, SERVICE_ID, "Reference range created: " + result.id(), NOW);
    }

    @Test
    void rejectsOverlappingCreate() {
        when(referenceRangeRepository.findActiveByClinicalServiceId(SERVICE_ID))
                .thenReturn(List.of(range(Gender.MALE, 18, 30, "5", "10")));

        assertThrows(ClinicalReferenceRangeOverlapException.class, () -> service.create(SERVICE_ID,
                new CreateClinicalReferenceRangeCommand(Gender.MALE, 25, 40, new BigDecimal("5"), new BigDecimal("10"))));
    }
    @Test
    void updateRejectsOverlapAgainstOtherRanges() {
        UUID rangeId = UUID.randomUUID();
        ClinicalReferenceRange existing = range(rangeId, Gender.MALE, 18, 30, "5", "10");
        when(referenceRangeRepository.findById(rangeId)).thenReturn(Optional.of(existing));
        when(referenceRangeRepository.findActiveByClinicalServiceId(SERVICE_ID))
                .thenReturn(List.of(range(UUID.randomUUID(), Gender.MALE, 35, 40, "5", "10")));

        assertThrows(ClinicalReferenceRangeOverlapException.class, () -> service.update(SERVICE_ID, rangeId,
                new UpdateClinicalReferenceRangeCommand(Gender.MALE, 30, 38, new BigDecimal("5"), new BigDecimal("10"))));
    }

    @Test
    void updateAllowsOverlapWithItself() {
        UUID rangeId = UUID.randomUUID();
        ClinicalReferenceRange existing = range(rangeId, Gender.MALE, 18, 30, "5", "10");
        when(referenceRangeRepository.findById(rangeId)).thenReturn(Optional.of(existing));
        when(referenceRangeRepository.findActiveByClinicalServiceId(SERVICE_ID)).thenReturn(List.of(existing));
        when(referenceRangeRepository.save(any(ClinicalReferenceRange.class))).thenAnswer(inv -> inv.getArgument(0));

        ClinicalReferenceRangeResult result = service.update(SERVICE_ID, rangeId,
                new UpdateClinicalReferenceRangeCommand(Gender.MALE, 18, 30, new BigDecimal("5"), new BigDecimal("12")));

        assertEquals(new BigDecimal("12"), result.upperBound());
    }

    @Test
    void rejectsRangeOfAnotherService() {
        UUID rangeId = UUID.randomUUID();
        UUID otherServiceId = UUID.randomUUID();
        ClinicalReferenceRange otherRange = ClinicalReferenceRange.restore(rangeId, otherServiceId, Gender.MALE,
                18, 30, new BigDecimal("5"), new BigDecimal("10"), true, NOW, null);
        when(referenceRangeRepository.findById(rangeId)).thenReturn(Optional.of(otherRange));

        assertThrows(ValidationException.class, () -> service.update(SERVICE_ID, rangeId,
                new UpdateClinicalReferenceRangeCommand(Gender.MALE, 18, 30, new BigDecimal("5"), new BigDecimal("10"))));
    }

    @Test
    void deactivatesReferenceRange() {
        UUID rangeId = UUID.randomUUID();
        ClinicalReferenceRange existing = range(rangeId, Gender.MALE, 18, 30, "5", "10");
        when(referenceRangeRepository.findById(rangeId)).thenReturn(Optional.of(existing));
        when(referenceRangeRepository.save(any(ClinicalReferenceRange.class))).thenAnswer(inv -> inv.getArgument(0));

        ClinicalReferenceRangeResult result = service.updateStatus(SERVICE_ID, rangeId, false);

        assertFalse(result.active());
        verify(auditService).record(ACTOR, ActionType.DEACTIVATE, SERVICE_ID,
                "Reference range status changed: " + rangeId, NOW);
    }

    @Test
    void activatingRangeRejectsOverlappingActiveRange() {
        // B [25,40] MALE was deactivated; A [18,64] MALE is now active. Re-activating B must fail.
        UUID rangeBId = UUID.randomUUID();
        ClinicalReferenceRange rangeB = ClinicalReferenceRange.restore(rangeBId, SERVICE_ID, Gender.MALE,
                25, 40, new BigDecimal("5"), new BigDecimal("10"), false, NOW, null);
        ClinicalReferenceRange rangeA = range(UUID.randomUUID(), Gender.MALE, 18, 64, "5", "10");

        when(referenceRangeRepository.findById(rangeBId)).thenReturn(Optional.of(rangeB));
        when(referenceRangeRepository.findActiveByClinicalServiceId(SERVICE_ID)).thenReturn(List.of(rangeA));

        assertThrows(ClinicalReferenceRangeOverlapException.class,
                () -> service.updateStatus(SERVICE_ID, rangeBId, true));

        verify(referenceRangeRepository, never()).save(any(ClinicalReferenceRange.class));
    }

    private ClinicalServiceCatalog service() {
        return ClinicalServiceCatalog.restore(SERVICE_ID, SERVICE_CATALOG_ID, "LAB-GLU", "Blood glucose",
                ClinicalServiceType.LAB_TEST, ClinicalResultDataType.NUMBER, "mmol/L", "3.9-5.5", "desc", true, NOW, null);
    }

    private ClinicalReferenceRange range(Gender gender, Integer min, Integer max, String lower, String upper) {
        return range(UUID.randomUUID(), gender, min, max, lower, upper);
    }

    private ClinicalReferenceRange range(UUID id, Gender gender, Integer min, Integer max, String lower, String upper) {
        return ClinicalReferenceRange.restore(id, SERVICE_ID, gender, min, max, dec(lower), dec(upper), true, NOW, null);
    }

    private static BigDecimal dec(String v) {
        return v == null ? null : new BigDecimal(v);
    }
}
