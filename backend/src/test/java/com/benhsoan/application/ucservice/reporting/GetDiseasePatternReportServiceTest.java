package com.benhsoan.application.ucservice.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.DiseasePatternItemResult;
import com.benhsoan.port.dto.result.DiseasePatternReportResult;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class GetDiseasePatternReportServiceTest {

    private final OperationalReportDataService operationalReportDataService =
            mock(OperationalReportDataService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private final GetDiseasePatternReportService service =
            new GetDiseasePatternReportService(
                    operationalReportDataService, userRepository, currentUserPort, clockPort);

    @Test
    void delegatesAndStampsGeneratedAtWhenAuthorizedWithoutDoctor() {
        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
        Instant generatedAt = Instant.parse("2026-08-31T08:00:00Z");
        when(clockPort.now()).thenReturn(generatedAt);

        UUID catalogId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        when(operationalReportDataService.getDiseasePatterns(any(), any(), any(), any()))
                .thenReturn(new DiseasePatternReportResult(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31),
                        null,
                        null,
                        50L,
                        null,
                        List.of(new DiseasePatternItemResult(
                                1,
                                catalogId,
                                "J00",
                                "Viêm mũi họng cấp",
                                "Bệnh hệ hô hấp",
                                50L,
                                100.0))
                ));

        DiseasePatternReportResult result = service.getDiseasePatternReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null);

        assertEquals(generatedAt, result.generatedAt());
        assertNull(result.doctorId());
        assertNull(result.doctorName());
        assertEquals(50L, result.totalDiagnoses());
        assertEquals(1, result.items().size());
        assertEquals("J00", result.items().get(0).diseaseCode());
        assertEquals(100.0, result.items().get(0).percentage());
        verify(operationalReportDataService)
                .getDiseasePatterns(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, null);
    }

    @Test
    void delegatesAndResolvesDoctorNameWhenDoctorIdProvided() {
        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
        Instant generatedAt = Instant.parse("2026-08-31T08:00:00Z");
        when(clockPort.now()).thenReturn(generatedAt);

        UUID doctorId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
        User doctor = mock(User.class);
        when(doctor.getFullName()).thenReturn("Dr. Nguyen Minh Anh");
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        UUID catalogId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        when(operationalReportDataService.getDiseasePatterns(any(), any(), any(), any()))
                .thenReturn(new DiseasePatternReportResult(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31),
                        doctorId,
                        "Dr. Nguyen Minh Anh",
                        20L,
                        null,
                        List.of(new DiseasePatternItemResult(
                                1,
                                catalogId,
                                "I10",
                                "Tăng huyết áp vô căn",
                                "Bệnh hệ tuần hoàn",
                                20L,
                                100.0))
                ));

        DiseasePatternReportResult result = service.getDiseasePatternReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), doctorId);

        assertEquals(generatedAt, result.generatedAt());
        assertEquals(doctorId, result.doctorId());
        assertEquals("Dr. Nguyen Minh Anh", result.doctorName());
        assertEquals(20L, result.totalDiagnoses());
        verify(operationalReportDataService)
                .getDiseasePatterns(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), doctorId, "Dr. Nguyen Minh Anh");
    }

    @Test
    void rejectsNonManager() {
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.getDiseasePatternReport(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null));
    }

    @Test
    void rejectsWhenDoctorNotFound() {
        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
        UUID doctorId = UUID.randomUUID();
        when(userRepository.findById(doctorId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> service.getDiseasePatternReport(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), doctorId));
    }
}
