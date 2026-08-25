package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogCodeAlreadyExistsException;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogDeletionNotAllowedException;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogInUseException;
import com.benhsoan.port.dto.command.medicalrecord.CreateDiagnosisCatalogCommand;
import com.benhsoan.port.dto.command.medicalrecord.UpdateDiagnosisCatalogCommand;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class DiagnosisCatalogManagementServicesTest {

    private static final Instant NOW = Instant.parse("2026-08-25T00:00:00Z");

    @Mock private DiagnosisCatalogRepository diagnosisCatalogRepository;
    @Mock private MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;
    @Mock private ClockPort clockPort;
    @Spy private DiagnosisCatalogResultMapper resultMapper = new DiagnosisCatalogResultMapper();
    @InjectMocks private CreateDiagnosisCatalogService createService;
    @InjectMocks private UpdateDiagnosisCatalogService updateService;
    @InjectMocks private UpdateDiagnosisCatalogStatusService updateStatusService;
    @InjectMocks private DeleteDiagnosisCatalogService deleteService;

    @BeforeEach
    void setUp() {
        lenient().when(clockPort.now()).thenReturn(NOW);
    }

    @Test
    void createsNormalizedCatalogEntry() {
        when(diagnosisCatalogRepository.existsByCode("J06.9")).thenReturn(false);
        when(diagnosisCatalogRepository.save(any(DiagnosisCatalog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = createService.create(new CreateDiagnosisCatalogCommand(
                " j06.9 ", "Nhiễm trùng hô hấp trên", "Hệ hô hấp", "Mô tả"
        ));

        assertEquals("J06.9", result.code());
        assertEquals("Hệ hô hấp", result.diseaseGroup());
    }

    @Test
    void rejectsDuplicateCodeBeforeSaving() {
        when(diagnosisCatalogRepository.existsByCode("J00")).thenReturn(true);

        assertThrows(DiagnosisCatalogCodeAlreadyExistsException.class, () -> createService.create(
                new CreateDiagnosisCatalogCommand("J00", "Cảm lạnh", "Hệ hô hấp", null)
        ));

        verify(diagnosisCatalogRepository, never()).save(any());
    }

    @Test
    void translatesDatabaseUniqueConflictToDuplicateCode() {
        when(diagnosisCatalogRepository.existsByCode("J00")).thenReturn(false);
        when(diagnosisCatalogRepository.save(any(DiagnosisCatalog.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(DiagnosisCatalogCodeAlreadyExistsException.class, () -> createService.create(
                new CreateDiagnosisCatalogCommand("J00", "Cảm lạnh", "Hệ hô hấp", null)
        ));
    }

    @Test
    void updatesNameGroupAndDescriptionWithoutChangingCode() {
        DiagnosisCatalog catalog = catalog(true);
        when(diagnosisCatalogRepository.findById(catalog.getId())).thenReturn(Optional.of(catalog));
        when(diagnosisCatalogRepository.save(catalog)).thenReturn(catalog);

        var result = updateService.update(new UpdateDiagnosisCatalogCommand(
                catalog.getId(), "Tên mới", "Nhóm mới", "Mô tả mới"
        ));

        assertEquals("J00", result.code());
        assertEquals("Tên mới", result.name());
        assertEquals("Nhóm mới", result.diseaseGroup());
    }

    @Test
    void changesCatalogStatus() {
        DiagnosisCatalog catalog = catalog(true);
        when(diagnosisCatalogRepository.findById(catalog.getId())).thenReturn(Optional.of(catalog));
        when(diagnosisCatalogRepository.save(catalog)).thenReturn(catalog);

        var result = updateStatusService.updateStatus(catalog.getId(), false);

        assertFalse(result.active());
        verify(diagnosisCatalogRepository).save(catalog);
    }

    @Test
    void rejectsDeletionWhenCatalogIsInUse() {
        DiagnosisCatalog catalog = catalog(true);
        when(diagnosisCatalogRepository.findById(catalog.getId())).thenReturn(Optional.of(catalog));
        when(medicalRecordDiagnosisRepository.existsByDiagnosisCatalogId(catalog.getId())).thenReturn(true);

        assertThrows(DiagnosisCatalogInUseException.class, () -> deleteService.delete(catalog.getId()));
    }

    @Test
    void rejectsDeletionEvenWhenCatalogIsUnused() {
        DiagnosisCatalog catalog = catalog(false);
        when(diagnosisCatalogRepository.findById(catalog.getId())).thenReturn(Optional.of(catalog));
        when(medicalRecordDiagnosisRepository.existsByDiagnosisCatalogId(catalog.getId())).thenReturn(false);

        assertThrows(DiagnosisCatalogDeletionNotAllowedException.class, () -> deleteService.delete(catalog.getId()));
    }

    private DiagnosisCatalog catalog(boolean active) {
        return DiagnosisCatalog.restore(
                UUID.randomUUID(), "J00", "Cảm lạnh", "Hệ hô hấp", null, active, NOW, null
        );
    }
}
