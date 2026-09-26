package com.benhsoan.application.ucservice.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.exception.MedicineNotFoundException;
import com.benhsoan.port.dto.command.inventory.CreateProcurementPlanItemCommand;
import com.benhsoan.port.dto.command.inventory.UpdateProcurementPlanCommand;
import com.benhsoan.port.dto.result.ProcurementPlanResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.inventory.MedicationProcurementPlanRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateMedicationProcurementPlanService Unit Tests (NCL-06-CN-012)")
class UpdateMedicationProcurementPlanServiceTest {

    @Mock
    private MedicationProcurementPlanRepository planRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private CurrentUserPort currentUserPort;

    @Mock
    private MedicationProcurementAuthorizer authorizer;

    @Mock
    private ClockPort clockPort;

    @Mock
    private AuditLogRepository auditLogRepository;

    private UpdateMedicationProcurementPlanService service;
    private UUID pharmacistId;
    private UUID planId;
    private UUID medicineId;
    private Instant now;

    @BeforeEach
    void setUp() {
        pharmacistId = UUID.randomUUID();
        planId = UUID.randomUUID();
        medicineId = UUID.randomUUID();
        now = Instant.now();

        MedicationProcurementResultMapper resultMapper = new MedicationProcurementResultMapper(medicineRepository);
        service = new UpdateMedicationProcurementPlanService(
                planRepository,
                medicineRepository,
                resultMapper,
                currentUserPort,
                authorizer,
                clockPort,
                auditLogRepository,
                new ObjectMapper()
        );
    }

    private MedicationProcurementPlan createDraftPlan() {
        MedicationProcurementItem item = MedicationProcurementItem.create(
                UUID.randomUUID(),
                planId,
                medicineId,
                20,
                50,
                30,
                60,
                70,
                "Ghi chú cũ",
                now
        );
        return MedicationProcurementPlan.createDraft(
                planId,
                "DT000001",
                pharmacistId,
                LocalDate.now().minusDays(30),
                LocalDate.now(),
                "Dự trù ban đầu",
                List.of(item),
                now
        );
    }

    @Test
    @DisplayName("Cập nhật phiếu dự trù thành công với batch load findAllById loại bỏ N+1 (P3-04)")
    void updatePlanSuccessWithBatchLoad() {
        MedicationProcurementPlan plan = createDraftPlan();
        Medicine mockMedicine = Medicine.restore(
                medicineId, "TH001", "Paracetamol", "Paracetamol", "500mg",
                com.benhsoan.domain.medicine.enums.DosageForm.TABLET, "Viên",
                com.benhsoan.domain.medicine.enums.AdministrationRoute.ORAL,
                true, now, null, 20, 50, false
        );

        when(planRepository.findByIdForUpdate(planId)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUserPort.getCurrentUserId()).thenReturn(pharmacistId);
        when(clockPort.now()).thenReturn(now);
        when(medicineRepository.findAllById(any())).thenReturn(List.of(mockMedicine));

        CreateProcurementPlanItemCommand itemCmd = new CreateProcurementPlanItemCommand(
                medicineId, 20, 50, 40, 70, 85, "Điều chỉnh tăng số lượng"
        );
        UpdateProcurementPlanCommand command = new UpdateProcurementPlanCommand(
                planId,
                "Ghi chú cập nhật",
                List.of(itemCmd)
        );

        ProcurementPlanResult result = service.update(command);

        assertNotNull(result);
        assertEquals(1, result.totalItems());
        assertEquals(85, result.totalProposedQuantity());
        verify(authorizer).requireCreatePermission();
        verify(medicineRepository).findAllById(any());
        verify(medicineRepository, never()).findById(any());
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("Ném MedicineNotFoundException khi thuốc trong danh sách cập nhật không tồn tại trong DB")
    void updatePlanThrowsMedicineNotFoundExceptionWhenMedicineMissing() {
        MedicationProcurementPlan plan = createDraftPlan();
        when(planRepository.findByIdForUpdate(planId)).thenReturn(Optional.of(plan));
        when(currentUserPort.getCurrentUserId()).thenReturn(pharmacistId);
        when(clockPort.now()).thenReturn(now);
        when(medicineRepository.findAllById(any())).thenReturn(List.of()); // Không tìm thấy!

        CreateProcurementPlanItemCommand itemCmd = new CreateProcurementPlanItemCommand(
                medicineId, 20, 50, 40, 70, 85, "Thuốc không tồn tại"
        );
        UpdateProcurementPlanCommand command = new UpdateProcurementPlanCommand(
                planId,
                "Ghi chú",
                List.of(itemCmd)
        );

        assertThrows(MedicineNotFoundException.class, () -> service.update(command));
    }

    @Test
    @DisplayName("Gửi duyệt phiếu dự trù thành công (chuyển sang PENDING_APPROVAL)")
    void submitPlanSuccess() {
        MedicationProcurementPlan plan = createDraftPlan();
        when(planRepository.findByIdForUpdate(planId)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUserPort.getCurrentUserId()).thenReturn(pharmacistId);
        when(clockPort.now()).thenReturn(now);

        ProcurementPlanResult result = service.submit(planId);

        assertNotNull(result);
        assertEquals(ProcurementPlanStatus.PENDING_APPROVAL, result.status());
        verify(authorizer).requireCreatePermission();
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("Hủy phiếu dự trù thành công (chuyển sang CANCELLED)")
    void cancelPlanSuccess() {
        MedicationProcurementPlan plan = createDraftPlan();
        when(planRepository.findByIdForUpdate(planId)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUserPort.getCurrentUserId()).thenReturn(pharmacistId);
        when(clockPort.now()).thenReturn(now);

        service.cancel(planId);

        assertEquals(ProcurementPlanStatus.CANCELLED, plan.getStatus());
        verify(authorizer).requireCreatePermission();
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("Người dùng không có quyền CREATE bị từ chối với AccessDeniedException khi update (P2-01)")
    void unauthorizedUserCannotUpdateThrowsAccessDeniedException() {
        org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("Từ chối quyền"))
                .when(authorizer).requireCreatePermission();

        UpdateProcurementPlanCommand command = new UpdateProcurementPlanCommand(planId, "Note", List.of());

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.update(command));
    }
}
