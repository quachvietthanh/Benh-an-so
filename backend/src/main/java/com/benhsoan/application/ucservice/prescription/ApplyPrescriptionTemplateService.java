package com.benhsoan.application.ucservice.prescription;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogNotFoundException;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.prescription.PrescriptionTemplate;
import com.benhsoan.domain.prescription.PrescriptionTemplateItem;
import com.benhsoan.domain.prescription.exception.PrescriptionTemplateNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.prescription.ApplyPrescriptionTemplateCommand;
import com.benhsoan.port.dto.command.prescription.CheckDrugInteractionCommand;
import com.benhsoan.port.dto.result.AppliedPrescriptionTemplateResult;
import com.benhsoan.port.dto.result.ContraindicationCheckResult;
import com.benhsoan.port.dto.result.DrugInteractionWarningResult;
import com.benhsoan.port.dto.result.PatientAllergyWarningResult;
import com.benhsoan.port.inbound.prescription.ApplyPrescriptionTemplateUseCase;
import com.benhsoan.port.inbound.prescription.CheckContraindicationUseCase;
import com.benhsoan.port.inbound.prescription.CheckDrugInteractionUseCase;
import com.benhsoan.port.inbound.prescription.CheckPatientDrugAllergyUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionTemplateRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

/**
 * NCL-05-CN-008 TC-02/TC-03: applies a template as a pre-populated draft. It never
 * persists a prescription and always reuses the existing interaction/allergy/
 * contraindication check services.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplyPrescriptionTemplateService implements ApplyPrescriptionTemplateUseCase {

    private static final String DISCONTINUED_REASON = "Thuốc đã ngừng sử dụng trong danh mục";
    private static final String MISSING_REASON = "Không tìm thấy thuốc trong danh mục";

    private final PrescriptionTemplateRepository templateRepository;
    private final DiagnosisCatalogRepository diagnosisCatalogRepository;
    private final MedicineRepository medicineRepository;
    private final CheckDrugInteractionUseCase checkDrugInteractionUseCase;
    private final CheckPatientDrugAllergyUseCase checkPatientDrugAllergyUseCase;
    private final CheckContraindicationUseCase checkContraindicationUseCase;
    private final CurrentUserPort currentUserPort;

    @Override
    public AppliedPrescriptionTemplateResult apply(ApplyPrescriptionTemplateCommand command) {
        requireCommand(command);
        authorizeDoctor();
        UUID currentUserId = currentUserPort.getCurrentUserId();

        PrescriptionTemplate template = templateRepository.findById(command.templateId())
                .orElseThrow(() -> new PrescriptionTemplateNotFoundException(command.templateId()));

        if (!Objects.equals(template.getCreatedBy(), currentUserId)) {
            throw new AccessDeniedException("Doctors may only apply their own prescription templates.");
        }

        DiagnosisCatalog diagnosis = diagnosisCatalogRepository.findById(template.getDiagnosisCatalogId())
                .orElseThrow(() -> new DiagnosisCatalogNotFoundException(template.getDiagnosisCatalogId()));

        List<AppliedPrescriptionTemplateResult.DraftItem> draftItems = new ArrayList<>();
        List<AppliedPrescriptionTemplateResult.SkippedItem> skippedItems = new ArrayList<>();
        List<UUID> activeMedicineIds = new ArrayList<>();

        for (PrescriptionTemplateItem item : template.getItems()) {
            Medicine medicine = medicineRepository.findById(item.getMedicineId()).orElse(null);
            if (medicine == null) {
                skippedItems.add(new AppliedPrescriptionTemplateResult.SkippedItem(
                        item.getMedicineId(), null, MISSING_REASON));
                continue;
            }
            if (!medicine.isActive()) {
                skippedItems.add(new AppliedPrescriptionTemplateResult.SkippedItem(
                        item.getMedicineId(), medicine.getMedicineName(), DISCONTINUED_REASON));
                continue;
            }
            activeMedicineIds.add(medicine.getId());
            draftItems.add(toDraftItem(item, medicine));
        }

        List<DrugInteractionWarningResult> interactions = checkDrugInteractionUseCase
                .check(new CheckDrugInteractionCommand(activeMedicineIds));
        List<PatientAllergyWarningResult> allergyWarnings = checkPatientDrugAllergyUseCase
                .check(command.medicalRecordId(), activeMedicineIds);
        ContraindicationCheckResult contraindication = checkContraindicationUseCase
                .check(command.medicalRecordId(), activeMedicineIds);

        return new AppliedPrescriptionTemplateResult(
                template.getId(),
                diagnosis.getCode(),
                diagnosis.getName(),
                List.copyOf(draftItems),
                List.copyOf(skippedItems),
                interactions,
                allergyWarnings,
                contraindication.warnings(),
                contraindication.missingData()
        );
    }

    private AppliedPrescriptionTemplateResult.DraftItem toDraftItem(
            PrescriptionTemplateItem item,
            Medicine medicine
    ) {
        return new AppliedPrescriptionTemplateResult.DraftItem(
                medicine.getId(),
                medicine.getMedicineCode(),
                medicine.getMedicineName(),
                medicine.getActiveIngredient(),
                medicine.getStrength(),
                medicine.getUnit(),
                item.getDosage(),
                item.getFrequency(),
                item.getRoute(),
                item.getDurationDays(),
                item.getQuantity(),
                item.getInstructions()
        );
    }

    private void authorizeDoctor() {
        if (!currentUserPort.hasRole("DOCTOR")) {
            throw new AccessDeniedException("Only doctors are allowed to apply prescription templates.");
        }
    }

    private void requireCommand(ApplyPrescriptionTemplateCommand command) {
        if (command == null || command.templateId() == null) {
            throw new ValidationException("Template id is required.");
        }
        if (command.medicalRecordId() == null) {
            throw new ValidationException("Medical record id is required.");
        }
    }
}

