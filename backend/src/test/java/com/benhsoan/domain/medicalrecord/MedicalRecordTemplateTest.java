package com.benhsoan.domain.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.medicalrecord.MedicalRecordTemplateVersion.SectionDefinition;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordFieldCode;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateException;
import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.ValidationException;

class MedicalRecordTemplateTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-26T00:00:00Z");
    private static final UUID SPECIALTY_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();

    @Test
    void createBuildsVersionOneAndSortsSections() {
        MedicalRecordTemplate template = MedicalRecordTemplate.create(SPECIALTY_ID, "  General examination  ", false,
                List.of(section(MedicalRecordFieldCode.CONCLUSION, "Conclusion", false, 2),
                        section(MedicalRecordFieldCode.CHIEF_COMPLAINT, "Chief complaint", true, 1)),
                ADMIN_ID, CREATED_AT);

        assertEquals("General examination", template.getName());
        assertEquals("general examination", template.getNameKey());
        assertEquals(1, template.getCurrentVersionNo());
        assertEquals(1, template.getVersions().size());
        assertEquals(MedicalRecordFieldCode.CHIEF_COMPLAINT,
                template.getCurrentVersion().getSections().getFirst().getFieldCode());
    }

    @Test
    void updateCreatesImmutableNewVersion() {
        MedicalRecordTemplate template = template(false);
        MedicalRecordTemplateVersion original = template.getCurrentVersion();

        template.update("Follow-up examination",
                List.of(section(MedicalRecordFieldCode.CHIEF_COMPLAINT, "Updated chief complaint", true, 1)),
                "Clarify wording", ADMIN_ID, CREATED_AT.plusSeconds(60));

        assertEquals(2, template.getCurrentVersionNo());
        assertEquals(2, template.getVersions().size());
        assertEquals("General examination", original.getTemplateName());
        assertEquals("Chief complaint", original.getSections().getFirst().getLabel());
        assertEquals("Follow-up examination", template.getCurrentVersion().getTemplateName());
        assertEquals("Updated chief complaint", template.getCurrentVersion().getSections().getFirst().getLabel());
    }

    @Test
    void defaultAndDeactivationRulesAreEnforced() {
        MedicalRecordTemplate template = template(true);

        MedicalRecordTemplateException lastActive = assertThrows(MedicalRecordTemplateException.class,
                () -> template.deactivate(1, UUID.randomUUID(), ADMIN_ID, CREATED_AT.plusSeconds(60)));
        assertEquals(DomainErrorCode.MEDICAL_RECORD_TEMPLATE_LAST_ACTIVE, lastActive.getCode());

        MedicalRecordTemplateException missingReplacement = assertThrows(MedicalRecordTemplateException.class,
                () -> template.deactivate(2, null, ADMIN_ID, CREATED_AT.plusSeconds(60)));
        assertEquals(DomainErrorCode.MEDICAL_RECORD_TEMPLATE_DEFAULT_REPLACEMENT_REQUIRED,
                missingReplacement.getCode());

        template.deactivate(2, UUID.randomUUID(), ADMIN_ID, CREATED_AT.plusSeconds(60));
        assertFalse(template.isActive());
        assertFalse(template.isDefaultTemplate());
        MedicalRecordTemplateException inactiveDefault = assertThrows(MedicalRecordTemplateException.class,
                () -> template.setDefault(ADMIN_ID, CREATED_AT.plusSeconds(120)));
        assertEquals(DomainErrorCode.MEDICAL_RECORD_TEMPLATE_INACTIVE, inactiveDefault.getCode());
    }

    @Test
    void rejectsDuplicateFieldCodeAndDisplayOrder() {
        assertThrows(ValidationException.class, () -> MedicalRecordTemplate.create(SPECIALTY_ID, "General examination", false,
                List.of(section(MedicalRecordFieldCode.CHIEF_COMPLAINT, "Chief complaint", true, 1),
                        section(MedicalRecordFieldCode.CHIEF_COMPLAINT, "Duplicate", false, 2)), ADMIN_ID, CREATED_AT));
        assertThrows(ValidationException.class, () -> MedicalRecordTemplate.create(SPECIALTY_ID, "General examination", false,
                List.of(section(MedicalRecordFieldCode.CHIEF_COMPLAINT, "Chief complaint", true, 1),
                        section(MedicalRecordFieldCode.SYMPTOMS, "Symptoms", false, 1)), ADMIN_ID, CREATED_AT));
    }

    private MedicalRecordTemplate template(boolean makeDefault) {
        return MedicalRecordTemplate.create(SPECIALTY_ID, "General examination", makeDefault,
                List.of(section(MedicalRecordFieldCode.CHIEF_COMPLAINT, "Chief complaint", true, 1)), ADMIN_ID,
                CREATED_AT);
    }

    private SectionDefinition section(MedicalRecordFieldCode fieldCode, String label, boolean required, int order) {
        return new SectionDefinition(fieldCode, label, required, order);
    }
}
