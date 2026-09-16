package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.port.dto.command.medicalrecord.GetOverdueMedicalRecordsQuery;
import com.benhsoan.port.dto.result.OverdueMedicalRecordResult;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.OverdueMedicalRecordQueryRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetOverdueMedicalRecordsService - Unit Tests (NCL-11-CN-006)")
class GetOverdueMedicalRecordsServiceTest {

    @Mock private ClinicConfigurationRepository clinicConfigurationRepository;
    @Mock private OverdueMedicalRecordQueryRepository overdueQueryRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;

    @InjectMocks private GetOverdueMedicalRecordsService service;

    private final Instant now = Instant.parse("2026-09-16T12:00:00Z");

    @Test
    @DisplayName("Doctor caller automatically restricts query to own doctorId")
    void doctorCallerRestrictsToOwnDoctorId() {
        UUID doctorId = UUID.randomUUID();
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorId);

        when(clinicConfigurationRepository.find()).thenReturn(Optional.empty());
        when(clockPort.now()).thenReturn(now);

        PageRequest pageRequest = PageRequest.of(0, 20);
        when(overdueQueryRepository.findOverdueRecords(
                eq(doctorId),
                eq(now.minus(Duration.ofHours(24))),
                eq(24),
                eq(now),
                eq(pageRequest)
        )).thenReturn(Page.empty());

        Page<OverdueMedicalRecordResult> result = service.getOverdueRecords(new GetOverdueMedicalRecordsQuery(null, pageRequest));

        assertNotNull(result);
        verify(overdueQueryRepository).findOverdueRecords(
                eq(doctorId),
                eq(now.minus(Duration.ofHours(24))),
                eq(24),
                eq(now),
                eq(pageRequest)
        );
    }

    @Test
    @DisplayName("Manager caller can query all overdue records or filter by specific doctor")
    void managerCallerCanQueryAllDoctors() {
        UUID filterDoctorId = UUID.randomUUID();
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(false);

        ClinicConfiguration config = ClinicConfiguration.create(
                "Clinic", "Address", "0123456789", java.time.LocalTime.of(8, 0), java.time.LocalTime.of(17, 0), 10, 12, now
        );
        when(clinicConfigurationRepository.find()).thenReturn(Optional.of(config));
        when(clockPort.now()).thenReturn(now);

        PageRequest pageRequest = PageRequest.of(0, 10);
        OverdueMedicalRecordResult item = new OverdueMedicalRecordResult(
                UUID.randomUUID(), MedicalRecordStatus.OPEN, UUID.randomUUID(), "KB-001", now.minus(Duration.ofHours(30)),
                UUID.randomUUID(), "BN-001", "Nguyễn Văn A", filterDoctorId, "BS Trần B", "bs@clinic.com", "0987654321",
                12, now.minus(Duration.ofHours(18)), 18, 0, null
        );
        when(overdueQueryRepository.findOverdueRecords(
                eq(filterDoctorId),
                eq(now.minus(Duration.ofHours(12))),
                eq(12),
                eq(now),
                eq(pageRequest)
        )).thenReturn(new PageImpl<>(List.of(item), pageRequest, 1));

        Page<OverdueMedicalRecordResult> result = service.getOverdueRecords(new GetOverdueMedicalRecordsQuery(filterDoctorId, pageRequest));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("KB-001", result.getContent().get(0).visitCode());
        assertEquals(18, result.getContent().get(0).overdueHours());
    }
}
