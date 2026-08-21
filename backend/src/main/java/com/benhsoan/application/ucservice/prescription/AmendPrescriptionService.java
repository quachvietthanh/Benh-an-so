package com.benhsoan.application.ucservice.prescription;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.prescription.snapshot.PrescriptionBusinessState;
import com.benhsoan.application.ucservice.prescription.snapshot.PrescriptionSnapshotMapper;
import com.benhsoan.application.ucservice.prescription.snapshot.PrescriptionSnapshotSerializer;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionAmendment;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.PrescriptionWarningLog;
import com.benhsoan.domain.prescription.enums.WarningAction;
import com.benhsoan.domain.prescription.exception.PrescriptionInteractionConfirmationRequiredException;
import com.benhsoan.domain.prescription.exception.PrescriptionInteractionConfirmationRequiredException.InteractionWarning;
import com.benhsoan.domain.prescription.exception.PrescriptionInvalidStatusException;
import com.benhsoan.domain.prescription.exception.PrescriptionNoChangesException;
import com.benhsoan.domain.prescription.exception.PrescriptionNotFoundException;
import com.benhsoan.domain.prescription.exception.UnauthorizedPrescriptionAmendmentException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.prescription.AmendPrescriptionCommand;
import com.benhsoan.port.dto.command.prescription.AmendPrescriptionItemCommand;
import com.benhsoan.port.dto.command.prescription.CheckDrugInteractionCommand;
import com.benhsoan.port.dto.command.prescription.PrescriptionInteractionOverrideCommand;
import com.benhsoan.port.dto.result.DrugInteractionWarningResult;
import com.benhsoan.port.dto.result.PrescriptionResult;
import com.benhsoan.port.inbound.prescription.AmendPrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.CheckDrugInteractionUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionAmendmentRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionWarningLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AmendPrescriptionService
        implements AmendPrescriptionUseCase {

    private final PrescriptionRepository prescriptionRepository;

    private final MedicineRepository medicineRepository;

    private final CheckDrugInteractionUseCase checkDrugInteractionUseCase;

    private final PrescriptionWarningLogRepository warningLogRepository;

    private final PrescriptionAmendmentRepository amendmentRepository;

    private final AuditLogRepository auditLogRepository;

    private final CurrentUserPort currentUserPort;

    private final PrescriptionResultMapper resultMapper;

    private final PrescriptionSnapshotMapper snapshotMapper;

    private final PrescriptionSnapshotSerializer snapshotSerializer;

    private final ClockPort clockPort;

    private final PrescriptionClinicalContextValidator clinicalContextValidator;

    @Override
    public PrescriptionResult amend(
            AmendPrescriptionCommand command
    ) {
        validateCommand(command);
        authorizeDoctor();

        UUID currentUserId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();
        Prescription prescription = loadForUpdate(command.prescriptionId());

        validateAmendmentAccess(prescription, currentUserId);
        clinicalContextValidator.requireEditableRecordForDoctor(
                prescription.getMedicalRecordId(),
                currentUserId
        );

        String beforeData = snapshotSerializer.serialize(
                snapshotMapper.toSnapshot(prescription)
        );
        PrescriptionBusinessState beforeBusinessState
                = snapshotMapper.toBusinessState(prescription);

        List<AmendPrescriptionItemCommand> itemCommands
                = validateItemCommands(command.items());
        List<PrescriptionItem> replacementItems = buildReplacementItems(
                prescription,
                itemCommands,
                now
        );

        Prescription candidate = Prescription.restore(
                prescription.getId(),
                prescription.getPrescriptionCode(),
                prescription.getMedicalRecordId(),
                prescription.getStatus(),
                command.note(),
                prescription.getPrescribedBy(),
                prescription.getPrescribedAt(),
                currentUserId,
                now,
                replacementItems
        );
        rejectNoBusinessChanges(beforeBusinessState, candidate);

        List<DrugInteractionWarningResult> interactions = findInteractions(
                replacementItems
        );
        Map<UUID, String> overrideReasons = validateInteractionOverrides(
                interactions,
                command.interactionOverrides()
        );

        prescription.replaceItems(
                replacementItems,
                command.note(),
                currentUserId,
                now
        );
        Prescription saved = prescriptionRepository.save(prescription);
        String afterData = snapshotSerializer.serialize(
                snapshotMapper.toSnapshot(saved)
        );

        List<PrescriptionWarningLog> warningLogs = saveWarningLogs(
                saved.getId(),
                interactions,
                overrideReasons,
                currentUserId,
                now
        );
        PrescriptionAmendment amendment = amendmentRepository.save(
                PrescriptionAmendment.create(
                        UUID.randomUUID(),
                        saved.getId(),
                        command.changeReason(),
                        beforeData,
                        afterData,
                        currentUserId,
                        now
                )
        );

        saveAuditLog(saved, amendment, warningLogs.size(), currentUserId);

        return resultMapper.toResult(saved, warningLogs);
    }

    private void validateCommand(AmendPrescriptionCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Amend prescription command is required."
            );
        }
        if (command.prescriptionId() == null) {
            throw new ValidationException("Prescription id is required.");
        }
        if (command.changeReason() == null
                || command.changeReason().isBlank()) {
            throw new ValidationException("Amendment reason is required.");
        }
    }

    private void authorizeDoctor() {
        if (!currentUserPort.hasRole("DOCTOR")) {
            throw new AccessDeniedException(
                    "Only doctors are allowed to amend prescriptions."
            );
        }
    }

    private Prescription loadForUpdate(UUID prescriptionId) {
        return prescriptionRepository.findByIdForUpdate(prescriptionId)
                .orElseThrow(() ->
                        new PrescriptionNotFoundException(prescriptionId));
    }

    private void validateAmendmentAccess(
            Prescription prescription,
            UUID currentUserId
    ) {
        if (!prescription.isPendingDispense()) {
            throw new PrescriptionInvalidStatusException(
                    "Only pending prescriptions can be amended."
            );
        }
        if (!Objects.equals(prescription.getPrescribedBy(), currentUserId)) {
            throw new UnauthorizedPrescriptionAmendmentException();
        }
    }

    private List<AmendPrescriptionItemCommand> validateItemCommands(
            List<AmendPrescriptionItemCommand> itemCommands
    ) {
        if (itemCommands == null || itemCommands.isEmpty()) {
            throw new ValidationException(
                    "An amended prescription must contain at least one medicine."
            );
        }

        Set<UUID> medicineIds = new HashSet<>();
        for (AmendPrescriptionItemCommand item : itemCommands) {
            if (item == null || item.medicineId() == null) {
                throw new ValidationException("Medicine id is required.");
            }
            if (!medicineIds.add(item.medicineId())) {
                throw new ValidationException(
                        "Duplicate medicine in amended prescription: "
                                + item.medicineId()
                );
            }
        }

        return List.copyOf(itemCommands);
    }

    private List<PrescriptionItem> buildReplacementItems(
            Prescription prescription,
            List<AmendPrescriptionItemCommand> itemCommands,
            Instant updatedAt
    ) {
        Map<UUID, PrescriptionItem> existingByMedicineId
                = new HashMap<>();
        for (PrescriptionItem item : prescription.getItems()) {
            existingByMedicineId.put(item.getMedicineId(), item);
        }

        Map<UUID, Medicine> medicines = loadActiveMedicines(itemCommands);

        return itemCommands.stream()
                .map(command -> {
                    PrescriptionItem existing = existingByMedicineId.get(
                            command.medicineId()
                    );
                    if (existing != null) {
                        return rebuildExistingItem(
                                existing,
                                command,
                                updatedAt
                        );
                    }
                    return createAddedItem(
                            prescription.getId(),
                            medicines.get(command.medicineId()),
                            command,
                            updatedAt
                    );
                })
                .toList();
    }

    private Map<UUID, Medicine> loadActiveMedicines(
            List<AmendPrescriptionItemCommand> itemCommands
    ) {
        Map<UUID, Medicine> medicines = new LinkedHashMap<>();

        for (AmendPrescriptionItemCommand item : itemCommands) {
            Medicine medicine = medicineRepository.findById(item.medicineId())
                    .orElseThrow(() -> new ValidationException(
                            "Medicine not found: " + item.medicineId()
                    ));
            if (!medicine.isActive()) {
                throw new ValidationException(
                        "Inactive medicine cannot remain in an amended prescription: "
                                + medicine.getId()
                );
            }
            medicines.put(medicine.getId(), medicine);
        }

        return medicines;
    }

    private PrescriptionItem rebuildExistingItem(
            PrescriptionItem existing,
            AmendPrescriptionItemCommand command,
            Instant updatedAt
    ) {
        return PrescriptionItem.restore(
                existing.getId(),
                existing.getPrescriptionId(),
                existing.getMedicineId(),
                existing.getMedicineName(),
                existing.getActiveIngredient(),
                existing.getStrength(),
                existing.getUnit(),
                command.dosage(),
                command.frequency(),
                command.route(),
                command.durationDays(),
                command.quantity(),
                command.instructions(),
                existing.getCreatedAt(),
                updatedAt
        );
    }

    private PrescriptionItem createAddedItem(
            UUID prescriptionId,
            Medicine medicine,
            AmendPrescriptionItemCommand command,
            Instant createdAt
    ) {
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
    }

    private void rejectNoBusinessChanges(
            PrescriptionBusinessState beforeBusinessState,
            Prescription candidate
    ) {
        PrescriptionBusinessState afterBusinessState
                = snapshotMapper.toBusinessState(candidate);
        if (beforeBusinessState.equals(afterBusinessState)) {
            throw new PrescriptionNoChangesException();
        }
    }

    private List<DrugInteractionWarningResult> findInteractions(
            List<PrescriptionItem> replacementItems
    ) {
        List<DrugInteractionWarningResult> interactions = checkDrugInteractionUseCase
                .check(new CheckDrugInteractionCommand(
                        replacementItems.stream()
                                .map(item -> item.getMedicineId())
                                .toList()
                ));
        return interactions == null ? List.of() : List.copyOf(interactions);
    }

    private Map<UUID, String> validateInteractionOverrides(
            List<DrugInteractionWarningResult> interactions,
            List<PrescriptionInteractionOverrideCommand> overrideCommands
    ) {
        List<PrescriptionInteractionOverrideCommand> safeOverrides
                = overrideCommands == null ? List.of() : overrideCommands;
        Map<UUID, String> reasonsByRuleId = new HashMap<>();

        for (PrescriptionInteractionOverrideCommand override : safeOverrides) {
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

        Set<UUID> detectedIds = interactions.stream()
                .map(interaction -> interaction.ruleId())
                .collect(java.util.stream.Collectors.toSet());
        for (UUID suppliedId : reasonsByRuleId.keySet()) {
            if (!detectedIds.contains(suppliedId)) {
                throw new ValidationException(
                        "Override does not belong to a detected interaction rule: "
                                + suppliedId
                );
            }
        }

        List<DrugInteractionWarningResult> unconfirmed = interactions.stream()
                .filter(interaction -> !reasonsByRuleId.containsKey(
                        interaction.ruleId()
                ))
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
            DrugInteractionWarningResult interaction
    ) {
        return new InteractionWarning(
                interaction.ruleId(),
                interaction.drugIdA(),
                interaction.drugIdB(),
                interaction.severity(),
                interaction.description(),
                interaction.clinicalRecommendation()
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
                .map(interaction -> warningLogRepository.save(
                        PrescriptionWarningLog.create(
                                UUID.randomUUID(),
                                prescriptionId,
                                interaction.ruleId(),
                                interaction.drugIdA(),
                                interaction.drugIdB(),
                                interaction.severity(),
                                buildWarningMessage(interaction),
                                WarningAction.OVERRIDDEN,
                                overrideReasons.get(interaction.ruleId()),
                                handledBy,
                                handledAt,
                                handledAt
                        )
                ))
                .toList();
    }

    private String buildWarningMessage(DrugInteractionWarningResult interaction) {
        return "%s Recommendation: %s".formatted(
                interaction.description(),
                interaction.clinicalRecommendation()
        );
    }

    private void saveAuditLog(
            Prescription prescription,
            PrescriptionAmendment amendment,
            int warningOverrideCount,
            UUID currentUserId
    ) {
        auditLogRepository.save(
                AuditLog.create(
                        currentUserId,
                        ActionType.UPDATE,
                        ResourceType.PRESCRIPTION,
                        prescription.getId(),
                        """
                        {
                        "prescriptionCode":"%s",
                        "amendmentId":"%s",
                        "itemCount":%d,
                        "warningOverrideCount":%d
                        }
                        """.formatted(
                                prescription.getPrescriptionCode(),
                                amendment.getId(),
                                prescription.getItems().size(),
                                warningOverrideCount
                        ),
                        null
                )
        );
    }
}
