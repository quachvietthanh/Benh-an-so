package com.benhsoan.application.ucservice.prescription;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.druginteraction.DrugInteraction;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.PrescriptionWarningLog;
import com.benhsoan.domain.prescription.enums.WarningAction;
import com.benhsoan.domain.prescription.exception.PrescriptionInteractionConfirmationRequiredException;
import com.benhsoan.domain.prescription.exception.PrescriptionInteractionConfirmationRequiredException.InteractionWarning;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.prescription.CreatePrescriptionCommand;
import com.benhsoan.port.dto.command.prescription.CreatePrescriptionItemCommand;
import com.benhsoan.port.dto.command.prescription.PrescriptionInteractionOverrideCommand;
import com.benhsoan.port.dto.result.PrescriptionResult;
import com.benhsoan.port.inbound.prescription.CreatePrescriptionUseCase;
import com.benhsoan.port.outbound.generator.PrescriptionCodeGenerator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.druginteraction.DrugInteractionRepository;
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

    private final DrugInteractionRepository drugInteractionRepository;

    private final MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;

    private final PrescriptionWarningLogRepository warningLogRepository;

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

        List<DrugInteraction> interactions = drugInteractionRepository
                .findActiveInteractionsAmong(medicines.keySet());
        Map<UUID, String> overrideReasons = validateInteractionOverrides(
                interactions,
                command.interactionOverrides()
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

        saveAuditLog(saved, warningLogs.size(), currentUserId);

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
        Map<UUID, Medicine> medicines = new LinkedHashMap<>();

        for (CreatePrescriptionItemCommand item : itemCommands) {
            Medicine medicine = medicineRepository
                    .findById(item.medicineId())
                    .orElseThrow(() -> new ValidationException(
                            "Medicine not found: " + item.medicineId()
                    ));

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
                            command.route() == null
                                    ? medicine.getDefaultRoute()
                                    : command.route(),
                            command.durationDays(),
                            command.quantity(),
                            command.instructions(),
                            createdAt
                    );
                })
                .toList();
    }

    private Map<UUID, String> validateInteractionOverrides(
            List<DrugInteraction> interactions,
            List<PrescriptionInteractionOverrideCommand> overrideCommands
    ) {
        List<PrescriptionInteractionOverrideCommand> safeOverrideCommands
                = overrideCommands == null ? List.of() : overrideCommands;
        Map<UUID, String> reasonsByInteractionId = new HashMap<>();

        for (PrescriptionInteractionOverrideCommand override : safeOverrideCommands) {
            if (override == null || override.drugInteractionId() == null) {
                throw new ValidationException(
                        "Drug interaction id is required for an override."
                );
            }
            if (override.overrideReason() == null
                    || override.overrideReason().isBlank()) {
                throw new ValidationException(
                        "Override reason is required for drug interaction: "
                                + override.drugInteractionId()
                );
            }
            if (reasonsByInteractionId.put(
                    override.drugInteractionId(),
                    override.overrideReason().trim()
            ) != null) {
                throw new ValidationException(
                        "Duplicate override for drug interaction: "
                                + override.drugInteractionId()
                );
            }
        }

        Set<UUID> detectedInteractionIds = interactions.stream()
                .map(DrugInteraction::getId)
                .collect(java.util.stream.Collectors.toSet());

        for (UUID suppliedId : reasonsByInteractionId.keySet()) {
            if (!detectedInteractionIds.contains(suppliedId)) {
                throw new ValidationException(
                        "Override does not belong to a detected interaction: "
                                + suppliedId
                );
            }
        }

        List<DrugInteraction> unconfirmed = interactions.stream()
                .filter(interaction -> !reasonsByInteractionId
                        .containsKey(interaction.getId()))
                .toList();

        if (!unconfirmed.isEmpty()) {
            throw new PrescriptionInteractionConfirmationRequiredException(
                    unconfirmed.stream()
                            .map(this::toInteractionWarning)
                            .toList()
            );
        }

        return Map.copyOf(reasonsByInteractionId);
    }

    private InteractionWarning toInteractionWarning(
            DrugInteraction interaction
    ) {
        return new InteractionWarning(
                interaction.getId(),
                interaction.getFirstMedicineId(),
                interaction.getSecondMedicineId(),
                interaction.getSeverity(),
                interaction.getDescription(),
                interaction.getRecommendation()
        );
    }

    private List<PrescriptionWarningLog> saveWarningLogs(
            UUID prescriptionId,
            List<DrugInteraction> interactions,
            Map<UUID, String> overrideReasons,
            UUID handledBy,
            Instant handledAt
    ) {
        return interactions.stream()
                .map(interaction -> warningLogRepository.save(
                        PrescriptionWarningLog.create(
                                UUID.randomUUID(),
                                prescriptionId,
                                interaction.getId(),
                                interaction.getFirstMedicineId(),
                                interaction.getSecondMedicineId(),
                                interaction.getSeverity(),
                                buildWarningMessage(interaction),
                                WarningAction.OVERRIDDEN,
                                overrideReasons.get(interaction.getId()),
                                handledBy,
                                handledAt,
                                handledAt
                        )
                ))
                .toList();
    }

    private String buildWarningMessage(DrugInteraction interaction) {
        return "%s Recommendation: %s".formatted(
                interaction.getDescription(),
                interaction.getRecommendation()
        );
    }

    private void saveAuditLog(
            Prescription prescription,
            int warningOverrideCount,
            UUID currentUserId
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
                        null
                )
        );
    }

    private void requireCommand(CreatePrescriptionCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Create prescription command is required."
            );
        }
    }
}
