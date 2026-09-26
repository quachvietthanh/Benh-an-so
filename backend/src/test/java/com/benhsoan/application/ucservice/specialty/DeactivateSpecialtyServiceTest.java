package com.benhsoan.application.ucservice.specialty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.medicalrecord.MedicalRecordTemplate;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.specialty.exception.CannotDeactivateDefaultSpecialtyException;
import com.benhsoan.domain.specialty.exception.SpecialtyInUseException;
import com.benhsoan.port.dto.command.specialty.DeactivateSpecialtyCommand;
import com.benhsoan.port.dto.result.SpecialtyResult;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordTemplateRepository;
import com.benhsoan.port.outbound.repository.specialty.DoctorSpecialtyRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class DeactivateSpecialtyServiceTest {

    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private DoctorSpecialtyRepository doctorSpecialtyRepository;
    @Mock private MedicalRecordTemplateRepository medicalRecordTemplateRepository;
    @Mock private ClockPort clockPort;
    @Mock private SpecialtyAuditService specialtyAuditService;

    private DeactivateSpecialtyService service;
    private final Instant now = Instant.parse("2026-09-18T10:00:00Z");

    @BeforeEach
    void setUp() {
        service = new DeactivateSpecialtyService(
                specialtyRepository,
                doctorSpecialtyRepository,
                medicalRecordTemplateRepository,
                clockPort,
                specialtyAuditService
        );
    }

    @Test
    void cannotDeactivateGeneralSpecialty() {
        Specialty general = Specialty.restore(Specialty.GENERAL_ID, "GENERAL", "General", true, now, now);
        when(specialtyRepository.findById(Specialty.GENERAL_ID)).thenReturn(Optional.of(general));

        DeactivateSpecialtyCommand command = new DeactivateSpecialtyCommand(Specialty.GENERAL_ID, true);

        assertThrows(CannotDeactivateDefaultSpecialtyException.class, () -> service.deactivate(command));
    }

    @Test
    void warnsWhenSpecialtyInUseWithoutConfirmation() {
        UUID specialtyId = UUID.randomUUID();
        Specialty specialty = Specialty.restore(specialtyId, "INTERNAL", "Nội", true, now, now);

        when(specialtyRepository.findById(specialtyId)).thenReturn(Optional.of(specialty));
        when(doctorSpecialtyRepository.countBySpecialtyId(specialtyId)).thenReturn(2L);
        when(medicalRecordTemplateRepository.findBySpecialtyIdAndActive(specialtyId, true))
                .thenReturn(List.of(mock(MedicalRecordTemplate.class)));

        DeactivateSpecialtyCommand command = new DeactivateSpecialtyCommand(specialtyId, false);

        SpecialtyInUseException ex = assertThrows(SpecialtyInUseException.class, () -> service.deactivate(command));
        assertEquals(2L, ex.getAssignedDoctorCount());
        assertEquals(1L, ex.getActiveTemplateCount());
    }

    @Test
    void deactivatesWhenSpecialtyInUseWithConfirmation() {
        UUID specialtyId = UUID.randomUUID();
        Specialty specialty = Specialty.restore(specialtyId, "INTERNAL", "Nội", true, now, now);

        when(specialtyRepository.findById(specialtyId)).thenReturn(Optional.of(specialty));
        when(doctorSpecialtyRepository.countBySpecialtyId(specialtyId)).thenReturn(2L);
        when(medicalRecordTemplateRepository.findBySpecialtyIdAndActive(specialtyId, true)).thenReturn(List.of());
        when(clockPort.now()).thenReturn(now.plusSeconds(3600));

        DeactivateSpecialtyCommand command = new DeactivateSpecialtyCommand(specialtyId, true);

        SpecialtyResult result = service.deactivate(command);

        assertFalse(result.active());
        verify(specialtyRepository).save(any(Specialty.class));
        verify(specialtyAuditService).record(eq(ActionType.DEACTIVATE), any(Specialty.class));
    }

    @Test
    void deactivatesUnusedSpecialtyWithoutConfirmation() {
        UUID specialtyId = UUID.randomUUID();
        Specialty specialty = Specialty.restore(specialtyId, "PEDIATRICS", "Nhi", true, now, now);

        when(specialtyRepository.findById(specialtyId)).thenReturn(Optional.of(specialty));
        when(doctorSpecialtyRepository.countBySpecialtyId(specialtyId)).thenReturn(0L);
        when(medicalRecordTemplateRepository.findBySpecialtyIdAndActive(specialtyId, true)).thenReturn(List.of());
        when(clockPort.now()).thenReturn(now.plusSeconds(3600));

        DeactivateSpecialtyCommand command = new DeactivateSpecialtyCommand(specialtyId, false);

        SpecialtyResult result = service.deactivate(command);

        assertFalse(result.active());
        verify(specialtyRepository).save(any(Specialty.class));
    }
}
