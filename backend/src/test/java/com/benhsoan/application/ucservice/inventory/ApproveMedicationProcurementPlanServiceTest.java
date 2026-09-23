package com.benhsoan.application.ucservice.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.inventory.procurement.MedicationProcurementItem;
import com.benhsoan.domain.inventory.procurement.MedicationProcurementPlan;
import com.benhsoan.domain.inventory.procurement.enums.ProcurementPlanStatus;
import com.benhsoan.domain.inventory.procurement.exception.SelfProcurementApprovalNotAllowedException;
import com.benhsoan.port.dto.command.inventory.ApproveProcurementPlanCommand;
import com.benhsoan.port.dto.result.ProcurementPlanResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.inventory.MedicationProcurementPlanRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ApproveMedicationProcurementPlanServiceTest {

    @Mock
    private MedicationProcurementPlanRepository planRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private CurrentUserPort currentUserPort;

    @Mock
    private ClockPort clockPort;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private MedicationProcurementAuditWriter auditWriter;

    private ApproveMedicationProcurementPlanService service;
    private UUID creatorId;
    private UUID managerId;
    private UUID planId;
    private UUID medicineId;
    private Instant now;

    @BeforeEach
    void setUp() {
        creatorId = UUID.randomUUID();
        managerId = UUID.randomUUID();
        planId = UUID.randomUUID();
        medicineId = UUID.randomUUID();
        now = Instant.now();

        MedicationProcurementResultMapper resultMapper = new MedicationProcurementResultMapper(medicineRepository);
        service = new ApproveMedicationProcurementPlanService(
                planRepository,
                resultMapper,
                currentUserPort,
                clockPort,
                auditLogRepository,
                auditWriter,
                new ObjectMapper()
        );
    }

    private MedicationProcurementPlan createPendingPlan() {
        MedicationProcurementItem item = MedicationProcurementItem.create(
                UUID.randomUUID(),
                planId,
                medicineId,
                20,
                50,
                30,
                60,
                70,
                "Ghi chú",
                now
        );
        return MedicationProcurementPlan.createAndSubmit(
                planId,
                "DT000001",
                creatorId,
                LocalDate.now().minusDays(30),
                LocalDate.now(),
                "Dự trù",
                List.of(item),
                now
        );
    }

    @Test
    @DisplayName("Ném lỗi SelfProcurementApprovalNotAllowedException khi người lập phiếu tự duyệt phiếu của mình (SoD) và ghi audit log")
    void selfApprovalNotAllowedThrowsException() {
        MedicationProcurementPlan plan = createPendingPlan();
        when(planRepository.findByIdForUpdate(planId)).thenReturn(Optional.of(plan));
        when(currentUserPort.getCurrentUserId()).thenReturn(creatorId); // Trùng với creatorId!
        when(clockPort.now()).thenReturn(now);

        ApproveProcurementPlanCommand command = new ApproveProcurementPlanCommand(planId, "Tự duyệt", null);

        assertThrows(SelfProcurementApprovalNotAllowedException.class, () -> service.approve(command));
        verify(auditWriter).writeSoDDenied(creatorId, planId, "DT000001", now);
    }

    @Test
    @DisplayName("Quản lý phòng khám phê duyệt thành công phiếu dự trù mua thuốc")
    void managerApproveSuccess() {
        MedicationProcurementPlan plan = createPendingPlan();
        when(planRepository.findByIdForUpdate(planId)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUserPort.getCurrentUserId()).thenReturn(managerId); // Khác với creatorId!
        when(clockPort.now()).thenReturn(now);

        ApproveProcurementPlanCommand command = new ApproveProcurementPlanCommand(
                planId,
                "Đồng ý duyệt",
                Map.of(medicineId, 65)
        );

        ProcurementPlanResult result = service.approve(command);

        assertNotNull(result);
        assertEquals(ProcurementPlanStatus.APPROVED, result.status());
        assertEquals(managerId, result.approvedBy());
        assertEquals(65, result.totalApprovedQuantity());
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("Ném ValidationException khi điều chỉnh số lượng duyệt với giá trị null")
    void approveWithNullAdjustmentThrowsValidationException() {
        MedicationProcurementPlan plan = createPendingPlan();
        when(planRepository.findByIdForUpdate(planId)).thenReturn(Optional.of(plan));
        when(currentUserPort.getCurrentUserId()).thenReturn(managerId);
        when(clockPort.now()).thenReturn(now);

        Map<UUID, Integer> adjustments = new java.util.HashMap<>();
        adjustments.put(medicineId, null);

        ApproveProcurementPlanCommand command = new ApproveProcurementPlanCommand(
                planId,
                "Duyệt có null",
                adjustments
        );

        assertThrows(com.benhsoan.domain.shared.exception.ValidationException.class,
                () -> service.approve(command));
    }

    @Test
    @DisplayName("Ném ValidationException khi điều chỉnh số lượng duyệt với giá trị âm")
    void approveWithNegativeAdjustmentThrowsValidationException() {
        MedicationProcurementPlan plan = createPendingPlan();
        when(planRepository.findByIdForUpdate(planId)).thenReturn(Optional.of(plan));
        when(currentUserPort.getCurrentUserId()).thenReturn(managerId);
        when(clockPort.now()).thenReturn(now);

        ApproveProcurementPlanCommand command = new ApproveProcurementPlanCommand(
                planId,
                "Duyệt âm",
                Map.of(medicineId, -10)
        );

        assertThrows(com.benhsoan.domain.shared.exception.ValidationException.class,
                () -> service.approve(command));
    }
}
