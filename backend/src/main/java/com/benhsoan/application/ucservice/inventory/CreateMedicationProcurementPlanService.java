package com.benhsoan.application.ucservice.inventory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.inventory.procurement.MedicationProcurementItem;
import com.benhsoan.domain.inventory.procurement.MedicationProcurementPlan;
import com.benhsoan.domain.inventory.procurement.exception.ProcurementPlanEmptyItemsException;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.exception.MedicineNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.inventory.CreateProcurementPlanCommand;
import com.benhsoan.port.dto.command.inventory.CreateProcurementPlanItemCommand;
import com.benhsoan.port.dto.result.ProcurementPlanResult;
import com.benhsoan.port.inbound.inventory.CreateMedicationProcurementPlanUseCase;
import com.benhsoan.port.outbound.generator.MedicationProcurementCodeGenerator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.inventory.MedicationProcurementPlanRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateMedicationProcurementPlanService implements CreateMedicationProcurementPlanUseCase {

    private final MedicationProcurementPlanRepository planRepository;
    private final MedicineRepository medicineRepository;
    private final MedicationProcurementCodeGenerator codeGenerator;
    private final MedicationProcurementResultMapper resultMapper;
    private final CurrentUserPort currentUserPort;
    private final MedicationProcurementAuthorizer authorizer;
    private final ClockPort clockPort;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    public ProcurementPlanResult create(CreateProcurementPlanCommand command) {
        authorizer.requireCreatePermission();

        if (command == null) {
            throw new ValidationException("Dữ liệu yêu cầu tạo phiếu dự trù không được để trống.");
        }
        if (command.items() == null || command.items().isEmpty()) {
            throw new ProcurementPlanEmptyItemsException();
        }

        UUID actorId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();
        UUID planId = UUID.randomUUID();
        String planCode = codeGenerator.generate();

        List<UUID> medicineIds = command.items().stream()
                .map(item -> {
                    if (item.medicineId() == null) {
                        throw new ValidationException("Mã thuốc trong danh sách không được để trống.");
                    }
                    return item.medicineId();
                })
                .toList();

        Map<UUID, Medicine> medicineMap = medicineRepository.findAllById(medicineIds).stream()
                .collect(Collectors.toMap(Medicine::getId, java.util.function.Function.identity()));

        List<MedicationProcurementItem> items = new ArrayList<>();
        for (CreateProcurementPlanItemCommand itemCmd : command.items()) {
            if (!medicineMap.containsKey(itemCmd.medicineId())) {
                throw new MedicineNotFoundException(itemCmd.medicineId());
            }

            items.add(MedicationProcurementItem.create(
                    UUID.randomUUID(),
                    planId,
                    itemCmd.medicineId(),
                    itemCmd.currentStock(),
                    itemCmd.minStockThreshold(),
                    itemCmd.previousPeriodConsumption(),
                    itemCmd.suggestedQuantity(),
                    itemCmd.proposedQuantity(),
                    itemCmd.note(),
                    now
            ));
        }

        MedicationProcurementPlan plan;
        if (command.submitImmediately()) {
            plan = MedicationProcurementPlan.createAndSubmit(
                    planId,
                    planCode,
                    actorId,
                    command.periodStartDate(),
                    command.periodEndDate(),
                    command.note(),
                    items,
                    now
            );
        } else {
            plan = MedicationProcurementPlan.createDraft(
                    planId,
                    planCode,
                    actorId,
                    command.periodStartDate(),
                    command.periodEndDate(),
                    command.note(),
                    items,
                    now
            );
        }

        MedicationProcurementPlan savedPlan = planRepository.save(plan);

        // Ghi nhận nhật ký kiểm toán (Audit Log)
        writeAuditLog(actorId, savedPlan, now);

        return resultMapper.toPlanResult(savedPlan, medicineMap);
    }

    private void writeAuditLog(UUID actorId, MedicationProcurementPlan plan, Instant now) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", "CREATE");
        payload.put("planId", plan.getId().toString());
        payload.put("planCode", plan.getPlanCode());
        payload.put("status", plan.getStatus().name());
        payload.put("totalItems", plan.getTotalItems());
        payload.put("totalProposedQuantity", plan.getTotalProposedQuantity());

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            json = "{}";
        }

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.CREATE,
                ResourceType.MEDICATION_PROCUREMENT_PLAN,
                plan.getId(),
                json,
                null,
                now
        ));
    }
}
