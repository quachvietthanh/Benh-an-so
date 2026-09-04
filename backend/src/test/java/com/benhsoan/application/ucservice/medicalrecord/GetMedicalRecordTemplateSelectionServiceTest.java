package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordTemplate;
import com.benhsoan.domain.medicalrecord.MedicalRecordTemplateVersion.SectionDefinition;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordFieldCode;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.result.MedicalRecordTemplateOptionResult;
import com.benhsoan.port.dto.result.MedicalRecordTemplateSelectionResult;
import com.benhsoan.port.dto.result.SpecialtyResult;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordTemplateRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetMedicalRecordTemplateSelectionService - Unit Tests")
class GetMedicalRecordTemplateSelectionServiceTest {

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private MedicalRecordTemplateRepository templateRepository;
    @Mock private MedicalRecordAuthorizationService authorizationService;
    @Mock private MedicalRecordAuthorizationAuditService authorizationAuditService;
    @Mock private MedicalRecordAccessAuditService accessAuditService;
    @Mock private MedicalRecordTemplateApplicationMapper templateMapper;
    @Mock private ClockPort clockPort;

    @InjectMocks private GetMedicalRecordTemplateSelectionService service;

    private final UUID actorId = UUID.randomUUID();
    private final UUID specialtyId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-08-26T08:00:00Z");

    @Test
    @DisplayName("getForVisit succeeds even when medical record does not exist yet for the visit")
    void getForVisitSucceedsWithoutExistingMedicalRecord() {
        Visit visit = Visit.create("VS-0001", UUID.randomUUID(), actorId, null, null, specialtyId,
                VisitType.WALK_IN, now, "Exam", null, actorId, now);
        Specialty specialty = Specialty.restore(specialtyId, "INTERNAL", "Internal Medicine", true, now, now);
        MedicalRecordTemplate template = MedicalRecordTemplate.create(specialtyId, "Internal Template", true,
                List.of(new SectionDefinition(MedicalRecordFieldCode.CHIEF_COMPLAINT, "Reason", true, 1)),
                actorId, now);
        SpecialtyResult specialtyResult = new SpecialtyResult(specialtyId, "INTERNAL", "Internal Medicine", true);
        MedicalRecordTemplateOptionResult optionResult = new MedicalRecordTemplateOptionResult(
                template.getId(), template.getCurrentVersion().getId(), specialtyResult, "Internal Template", 1, true, List.of());

        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(visit.getId())).thenReturn(Optional.empty());
        when(authorizationService.requireVisitTemplateReadAccess(visit.getId())).thenReturn(actorId);
        when(specialtyRepository.findById(specialtyId)).thenReturn(Optional.of(specialty));
        when(templateRepository.findBySpecialtyIdAndActive(specialtyId, true)).thenReturn(List.of(template));
        when(templateMapper.toSpecialty(specialty)).thenReturn(specialtyResult);
        when(templateMapper.toOption(template)).thenReturn(optionResult);

        MedicalRecordTemplateSelectionResult result = service.getForVisit(visit.getId());

