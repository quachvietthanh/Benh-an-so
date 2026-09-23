package com.benhsoan.application.ucservice.inventory;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.inventory.procurement.MedicationProcurementPlan;
import com.benhsoan.domain.inventory.procurement.exception.ProcurementPlanNotFoundException;
import com.benhsoan.domain.inventory.procurement.exception.SelfProcurementApprovalNotAllowedException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.inventory.ApproveProcurementPlanCommand;
import com.benhsoan.port.dto.result.ProcurementPlanResult;
import com.benhsoan.port.inbound.inventory.ApproveMedicationProcurementPlanUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.inventory.MedicationProcurementPlanRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ApproveMedicationProcurementPlanService implements ApproveMedicationProcurementPlanUseCase {

    private final MedicationProcurementPlanRepository planRepository;
    private final MedicationProcurementResultMapper resultMapper;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final AuditLogRepository auditLogRepository;
    private final MedicationProcurementAuditWriter auditWriter;
    private final ObjectMapper objectMapper;

    @Override
    public ProcurementPlanResult approve(ApproveProcurementPlanCommand command) {
        if (command == null || command.planId() == null) {
            throw new ValidationException("Mã định danh phiếu dự trù phê duyệt không được để trống.");
        }

        UUID actorId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();

        MedicationProcurementPlan plan = planRepository.findByIdForUpdate(command.planId())
                .orElseThrow(() -> new ProcurementPlanNotFoundException(command.planId()));

        // Quy tắc phân tách nhiệm vụ (Separation of Duties - SoD): Người lập phiếu không được tự duyệt
        if (actorId != null && actorId.equals(plan.getCreatedBy())) {
            auditWriter.writeSoDDenied(actorId, plan.getId(), plan.getPlanCode(), now);
            throw new SelfProcurementApprovalNotAllowedException();
        }

        plan.approve(actorId, now, command.approvedQuantities());
        MedicationProcurementPlan savedPlan = planRepository.save(plan);

        // Ghi nhận nhật ký kiểm toán (Audit Log)
        writeAuditLog(actorId, savedPlan, now);

        return resultMapper.toPlanResult(savedPlan);
    }

    private void writeAuditLog(UUID actorId, MedicationProcurementPlan plan, Instant now) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", "APPROVE");
        payload.put("planId", plan.getId().toString());
        payload.put("planCode", plan.getPlanCode());
        payload.put("status", plan.getStatus().name());
        payload.put("approvedBy", actorId != null ? actorId.toString() : null);
        payload.put("totalApprovedQuantity", plan.getTotalApprovedQuantity());

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            json = "{}";
        }

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.UPDATE,
                ResourceType.MEDICATION_PROCUREMENT_PLAN,
                plan.getId(),
                json,
                null,
                now
        ));
    }
}
