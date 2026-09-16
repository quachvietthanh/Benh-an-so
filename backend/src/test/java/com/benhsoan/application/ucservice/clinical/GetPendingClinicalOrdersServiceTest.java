package com.benhsoan.application.ucservice.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;
import com.benhsoan.port.dto.command.clinical.GetPendingClinicalOrdersQuery;
import com.benhsoan.port.dto.result.PendingClinicalOrderResult;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetPendingClinicalOrdersService - Unit Tests (NCL-04-CN-008: TC-01)")
class GetPendingClinicalOrdersServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T10:00:00Z");

    @Mock
    private ClinicalOrderItemRepository clinicalOrderItemRepository;
    @Mock
    private ClinicalOrderAuthorizationService authorizationService;
    @Mock
    private ClinicalOrderAuditService auditService;
    @Mock
    private ClockPort clockPort;

    @InjectMocks
    private GetPendingClinicalOrdersService service;

    @Test
    @DisplayName("TC-01: Bác sĩ xem danh sách chỉ định chờ kết quả, tự động giới hạn phạm vi theo actorId khi không phải ADMIN")
    void tc01_getPendingOrdersScopedToDoctorWhenNonAdmin() {
        UUID actorDoctorId = UUID.randomUUID();
        UUID otherDoctorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Instant orderedAt = NOW.minusSeconds(45 * 60);

        PendingClinicalOrderResult itemResult = new PendingClinicalOrderResult(
                UUID.randomUUID(), UUID.randomUUID(), "ORD-001", UUID.randomUUID(), "VIS-001",
                patientId, "BN-001", "Nguyễn Văn A", actorDoctorId, "BS. Trần B",
                UUID.randomUUID(), "XRAY-01", "Chụp X-quang phổi", ClinicalServiceType.IMAGING,
                "Chụp đứng", "Ho kéo dài", ClinicalOrderItemStatus.PENDING, orderedAt, 45L);

        when(authorizationService.requireReadAccess()).thenReturn(actorDoctorId);
        when(authorizationService.isAdmin()).thenReturn(false);
        when(clockPort.now()).thenReturn(NOW);
        when(clinicalOrderItemRepository.findPendingOrders(
                eq(patientId),
                eq(actorDoctorId), // Must be scoped to actorDoctorId, not otherDoctorId!
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                eq(NOW),
                eq(PageRequest.of(0, 10)))).thenReturn(new PageImpl<>(List.of(itemResult)));

        GetPendingClinicalOrdersQuery query = new GetPendingClinicalOrdersQuery(patientId, otherDoctorId, null, null, 0,
                10);
        var result = service.getPendingOrders(query);

        assertEquals(1, result.getTotalElements());
        var item = result.getContent().getFirst();
        assertEquals("ORD-001", item.orderCode());
        assertEquals("Nguyễn Văn A", item.patientFullName());
        assertEquals("Chụp X-quang phổi", item.serviceName());
        assertEquals(45L, item.waitingMinutes());

        verify(auditService).recordViewPending(actorDoctorId, NOW);
    }

    @Test
    @DisplayName("ADMIN xem danh sách chỉ định chờ kết quả có thể lọc theo doctorId bất kỳ")
    void adminCanFilterByAnyDoctorId() {
        UUID adminId = UUID.randomUUID();
        UUID targetDoctorId = UUID.randomUUID();

        when(authorizationService.requireReadAccess()).thenReturn(adminId);
        when(authorizationService.isAdmin()).thenReturn(true);
        when(clockPort.now()).thenReturn(NOW);
        when(clinicalOrderItemRepository.findPendingOrders(
                org.mockito.ArgumentMatchers.isNull(),
                eq(targetDoctorId),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                eq(NOW),
                eq(PageRequest.of(0, 20)))).thenReturn(new PageImpl<>(List.of()));

        GetPendingClinicalOrdersQuery query = new GetPendingClinicalOrdersQuery(null, targetDoctorId, null, null, 0,
                20);
        var result = service.getPendingOrders(query);

        assertEquals(0, result.getTotalElements());
        verify(clinicalOrderItemRepository).findPendingOrders(null, targetDoctorId, null, null, NOW,
                PageRequest.of(0, 20));
    }
}