        assertNull(result.medicalRecordId(), "medicalRecordId should be null when not yet created");
        assertEquals(visit.getId(), result.visitId());
        assertEquals(specialtyResult, result.visitSpecialty());
        assertFalse(result.fallback());
        assertEquals(1, result.availableTemplates().size());
        verify(authorizationService).requireVisitTemplateVisitAccess(actorId, visit.getDoctorId(), visit.getId());
        verifyNoInteractions(accessAuditService);
    }

    @Test
    @DisplayName("getForVisit succeeds and records view audit when medical record already exists")
    void getForVisitSucceedsWithExistingMedicalRecordAndAudits() {
        Visit visit = Visit.create("VS-0001", UUID.randomUUID(), actorId, null, null, specialtyId,
                VisitType.WALK_IN, now, "Exam", null, actorId, now);
        MedicalRecord record = MedicalRecord.create(visit.getId(), null, null, null, null, null, null, null, null, actorId, now);
        Specialty specialty = Specialty.restore(specialtyId, "INTERNAL", "Internal Medicine", true, now, now);
        MedicalRecordTemplate template = MedicalRecordTemplate.create(specialtyId, "Internal Template", true,
                List.of(new SectionDefinition(MedicalRecordFieldCode.CHIEF_COMPLAINT, "Reason", true, 1)),
                actorId, now);
        SpecialtyResult specialtyResult = new SpecialtyResult(specialtyId, "INTERNAL", "Internal Medicine", true);
        MedicalRecordTemplateOptionResult optionResult = new MedicalRecordTemplateOptionResult(
                template.getId(), template.getCurrentVersion().getId(), specialtyResult, "Internal Template", 1, true, List.of());

        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(visit.getId())).thenReturn(Optional.of(record));
        when(authorizationService.requireTemplateReadAccess(record.getId())).thenReturn(actorId);
        when(specialtyRepository.findById(specialtyId)).thenReturn(Optional.of(specialty));
        when(templateRepository.findBySpecialtyIdAndActive(specialtyId, true)).thenReturn(List.of(template));
        when(templateMapper.toSpecialty(specialty)).thenReturn(specialtyResult);
        when(templateMapper.toOption(template)).thenReturn(optionResult);
        when(clockPort.now()).thenReturn(now);

        MedicalRecordTemplateSelectionResult result = service.getForVisit(visit.getId());

        assertEquals(record.getId(), result.medicalRecordId());
        assertEquals(visit.getId(), result.visitId());
        verify(accessAuditService).recordRecordView(visit.getPatientId(), visit.getId(), record.getId(), actorId, now);
    }

    @Test
    @DisplayName("P1: In fallback mode, availableTemplates contains only the effective default template")
    void fallbackModeReturnsOnlyDefaultTemplateInAvailableList() {
        UUID emptySpecialtyId = UUID.randomUUID();
        Visit visit = Visit.create("VS-0002", UUID.randomUUID(), actorId, null, null, emptySpecialtyId,
                VisitType.WALK_IN, now, "Exam", null, actorId, now);
        Specialty specialty = Specialty.restore(emptySpecialtyId, "ENT", "Ear Nose Throat", true, now, now);
        Specialty generalSpecialty = Specialty.restore(Specialty.GENERAL_ID, "GENERAL", "General", true, now, now);

        MedicalRecordTemplate generalDefault = MedicalRecordTemplate.create(Specialty.GENERAL_ID, "General Initial", true,
                List.of(new SectionDefinition(MedicalRecordFieldCode.CHIEF_COMPLAINT, "Reason", true, 1)),
                actorId, now);
        MedicalRecordTemplate generalFollowUp = MedicalRecordTemplate.create(Specialty.GENERAL_ID, "General Follow-up", false,
                List.of(new SectionDefinition(MedicalRecordFieldCode.CHIEF_COMPLAINT, "Reason", true, 1)),
                actorId, now);

        SpecialtyResult specialtyResult = new SpecialtyResult(emptySpecialtyId, "ENT", "Ear Nose Throat", true);
        MedicalRecordTemplateOptionResult defaultOption = new MedicalRecordTemplateOptionResult(
                generalDefault.getId(), generalDefault.getCurrentVersion().getId(), specialtyResult, "General Initial", 1, true, List.of());

        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(visit.getId())).thenReturn(Optional.empty());
        when(authorizationService.requireVisitTemplateReadAccess(visit.getId())).thenReturn(actorId);
        when(specialtyRepository.findById(emptySpecialtyId)).thenReturn(Optional.of(specialty));
        when(templateRepository.findBySpecialtyIdAndActive(emptySpecialtyId, true)).thenReturn(List.of());
        when(templateRepository.findBySpecialtyIdAndActive(Specialty.GENERAL_ID, true)).thenReturn(List.of(generalDefault, generalFollowUp));
        when(templateMapper.toSpecialty(specialty)).thenReturn(specialtyResult);
        when(templateMapper.toOption(generalDefault)).thenReturn(defaultOption);

        MedicalRecordTemplateSelectionResult result = service.getForVisit(visit.getId());

        org.junit.jupiter.api.Assertions.assertTrue(result.fallback(), "Fallback flag must be true");
        assertEquals(1, result.availableTemplates().size(), "availableTemplates must contain only 1 default template in fallback mode");
        assertEquals(defaultOption, result.effectiveTemplate());
        assertEquals(defaultOption, result.availableTemplates().getFirst());
    }

    @Test
    @DisplayName("P2-B: Specialty without explicit default template selects first active template without crashing")
    void specialtyWithoutExplicitDefaultSelectsFirstActiveTemplateWithoutCrashing() {
        Visit visit = Visit.create("VS-0003", UUID.randomUUID(), actorId, null, null, specialtyId,
                VisitType.WALK_IN, now, "Exam", null, actorId, now);
        Specialty specialty = Specialty.restore(specialtyId, "INTERNAL", "Internal Medicine", true, now, now);

        MedicalRecordTemplate templateA = MedicalRecordTemplate.create(specialtyId, "Internal Template A", false,
                List.of(new SectionDefinition(MedicalRecordFieldCode.CHIEF_COMPLAINT, "Reason", true, 1)),
                actorId, now);
        MedicalRecordTemplate templateB = MedicalRecordTemplate.create(specialtyId, "Internal Template B", false,
                List.of(new SectionDefinition(MedicalRecordFieldCode.CHIEF_COMPLAINT, "Reason", true, 1)),
                actorId, now);

        SpecialtyResult specialtyResult = new SpecialtyResult(specialtyId, "INTERNAL", "Internal Medicine", true);
        MedicalRecordTemplateOptionResult optionA = new MedicalRecordTemplateOptionResult(
                templateA.getId(), templateA.getCurrentVersion().getId(), specialtyResult, "Internal Template A", 1, false, List.of());
        MedicalRecordTemplateOptionResult optionB = new MedicalRecordTemplateOptionResult(
                templateB.getId(), templateB.getCurrentVersion().getId(), specialtyResult, "Internal Template B", 1, false, List.of());

        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(visit.getId())).thenReturn(Optional.empty());
        when(authorizationService.requireVisitTemplateReadAccess(visit.getId())).thenReturn(actorId);
        when(specialtyRepository.findById(specialtyId)).thenReturn(Optional.of(specialty));
        when(templateRepository.findBySpecialtyIdAndActive(specialtyId, true)).thenReturn(List.of(templateA, templateB));
        when(templateMapper.toSpecialty(specialty)).thenReturn(specialtyResult);
        when(templateMapper.toOption(templateA)).thenReturn(optionA);
        when(templateMapper.toOption(templateB)).thenReturn(optionB);

        MedicalRecordTemplateSelectionResult result = service.getForVisit(visit.getId());

        assertFalse(result.fallback());
        assertEquals(2, result.availableTemplates().size());
        assertEquals(optionA, result.effectiveTemplate());
    }

    @Test
    @DisplayName("P2-A: General missing default template audits configuration failure and throws conflict")
    void generalMissingDefaultAuditsAndThrowsConflict() {
        UUID emptySpecialtyId = UUID.randomUUID();
        Visit visit = Visit.create("VS-0004", UUID.randomUUID(), actorId, null, null, emptySpecialtyId,
                VisitType.WALK_IN, now, "Exam", null, actorId, now);
        Specialty specialty = Specialty.restore(emptySpecialtyId, "ENT", "Ear Nose Throat", true, now, now);

        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(visit.getId())).thenReturn(Optional.empty());
        when(authorizationService.requireVisitTemplateReadAccess(visit.getId())).thenReturn(actorId);
        when(specialtyRepository.findById(emptySpecialtyId)).thenReturn(Optional.of(specialty));
        when(templateRepository.findBySpecialtyIdAndActive(emptySpecialtyId, true)).thenReturn(List.of());
        when(templateRepository.findBySpecialtyIdAndActive(Specialty.GENERAL_ID, true)).thenReturn(List.of());

        assertThrows(com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateDefaultNotConfiguredException.class,
                () -> service.getForVisit(visit.getId()));

        verify(authorizationAuditService).recordTemplateConfigurationFailure(
                org.mockito.ArgumentMatchers.eq(actorId),
                org.mockito.ArgumentMatchers.eq(visit.getId()),
                org.mockito.ArgumentMatchers.eq(com.benhsoan.domain.auditlog.enums.ResourceType.VISIT),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("P3: Unauthorized doctor on visit without record throws MedicalRecordAccessDeniedException")
    void unauthorizedDoctorOnVisitWithoutRecordAuditsVisitResourceType() {
        Visit visit = Visit.create("VS-0005", UUID.randomUUID(), UUID.randomUUID(), null, null, specialtyId,
                VisitType.WALK_IN, now, "Exam", null, actorId, now);

        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(visit.getId())).thenReturn(Optional.empty());
        when(authorizationService.requireVisitTemplateReadAccess(visit.getId())).thenReturn(actorId);
        org.mockito.Mockito.doThrow(new com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException())
                .when(authorizationService).requireVisitTemplateVisitAccess(actorId, visit.getDoctorId(), visit.getId());

        assertThrows(com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException.class,
                () -> service.getForVisit(visit.getId()));

        verify(authorizationService).requireVisitTemplateReadAccess(visit.getId());
        verify(authorizationService).requireVisitTemplateVisitAccess(actorId, visit.getDoctorId(), visit.getId());
    }

    @Test
    @DisplayName("getForVisit throws VisitNotFoundException when visit does not exist")
    void getForVisitThrowsWhenVisitNotFound() {
        UUID unknownVisitId = UUID.randomUUID();
        when(visitRepository.findById(unknownVisitId)).thenReturn(Optional.empty());

        assertThrows(VisitNotFoundException.class, () -> service.getForVisit(unknownVisitId));
    }
}
