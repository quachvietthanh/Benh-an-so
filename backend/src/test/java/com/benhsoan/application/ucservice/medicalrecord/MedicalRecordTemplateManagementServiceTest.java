package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

import com.benhsoan.domain.medicalrecord.MedicalRecordTemplate;
import com.benhsoan.domain.medicalrecord.MedicalRecordTemplateVersion.SectionDefinition;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordFieldCode;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateDefaultReplacementRequiredException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateInvalidReplacementException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateLastActiveException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateNameDuplicateException;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.port.dto.command.medicalrecord.CreateMedicalRecordTemplateCommand;
import com.benhsoan.port.dto.command.medicalrecord.MedicalRecordTemplateSectionCommand;
import com.benhsoan.port.dto.command.medicalrecord.UpdateMedicalRecordTemplateCommand;
import com.benhsoan.port.dto.command.medicalrecord.UpdateMedicalRecordTemplateStatusCommand;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordTemplateRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class MedicalRecordTemplateManagementServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-26T00:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID SPECIALTY_ID = UUID.randomUUID();

    @Mock private MedicalRecordTemplateRepository templateRepository;
    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;
    @Mock private MedicalRecordTemplateAuditWriter auditWriter;

    private final MedicalRecordTemplateCommandMapper commandMapper = new MedicalRecordTemplateCommandMapper();
    private final MedicalRecordTemplateResultMapper resultMapper = new MedicalRecordTemplateResultMapper();
    private Specialty specialty;

    @BeforeEach
    void setUp() {
        specialty = Specialty.restore(SPECIALTY_ID, "INTERNAL_MEDICINE", "Internal medicine", true, NOW, null);
    }

    @Test
    void createsInternalMedicineTemplateWithSixSectionsAndAuditsSuccess() {
        stubActorAndClock();
        stubSpecialty();
        when(templateRepository.findBySpecialtyIdAndActive(SPECIALTY_ID, true)).thenReturn(List.of());
        when(templateRepository.existsBySpecialtyIdAndNameKey(any(), any())).thenReturn(false);
        when(templateRepository.save(any(MedicalRecordTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = createService().create(new CreateMedicalRecordTemplateCommand(SPECIALTY_ID, "Internal initial", false,
                List.of(section(MedicalRecordFieldCode.CHIEF_COMPLAINT, 1), section(MedicalRecordFieldCode.SYMPTOMS, 2),
                        section(MedicalRecordFieldCode.MEDICAL_HISTORY, 3), section(MedicalRecordFieldCode.PHYSICAL_EXAMINATION, 4),
                        section(MedicalRecordFieldCode.CLINICAL_PROGRESS, 5), section(MedicalRecordFieldCode.CONCLUSION, 6))));

        assertEquals(1, result.currentVersionNo());
        assertEquals(6, result.sections().size());
        assertEquals(true, result.defaultTemplate());
        verify(auditWriter).writeCreated(any(), any(), any());
    }

    @Test
    void rejectsDuplicateNormalizedName() {
        stubActorAndClock();
        stubSpecialty();
        when(templateRepository.findBySpecialtyIdAndActive(SPECIALTY_ID, true)).thenReturn(List.of(template("Existing", false)));
        when(templateRepository.existsBySpecialtyIdAndNameKey(any(), any())).thenReturn(true);

        assertThrows(MedicalRecordTemplateNameDuplicateException.class,
                () -> createService().create(new CreateMedicalRecordTemplateCommand(SPECIALTY_ID, " existing ", false,
                        List.of(section(MedicalRecordFieldCode.CHIEF_COMPLAINT, 1)))));
        verify(templateRepository, never()).save(any());
    }

    @Test
    void updateCreatesVersionAndRejectsAnotherTemplatesName() {
        MedicalRecordTemplate template = template("Original", false);
        when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
        when(templateRepository.existsBySpecialtyIdAndNameKeyAndIdNot(SPECIALTY_ID, "duplicate", template.getId()))
                .thenReturn(true);

        assertThrows(MedicalRecordTemplateNameDuplicateException.class,
                () -> updateService().update(new UpdateMedicalRecordTemplateCommand(template.getId(), " Duplicate ",
                        List.of(section(MedicalRecordFieldCode.CHIEF_COMPLAINT, 1)), "revision")));
    }

    @Test
    void updateCreatesNewVersionAndWritesAudit() {
        stubActorAndClock();
        stubSpecialty();
        MedicalRecordTemplate template = template("Original", false);
        when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
        when(templateRepository.existsBySpecialtyIdAndNameKeyAndIdNot(any(), any(), any())).thenReturn(false);
        when(templateRepository.save(any(MedicalRecordTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = updateService().update(new UpdateMedicalRecordTemplateCommand(template.getId(), "Updated",
                List.of(section(MedicalRecordFieldCode.CHIEF_COMPLAINT, 1)), "revision"));

        assertEquals(2, result.currentVersionNo());
        verify(auditWriter).writeUpdated(any(), any(), any());
    }

    @Test
    void blocksDeactivationOfLastActiveTemplate() {
        MedicalRecordTemplate template = template("Only template", true);
        when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
        when(templateRepository.findBySpecialtyIdAndActive(SPECIALTY_ID, true)).thenReturn(List.of(template));

        assertThrows(MedicalRecordTemplateLastActiveException.class,
                () -> statusService().updateStatus(new UpdateMedicalRecordTemplateStatusCommand(template.getId(), false, null)));
    }

    @Test
    void blocksDefaultDeactivationWithoutReplacement() {
        MedicalRecordTemplate template = template("Default template", true);
        when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
        when(templateRepository.findBySpecialtyIdAndActive(SPECIALTY_ID, true)).thenReturn(List.of(template, template("Other", false)));

        assertThrows(MedicalRecordTemplateDefaultReplacementRequiredException.class,
                () -> statusService().updateStatus(new UpdateMedicalRecordTemplateStatusCommand(template.getId(), false, null)));
    }

    @Test
    void blocksReplacementFromAnotherSpecialtyOrInactiveReplacement() {
        MedicalRecordTemplate template = template("Default template", true);
        MedicalRecordTemplate otherSpecialty = MedicalRecordTemplate.create(UUID.randomUUID(), "Other specialty", false,
                List.of(definition(MedicalRecordFieldCode.CHIEF_COMPLAINT, 1)), ACTOR_ID, NOW);
        MedicalRecordTemplate inactive = template("Inactive replacement", false);
        inactive.deactivate(2, UUID.randomUUID(), ACTOR_ID, NOW.plusSeconds(1));
        when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
        when(templateRepository.findBySpecialtyIdAndActive(SPECIALTY_ID, true)).thenReturn(List.of(template, template("Other", false)));
        when(templateRepository.findById(otherSpecialty.getId())).thenReturn(Optional.of(otherSpecialty));
        when(templateRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

        assertThrows(MedicalRecordTemplateInvalidReplacementException.class,
                () -> statusService().updateStatus(new UpdateMedicalRecordTemplateStatusCommand(template.getId(), false,
                        otherSpecialty.getId())));
        assertThrows(MedicalRecordTemplateInvalidReplacementException.class,
                () -> statusService().updateStatus(new UpdateMedicalRecordTemplateStatusCommand(template.getId(), false,
                        inactive.getId())));
        verify(templateRepository, never()).deactivate(any(), any(), any(), any());
    }

    private CreateMedicalRecordTemplateService createService() {
        return new CreateMedicalRecordTemplateService(templateRepository, specialtyRepository, currentUserPort, clockPort,
                commandMapper, resultMapper, auditWriter);
    }

    private UpdateMedicalRecordTemplateService updateService() {
        return new UpdateMedicalRecordTemplateService(templateRepository, specialtyRepository, currentUserPort, clockPort,
                commandMapper, resultMapper, auditWriter);
    }

    private UpdateMedicalRecordTemplateStatusService statusService() {
        return new UpdateMedicalRecordTemplateStatusService(templateRepository, specialtyRepository, currentUserPort, clockPort,
                resultMapper, auditWriter);
    }

    private MedicalRecordTemplate template(String name, boolean defaultTemplate) {
        return MedicalRecordTemplate.create(SPECIALTY_ID, name, defaultTemplate,
                List.of(definition(MedicalRecordFieldCode.CHIEF_COMPLAINT, 1)), ACTOR_ID, NOW);
    }

    private void stubSpecialty() {
        when(specialtyRepository.findById(SPECIALTY_ID)).thenReturn(Optional.of(specialty));
    }

    private void stubActorAndClock() {
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
    }

    private MedicalRecordTemplateSectionCommand section(MedicalRecordFieldCode fieldCode, int order) {
        return new MedicalRecordTemplateSectionCommand(fieldCode, fieldCode.name(), true, order);
    }

    private SectionDefinition definition(MedicalRecordFieldCode fieldCode, int order) {
        return new SectionDefinition(fieldCode, fieldCode.name(), true, order);
    }
}
