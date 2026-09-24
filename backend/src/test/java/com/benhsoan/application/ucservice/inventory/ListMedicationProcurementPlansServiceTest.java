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
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.inventory.procurement.MedicationProcurementPlan;
import com.benhsoan.domain.inventory.procurement.enums.ProcurementPlanStatus;
import com.benhsoan.port.dto.query.inventory.ListProcurementPlansQuery;
import com.benhsoan.port.dto.result.ProcurementPlanSummaryResult;
import com.benhsoan.port.outbound.repository.inventory.MedicationProcurementPlanRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListMedicationProcurementPlansService Unit Tests (NCL-06-CN-012)")
class ListMedicationProcurementPlansServiceTest {

    @Mock
    private MedicationProcurementPlanRepository planRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private MedicationProcurementAuthorizer authorizer;

    private ListMedicationProcurementPlansService service;

    @BeforeEach
    void setUp() {
        MedicationProcurementResultMapper resultMapper = new MedicationProcurementResultMapper(medicineRepository);
        service = new ListMedicationProcurementPlansService(
                planRepository,
                resultMapper,
                authorizer
        );
    }

    @Test
    @DisplayName("Tra cứu danh sách phiếu dự trù thành công khi có quyền READ (P2-01)")
    void listPlansSuccess() {
        MedicationProcurementPlan plan = MedicationProcurementPlan.builder()
                .id(UUID.randomUUID())
                .planCode("DT000001")
                .status(ProcurementPlanStatus.DRAFT)
                .createdBy(UUID.randomUUID())
                .periodStartDate(LocalDate.now().minusDays(30))
                .periodEndDate(LocalDate.now())
                .items(List.of())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(planRepository.findAll(any(), any()))
                .thenReturn(new PageImpl<>(List.of(plan)));

        ListProcurementPlansQuery query = new ListProcurementPlansQuery(
                ProcurementPlanStatus.DRAFT, null, null, null, 0, 10
        );

        Page<ProcurementPlanSummaryResult> result = service.list(query);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("DT000001", result.getContent().get(0).planCode());
        verify(authorizer).requireReadPermission();
    }

    @Test
    @DisplayName("Người dùng không có quyền READ bị từ chối với AccessDeniedException từ Service Layer (P2-01)")
    void unauthorizedUserCannotListThrowsAccessDeniedException() {
        org.mockito.Mockito.doThrow(new AccessDeniedException("Từ chối quyền"))
                .when(authorizer).requireReadPermission();

        ListProcurementPlansQuery query = new ListProcurementPlansQuery(
                null, null, null, null, 0, 10
        );

        assertThrows(AccessDeniedException.class, () -> service.list(query));
    }
}
