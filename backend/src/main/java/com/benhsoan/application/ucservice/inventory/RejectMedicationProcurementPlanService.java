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
import com.benhsoan.port.dto.command.inventory.RejectProcurementPlanCommand;
import com.benhsoan.port.dto.result.ProcurementPlanResult;
import com.benhsoan.port.inbound.inventory.RejectMedicationProcurementPlanUseCase;
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
public class RejectMedicationProcurementPlanService implements RejectMedicationProcurementPlanUseCase {

    private final MedicationProcurementPlanRepository planRepository;
    private final MedicationProcurementResultMapper resultMapper;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    public ProcurementPlanResult reject(RejectProcurementPlanCommand command) {
        if (command == null || command.planId() == null) {
            throw new ValidationException("Mã định danh phiếu dự trù từ chối không được để trống.");
        }
        if (command.reason() == null || command.reason().trim().length() < 5) {
            throw new ValidationException("Lý do từ chối phiếu dự trù phải có ít nhất 5 ký tự.");
        }

        UUID actorId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();

        MedicationProcurementPlan plan = planRepository.findByIdForUpdate(command.planId())
                .orElseThrow(() -> new ProcurementPlanNotFoundException(command.planId()));

        // Quy tắc phân tách nhiệm vụ (Separation of Duties - SoD): Người lập phiếu không được tự từ chối phiếu của chính mình
        if (actorId != null && actorId.equals(plan.getCreatedBy())) {
            throw new SelfProcurementApprovalNotAllowedException();
        }

        plan.reject(actorId, command.reason(), now);
        MedicationProcurementPlan savedPlan = planRepository.save(plan);

        // Ghi nhận nhật ký kiểm toán (Audit Log)
        writeAuditLog(actorId, savedPlan, command.reason(), now);

        return resultMapper.toPlanResult(savedPlan);
    }

    private void writeAuditLog(UUID actorId, MedicationProcurementPlan plan, String reason, Instant now) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", "REJECT");
        payload.put("planId", plan.getId().toString());
        payload.put("planCode", plan.getPlanCode());
        payload.put("status", plan.getStatus().name());
        payload.put("rejectedBy", actorId != null ? actorId.toString() : null);
        payload.put("reason", reason);

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
