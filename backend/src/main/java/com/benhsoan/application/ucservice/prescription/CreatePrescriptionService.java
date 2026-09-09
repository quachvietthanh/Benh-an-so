package com.benhsoan.application.ucservice.prescription;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.PrescriptionWarningLog;
import com.benhsoan.domain.prescription.enums.WarningAction;
import com.benhsoan.domain.prescription.PrescriptionAllergyWarningLog;
import com.benhsoan.domain.prescription.exception.PrescriptionAllergyConfirmationRequiredException;
import com.benhsoan.domain.prescription.exception.PrescriptionInteractionConfirmationRequiredException;
import com.benhsoan.domain.prescription.exception.PrescriptionInteractionConfirmationRequiredException.InteractionWarning;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.prescription.CheckDrugInteractionCommand;
import com.benhsoan.port.dto.command.prescription.CreatePrescriptionCommand;
import com.benhsoan.port.dto.command.prescription.CreatePrescriptionItemCommand;
import com.benhsoan.port.dto.command.prescription.PrescriptionAllergyOverrideCommand;
import com.benhsoan.port.dto.command.prescription.PrescriptionInteractionOverrideCommand;
import com.benhsoan.port.dto.result.DrugInteractionWarningResult;
import com.benhsoan.port.dto.result.PatientAllergyWarningResult;
import com.benhsoan.port.dto.result.PrescriptionResult;
import com.benhsoan.port.inbound.prescription.CheckDrugInteractionUseCase;
import com.benhsoan.port.inbound.prescription.CheckPatientDrugAllergyUseCase;
import com.benhsoan.port.inbound.prescription.CreatePrescriptionUseCase;
import com.benhsoan.port.outbound.generator.PrescriptionCodeGenerator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionAllergyWarningLogRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionWarningLogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreatePrescriptionService
        implements CreatePrescriptionUseCase {

    private final PrescriptionRepository prescriptionRepository;

    private final MedicineRepository medicineRepository;

    private final CheckDrugInteractionUseCase checkDrugInteractionUseCase;

    private final CheckPatientDrugAllergyUseCase checkPatientDrugAllergyUseCase;

    private final MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;

    private final PrescriptionWarningLogRepository warningLogRepository;

    private final PrescriptionAllergyWarningLogRepository allergyWarningLogRepository;

    private final PrescriptionCodeGenerator prescriptionCodeGenerator;

    private final CurrentUserPort currentUserPort;

    private final PrescriptionResultMapper resultMapper;

    private final AuditLogRepository auditLogRepository;

    private final ClockPort clockPort;

    private final PrescriptionClinicalContextValidator clinicalContextValidator;

    @Override
    public PrescriptionResult create(
            CreatePrescriptionCommand command
    ) {
        requireCommand(command);
        authorizeDoctor();

        UUID currentUserId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();

        validateMedicalRecord(command.medicalRecordId(), currentUserId);
        List<CreatePrescriptionItemCommand> itemCommands
                = validateItemCommands(command.items());
        Map<UUID, Medicine> medicines = loadActiveMedicines(itemCommands);

        UUID prescriptionId = UUID.randomUUID();
        List<PrescriptionItem> items = createItems(
                prescriptionId,
                itemCommands,
                medicines,
                now
        );

        // Detect interactions with the V17 active-ingredient rule engine so the
        // save flow produces the same warnings as the real-time
        // POST /prescriptions/check-interactions endpoint.
        List<DrugInteractionWarningResult> interactions = checkDrugInteractionUseCase
                .check(new CheckDrugInteractionCommand(
                        List.copyOf(medicines.keySet())
                ));
        Map<UUID, String> overrideReasons = validateInteractionOverrides(
                interactions,
                command.interactionOverrides()
        );

        // Detect patient medication allergies (QTN-26, NCL-05-CN-004)
        List<PatientAllergyWarningResult> allergyWarnings = checkPatientDrugAllergyUseCase
                .check(command.medicalRecordId(), List.copyOf(medicines.keySet()));
        Map<AllergyKey, String> allergyOverrideReasons = validateAllergyOverrides(
                allergyWarnings,
                command.allergyOverrides()
        );

        Prescription prescription = Prescription.create(
                prescriptionId,
                prescriptionCodeGenerator.generate(),
                command.medicalRecordId(),
                command.note(),
                currentUserId,
                now,
                items
        );

        Prescription saved = prescriptionRepository.save(prescription);
        List<PrescriptionWarningLog> warningLogs = saveWarningLogs(
                saved.getId(),
                interactions,
                overrideReasons,
                currentUserId,
                now
        );
        List<PrescriptionAllergyWarningLog> allergyLogs = saveAllergyWarningLogs(
                saved.getId(),
                allergyWarnings,
                allergyOverrideReasons,
                currentUserId,
                now
        );

        saveAuditLog(saved, warningLogs.size() + allergyLogs.size(), currentUserId, now);

        return resultMapper.toResult(saved, warningLogs);
    }

    private void authorizeDoctor() {
        if (!currentUserPort.hasRole("DOCTOR")) {
            throw new AccessDeniedException(
                    "Only doctors are allowed to create prescriptions."
            );
        }
    }

    private void validateMedicalRecord(UUID medicalRecordId, UUID doctorId) {
        clinicalContextValidator.requireEditableRecordForDoctor(
                medicalRecordId,
                doctorId
        );

        if (!medicalRecordDiagnosisRepository
                .existsByMedicalRecordId(medicalRecordId)) {
            throw new ValidationException(
                    "A diagnosis is required before creating a prescription."
            );
        }
    }

    private List<CreatePrescriptionItemCommand> validateItemCommands(
            List<CreatePrescriptionItemCommand> itemCommands
    ) {
        if (itemCommands == null || itemCommands.isEmpty()) {
            throw new ValidationException(
                    "A prescription must contain at least one medicine."
            );
        }

        Set<UUID> medicineIds = new HashSet<>();
        for (CreatePrescriptionItemCommand item : itemCommands) {
            if (item == null || item.medicineId() == null) {
                throw new ValidationException("Medicine id is required.");
            }
            if (!medicineIds.add(item.medicineId())) {
                throw new ValidationException(
                        "Duplicate medicine in prescription: "
                                + item.medicineId()
                );
            }
        }

        return List.copyOf(itemCommands);
    }

    private Map<UUID, Medicine> loadActiveMedicines(
            List<CreatePrescriptionItemCommand> itemCommands
    ) {
        Map<UUID, Medicine> foundById = medicineRepository
                .findAllById(itemCommands.stream()
                        .map(CreatePrescriptionItemCommand::medicineId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(
                        Medicine::getId,
                        medicine -> medicine
                ));

        Map<UUID, Medicine> medicines = new LinkedHashMap<>();
        for (CreatePrescriptionItemCommand item : itemCommands) {
            Medicine medicine = foundById.get(item.medicineId());
            if (medicine == null) {
                throw new ValidationException(
                        "Medicine not found: " + item.medicineId()
                );
            }

            if (!medicine.isActive()) {
                throw new ValidationException(
                        "Inactive medicine cannot be prescribed: "
                                + medicine.getId()
                );
            }

            medicines.put(medicine.getId(), medicine);
        }

        return medicines;
    }

    private List<PrescriptionItem> createItems(
            UUID prescriptionId,
            List<CreatePrescriptionItemCommand> itemCommands,
            Map<UUID, Medicine> medicines,
            Instant createdAt
    ) {
        return itemCommands.stream()
                .map(command -> {
                    Medicine medicine = medicines.get(command.medicineId());

                    return PrescriptionItem.create(
                            UUID.randomUUID(),
                            prescriptionId,
                            medicine.getId(),
                            medicine.getMedicineName(),
                            medicine.getActiveIngredient(),
                            medicine.getStrength(),
                            medicine.getUnit(),
                            command.dosage(),
                            command.frequency(),
                            command.route(),
                            command.durationDays(),
                            command.quantity(),
                            command.instructions(),
                            createdAt
                    );
                })
                .toList();
    }

    private Map<UUID, String> validateInteractionOverrides(
            List<DrugInteractionWarningResult> interactions,
            List<PrescriptionInteractionOverrideCommand> overrideCommands
    ) {
        List<PrescriptionInteractionOverrideCommand> safeOverrideCommands
                = overrideCommands == null ? List.of() : overrideCommands;
        Map<UUID, String> reasonsByRuleId = new HashMap<>();

        for (PrescriptionInteractionOverrideCommand override : safeOverrideCommands) {
            if (override == null || override.ruleId() == null) {
                throw new ValidationException(
                        "Interaction rule id is required for an override."
                );
            }
            if (override.overrideReason() == null
                    || override.overrideReason().isBlank()) {
                throw new ValidationException(
                        "Override reason is required for interaction rule: "
                                + override.ruleId()
                );
            }
            if (reasonsByRuleId.put(
                    override.ruleId(),
                    override.overrideReason().trim()
            ) != null) {
                throw new ValidationException(
                        "Duplicate override for interaction rule: "
                                + override.ruleId()
                );
            }
        }

        Set<UUID> detectedRuleIds = interactions.stream()
                .map(DrugInteractionWarningResult::ruleId)
                .collect(java.util.stream.Collectors.toSet());

        for (UUID suppliedRuleId : reasonsByRuleId.keySet()) {
            if (!detectedRuleIds.contains(suppliedRuleId)) {
                throw new ValidationException(
                        "Override does not belong to a detected interaction: "
                                + suppliedRuleId
                );
            }
        }

        List<DrugInteractionWarningResult> unconfirmed = interactions.stream()
                .filter(warning -> !reasonsByRuleId
                        .containsKey(warning.ruleId()))
                .toList();

        if (!unconfirmed.isEmpty()) {
            throw new PrescriptionInteractionConfirmationRequiredException(
                    unconfirmed.stream()
                            .map(this::toInteractionWarning)
                            .toList()
            );
        }

        return Map.copyOf(reasonsByRuleId);
    }

    private InteractionWarning toInteractionWarning(
            DrugInteractionWarningResult warning
    ) {
        return new InteractionWarning(
                warning.ruleId(),
                warning.drugIdA(),
                warning.drugIdB(),
                warning.severity(),
                warning.description(),
                warning.clinicalRecommendation()
        );
    }

    private List<PrescriptionWarningLog> saveWarningLogs(
            UUID prescriptionId,
            List<DrugInteractionWarningResult> interactions,
            Map<UUID, String> overrideReasons,
            UUID handledBy,
            Instant handledAt
    ) {
        return interactions.stream()
                .map(warning -> warningLogRepository.save(
                        PrescriptionWarningLog.create(
                                UUID.randomUUID(),
                                prescriptionId,
                                warning.ruleId(),
                                warning.drugIdA(),
                                warning.drugIdB(),
                                warning.severity(),
                                warning.description(),
                                WarningAction.OVERRIDDEN,
                                overrideReasons.get(warning.ruleId()),
                                handledBy,
                                handledAt,
                                handledAt
                        )
                ))
                .toList();
    }

    private void saveAuditLog(
            Prescription prescription,
            int warningOverrideCount,
            UUID currentUserId,
            Instant issuedAt
    ) {
        auditLogRepository.save(
                AuditLog.create(
                        currentUserId,
                        ActionType.CREATE,
                        ResourceType.PRESCRIPTION,
                        prescription.getId(),
                        """
                        {
                        "prescriptionCode":"%s",
                        "medicalRecordId":"%s",
                        "itemCount":%d,
                        "warningOverrideCount":%d
                        }
                        """.formatted(
                                prescription.getPrescriptionCode(),
                                prescription.getMedicalRecordId(),
                                prescription.getItems().size(),
                                warningOverrideCount
                        ),
                        null,
                        issuedAt
                )
        );
    }

    private Map<AllergyKey, String> validateAllergyOverrides(
            List<PatientAllergyWarningResult> allergyWarnings,
            List<PrescriptionAllergyOverrideCommand> overrideCommands
    ) {
        List<PrescriptionAllergyOverrideCommand> safeOverrideCommands
                = overrideCommands == null ? List.of() : overrideCommands;
        Map<AllergyKey, String> reasonsByKey = new HashMap<>();

        for (PrescriptionAllergyOverrideCommand override : safeOverrideCommands) {
            if (override == null || override.allergyId() == null || override.medicineId() == null) {
                throw new ValidationException(
                        "Allergy ID and medicine ID are required for an allergy override."
                );
            }
            if (override.overrideReason() == null || override.overrideReason().isBlank()) {
                throw new ValidationException(
                        "Override reason is required for medication allergy: " + override.allergyId()
                );
            }
            AllergyKey key = new AllergyKey(override.allergyId(), override.medicineId());
            if (reasonsByKey.put(key, override.overrideReason().trim()) != null) {
                throw new ValidationException(
                        "Duplicate override for medication allergy: " + override.allergyId()
                );
            }
        }

        Set<AllergyKey> detectedKeys = allergyWarnings.stream()
                .map(w -> new AllergyKey(w.allergyId(), w.medicineId()))
                .collect(Collectors.toSet());

        for (AllergyKey suppliedKey : reasonsByKey.keySet()) {
            if (!detectedKeys.contains(suppliedKey)) {
                throw new ValidationException(
                        "Override does not belong to a detected medication allergy: "
                                + suppliedKey.allergyId()
                );
            }
        }

        List<PatientAllergyWarningResult> unconfirmed = allergyWarnings.stream()
                .filter(warning -> !reasonsByKey.containsKey(new AllergyKey(warning.allergyId(), warning.medicineId())))
                .toList();

        if (!unconfirmed.isEmpty()) {
            throw new PrescriptionAllergyConfirmationRequiredException(
                    unconfirmed.stream()
                            .map(this::toAllergyWarning)
                            .toList()
            );
        }

        return Map.copyOf(reasonsByKey);
    }

    private PrescriptionAllergyConfirmationRequiredException.AllergyWarning toAllergyWarning(
            PatientAllergyWarningResult warning
    ) {
        return new PrescriptionAllergyConfirmationRequiredException.AllergyWarning(
                warning.allergyId(),
                warning.patientId(),
                warning.medicineId(),
                warning.medicineName(),
                warning.activeIngredient(),
                warning.allergenName(),
                warning.severity(),
                warning.reaction()
        );
    }

    private List<PrescriptionAllergyWarningLog> saveAllergyWarningLogs(
            UUID prescriptionId,
            List<PatientAllergyWarningResult> allergyWarnings,
            Map<AllergyKey, String> overrideReasons,
            UUID handledBy,
            Instant handledAt
    ) {
        return allergyWarnings.stream()
                .map(warning -> allergyWarningLogRepository.save(
                        PrescriptionAllergyWarningLog.create(
                                UUID.randomUUID(),
                                prescriptionId,
                                warning.patientId(),
                                warning.allergyId(),
                                warning.medicineId(),
                                warning.activeIngredient(),
                                warning.allergenName(),
                                warning.severity(),
                                warning.reaction(),
                                overrideReasons.get(new AllergyKey(warning.allergyId(), warning.medicineId())),
                                handledBy,
                                handledAt,
                                handledAt
                        )
                ))
                .toList();
    }

    private record AllergyKey(UUID allergyId, UUID medicineId) {
    }

    private void requireCommand(CreatePrescriptionCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Create prescription command is required."
            );
        }
    }
}
