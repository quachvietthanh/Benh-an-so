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
import com.benhsoan.port.dto.command.inventory.RejectProcurementPlanCommand;
import com.benhsoan.port.dto.result.ProcurementPlanResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.inventory.MedicationProcurementPlanRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RejectMedicationProcurementPlanServiceTest {

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

    private RejectMedicationProcurementPlanService service;
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
        service = new RejectMedicationProcurementPlanService(
                planRepository,
                resultMapper,
                currentUserPort,
                clockPort,
                auditLogRepository,
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
    @DisplayName("Ném lỗi SelfProcurementApprovalNotAllowedException khi người lập phiếu tự từ chối phiếu của mình (SoD)")
    void selfRejectNotAllowedThrowsException() {
        MedicationProcurementPlan plan = createPendingPlan();
        when(planRepository.findByIdForUpdate(planId)).thenReturn(Optional.of(plan));
        when(currentUserPort.getCurrentUserId()).thenReturn(creatorId); // Trùng creatorId!
        when(clockPort.now()).thenReturn(now);

        RejectProcurementPlanCommand command = new RejectProcurementPlanCommand(planId, "Lý do hợp lệ");

        assertThrows(SelfProcurementApprovalNotAllowedException.class, () -> service.reject(command));
    }

    @Test
    @DisplayName("Quản lý phòng khám từ chối thành công phiếu dự trù mua thuốc")
    void managerRejectSuccess() {
        MedicationProcurementPlan plan = createPendingPlan();
        when(planRepository.findByIdForUpdate(planId)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUserPort.getCurrentUserId()).thenReturn(managerId); // Khác creatorId!
        when(clockPort.now()).thenReturn(now);

        String reason = "Số lượng đề xuất vượt quá hạn mức ngân sách tháng.";
        RejectProcurementPlanCommand command = new RejectProcurementPlanCommand(planId, reason);

        ProcurementPlanResult result = service.reject(command);

        assertNotNull(result);
        assertEquals(ProcurementPlanStatus.REJECTED, result.status());
        assertEquals(managerId, result.approvedBy());
        assertEquals(reason, result.rejectionReason());
        verify(auditLogRepository).save(any());
    }
}
