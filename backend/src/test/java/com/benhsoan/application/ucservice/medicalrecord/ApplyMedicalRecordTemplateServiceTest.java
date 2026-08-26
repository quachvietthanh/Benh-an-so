package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordTemplate;
import com.benhsoan.domain.medicalrecord.MedicalRecordTemplateVersion.SectionDefinition;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordFieldCode;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAlreadyLockedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordInvalidVisitException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateInactiveException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateSpecialtyMismatchException;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.command.medicalrecord.ApplyMedicalRecordTemplateCommand;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordTemplateRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class ApplyMedicalRecordTemplateServiceTest {

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private MedicalRecordTemplateRepository templateRepository;
    @Mock private MedicalRecordAuthorizationService authorizationService;
    @Mock private MedicalRecordAccessAuditService accessAuditService;
    @Mock private MedicalRecordTemplateApplicationMapper templateMapper;
    @Mock private MedicalRecordResultMapper resultMapper;
    @Mock private ClockPort clockPort;

    @InjectMocks private ApplyMedicalRecordTemplateService service;

    private final UUID actorId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-08-26T08:00:00Z");

    @Test
    void appliesCurrentImmutableVersionToLockedMedicalRecord() {
        UUID specialtyId = UUID.randomUUID();
        Visit visit = visit(specialtyId);
        MedicalRecord record = emptyRecord(visit);
        MedicalRecordTemplate template = template(specialtyId, true);

        when(authorizationService.requireTemplateWriteAccess(record.getId())).thenReturn(actorId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(templateRepository.findByIdForUpdate(template.getId())).thenReturn(Optional.of(template));
        when(templateRepository.findBySpecialtyIdAndActive(specialtyId, true)).thenReturn(List.of(template));
        when(clockPort.now()).thenReturn(now);
        when(medicalRecordRepository.save(record)).thenReturn(record);

        service.apply(record.getId(), new ApplyMedicalRecordTemplateCommand(template.getId()));

        assertEquals(template.getCurrentVersion().getId(), record.getAppliedTemplateVersionId());
        assertEquals(actorId, record.getTemplateAppliedBy());
        assertEquals(now, record.getTemplateAppliedAt());
        verify(medicalRecordRepository).findByIdForUpdate(record.getId());
        verify(templateRepository).findByIdForUpdate(template.getId());
        verify(accessAuditService).recordRecordAccessInCurrentTransaction(
                any(), any(), any(), any(), eq(MedicalRecordAccessAction.TEMPLATE_APPLY), any(), any());
    }

    @Test
    void rejectsSignedRecordBeforeResolvingVisitOrTemplate() {
        Visit visit = visit(Specialty.GENERAL_ID);
        MedicalRecord record = MedicalRecord.create(visit.getId(), "Pain", null, null, null, null, null, null,
                "Conclusion", actorId, now);
        record.sign("signature", actorId, now);

        when(authorizationService.requireTemplateWriteAccess(record.getId())).thenReturn(actorId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));

        assertThrows(MedicalRecordAlreadyLockedException.class,
                () -> service.apply(record.getId(), new ApplyMedicalRecordTemplateCommand(UUID.randomUUID())));

        verify(medicalRecordRepository).findByIdForUpdate(record.getId());
        verifyNoInteractions(visitRepository, templateRepository);
    }

    @Test
    void rejectsInactiveVisitBeforeResolvingTemplate() {
        Visit visit = visit(Specialty.GENERAL_ID);
        visit.cancel(now);
        MedicalRecord record = emptyRecord(visit);

        when(authorizationService.requireTemplateWriteAccess(record.getId())).thenReturn(actorId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

        assertThrows(MedicalRecordInvalidVisitException.class,
                () -> service.apply(record.getId(), new ApplyMedicalRecordTemplateCommand(UUID.randomUUID())));

        verifyNoInteractions(templateRepository);
    }

    @Test
    void appliesGeneralDefaultAsDeterministicFallbackWhenSpecialtyHasNoActiveTemplate() {
        UUID visitSpecialtyId = UUID.randomUUID();
        Visit visit = visit(visitSpecialtyId);
        MedicalRecord record = emptyRecord(visit);
        MedicalRecordTemplate generalDefault = template(Specialty.GENERAL_ID, true);

        when(authorizationService.requireTemplateWriteAccess(record.getId())).thenReturn(actorId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(templateRepository.findByIdForUpdate(generalDefault.getId())).thenReturn(Optional.of(generalDefault));
        when(templateRepository.findBySpecialtyIdAndActive(visitSpecialtyId, true)).thenReturn(List.of());
        when(templateRepository.findBySpecialtyIdAndActive(Specialty.GENERAL_ID, true)).thenReturn(List.of(generalDefault));
        when(clockPort.now()).thenReturn(now);
        when(medicalRecordRepository.save(record)).thenReturn(record);

        service.apply(record.getId(), new ApplyMedicalRecordTemplateCommand(generalDefault.getId()));

        assertEquals(generalDefault.getCurrentVersion().getId(), record.getAppliedTemplateVersionId());
        verify(accessAuditService).recordRecordAccessInCurrentTransaction(
                any(), any(), any(), any(), eq(MedicalRecordAccessAction.TEMPLATE_APPLY),
                eq("Template applied: template=" + generalDefault.getId() + ", version="
                        + generalDefault.getCurrentVersion().getId() + ", specialty=" + Specialty.GENERAL_ID
                        + ", fallback=true"), eq(now));
    }

    @Test
    void rejectsTemplateWhoseSpecialtyDoesNotMatchVisitWhenSpecialtyTemplateExists() {
        UUID visitSpecialtyId = UUID.randomUUID();
        Visit visit = visit(visitSpecialtyId);
        MedicalRecord record = emptyRecord(visit);
        MedicalRecordTemplate selectedGeneral = template(Specialty.GENERAL_ID, true);
        MedicalRecordTemplate matchingSpecialty = template(visitSpecialtyId, true);

        when(authorizationService.requireTemplateWriteAccess(record.getId())).thenReturn(actorId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(templateRepository.findByIdForUpdate(selectedGeneral.getId())).thenReturn(Optional.of(selectedGeneral));
        when(templateRepository.findBySpecialtyIdAndActive(visitSpecialtyId, true)).thenReturn(List.of(matchingSpecialty));

        assertThrows(MedicalRecordTemplateSpecialtyMismatchException.class,
                () -> service.apply(record.getId(), new ApplyMedicalRecordTemplateCommand(selectedGeneral.getId())));

        verifyNoInteractions(accessAuditService);
    }

    @Test
    void rejectsInactiveTemplate() {
        Visit visit = visit(Specialty.GENERAL_ID);
        MedicalRecord record = emptyRecord(visit);
        MedicalRecordTemplate inactive = template(Specialty.GENERAL_ID, false);
        inactive.deactivate(2, null, actorId, now);

        when(authorizationService.requireTemplateWriteAccess(record.getId())).thenReturn(actorId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(templateRepository.findByIdForUpdate(inactive.getId())).thenReturn(Optional.of(inactive));

        assertThrows(MedicalRecordTemplateInactiveException.class,
                () -> service.apply(record.getId(), new ApplyMedicalRecordTemplateCommand(inactive.getId())));

        verifyNoInteractions(accessAuditService);
    }

    @Test
    void rejectsDoctorWhoIsNotResponsibleBeforeLoadingTemplate() {
        Visit visit = visit(Specialty.GENERAL_ID);
        MedicalRecord record = emptyRecord(visit);

        when(authorizationService.requireTemplateWriteAccess(record.getId())).thenReturn(actorId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        doThrow(new MedicalRecordAccessDeniedException()).when(authorizationService)
                .requireTemplateVisitAccess(actorId, visit.getDoctorId(), record.getId());

        assertThrows(MedicalRecordAccessDeniedException.class,
                () -> service.apply(record.getId(), new ApplyMedicalRecordTemplateCommand(UUID.randomUUID())));

        verifyNoInteractions(templateRepository, accessAuditService);
    }

    @Test
    void rejectsApplyingTemplateWhenRecordAlreadyHasClinicalContentEvenWithoutTemplate() {
        UUID visitSpecialtyId = UUID.randomUUID();
        Visit visit = visit(visitSpecialtyId);
        MedicalRecord record = MedicalRecord.create(visit.getId(), "Chest pain", null, null, null, null, null, null, null, actorId, now);
        MedicalRecordTemplate template = template(visitSpecialtyId, true);

        when(authorizationService.requireTemplateWriteAccess(record.getId())).thenReturn(actorId);
        when(medicalRecordRepository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(templateRepository.findByIdForUpdate(template.getId())).thenReturn(Optional.of(template));
        when(templateRepository.findBySpecialtyIdAndActive(visitSpecialtyId, true)).thenReturn(List.of(template));

        assertThrows(com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateChangeWithContentException.class,
                () -> service.apply(record.getId(), new ApplyMedicalRecordTemplateCommand(template.getId())));

        verifyNoInteractions(accessAuditService);
    }

    private Visit visit(UUID specialtyId) {
        return Visit.create("VS-0001", UUID.randomUUID(), actorId, null, null, specialtyId, VisitType.WALK_IN,
                now, "Exam", null, actorId, now);
    }

    private MedicalRecord emptyRecord(Visit visit) {
        return MedicalRecord.create(visit.getId(), null, null, null, null, null, null, null, null, actorId, now);
    }

    private MedicalRecordTemplate template(UUID specialtyId, boolean defaultTemplate) {
        return MedicalRecordTemplate.create(specialtyId, "Initial examination", defaultTemplate,
                List.of(new SectionDefinition(MedicalRecordFieldCode.CHIEF_COMPLAINT, "Reason", true, 1)),
                actorId, now);
    }
}
