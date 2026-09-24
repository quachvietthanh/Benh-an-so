package com.benhsoan.application.ucservice.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.inventory.procurement.MedicationProcurementPlan;
import com.benhsoan.domain.inventory.procurement.enums.ProcurementPlanStatus;
import com.benhsoan.domain.inventory.procurement.exception.ProcurementPlanNotFoundException;
import com.benhsoan.port.dto.result.ProcurementPlanResult;
import com.benhsoan.port.outbound.repository.inventory.MedicationProcurementPlanRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetMedicationProcurementPlanService Unit Tests (NCL-06-CN-012)")
class GetMedicationProcurementPlanServiceTest {

    @Mock
    private MedicationProcurementPlanRepository planRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private MedicationProcurementAuthorizer authorizer;

    private GetMedicationProcurementPlanService service;
    private UUID planId;
    private UUID creatorId;
    private Instant now;

    @BeforeEach
    void setUp() {
        planId = UUID.randomUUID();
        creatorId = UUID.randomUUID();
        now = Instant.now();

        MedicationProcurementResultMapper resultMapper = new MedicationProcurementResultMapper(medicineRepository);
        service = new GetMedicationProcurementPlanService(
                planRepository,
                resultMapper,
                authorizer
        );
    }

    private MedicationProcurementPlan createMockPlan() {
        return MedicationProcurementPlan.builder()
                .id(planId)
                .planCode("DT000001")
                .status(ProcurementPlanStatus.PENDING_APPROVAL)
                .createdBy(creatorId)
                .periodStartDate(LocalDate.now().minusDays(30))
                .periodEndDate(LocalDate.now())
                .note("Dự trù định kỳ")
                .items(List.of())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    @DisplayName("Lấy chi tiết phiếu dự trù theo ID thành công khi có quyền READ (P2-01)")
    void getByIdSuccess() {
        MedicationProcurementPlan plan = createMockPlan();
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        ProcurementPlanResult result = service.getById(planId);

        assertNotNull(result);
        assertEquals("DT000001", result.planCode());
        verify(authorizer).requireReadPermission();
    }

    @Test
    @DisplayName("Lấy chi tiết phiếu dự trù theo mã phiếu thành công khi có quyền READ (P2-01)")
    void getByPlanCodeSuccess() {
        MedicationProcurementPlan plan = createMockPlan();
        when(planRepository.findByPlanCode("DT000001")).thenReturn(Optional.of(plan));

        ProcurementPlanResult result = service.getByPlanCode("DT000001");

        assertNotNull(result);
        assertEquals(planId, result.id());
        verify(authorizer).requireReadPermission();
    }

    @Test
    @DisplayName("Ném ProcurementPlanNotFoundException khi không tìm thấy phiếu")
    void getByIdNotFoundThrowsException() {
        when(planRepository.findById(planId)).thenReturn(Optional.empty());

        assertThrows(ProcurementPlanNotFoundException.class, () -> service.getById(planId));
        verify(authorizer).requireReadPermission();
    }

    @Test
    @DisplayName("Người dùng không có quyền READ bị từ chối với AccessDeniedException từ Service Layer (P2-01)")
    void unauthorizedUserCannotGetByIdThrowsAccessDeniedException() {
        org.mockito.Mockito.doThrow(new AccessDeniedException("Từ chối quyền"))
                .when(authorizer).requireReadPermission();

        assertThrows(AccessDeniedException.class, () -> service.getById(planId));
    }
}
