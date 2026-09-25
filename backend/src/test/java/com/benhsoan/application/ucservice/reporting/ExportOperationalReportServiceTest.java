package com.benhsoan.application.ucservice.reporting;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.reporting.enums.ReportType;
import com.benhsoan.port.outbound.repository.reporting.VisitReportDetailItem;
import com.benhsoan.domain.reporting.exception.OperationalReportDataEmptyException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.DiseasePatternItemResult;
import com.benhsoan.port.dto.result.DiseasePatternReportResult;
import com.benhsoan.port.dto.result.OperationalReportExportResult;
import com.benhsoan.port.dto.result.OperationalSummaryResult;
import com.benhsoan.port.dto.result.OperationalTimelineItemResult;
import com.benhsoan.port.dto.result.OperationalTimelineResult;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

class ExportOperationalReportServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);

    @BeforeEach
    void setUp() {
        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
    }

    private ExportOperationalReportService createService(
            OperationalReportDataService dataService,
            OperationalReportAuditService auditService
    ) {
        return new ExportOperationalReportService(dataService, auditService, userRepository, currentUserPort);
    }

    @Test
    void exportsCsvAndWritesAuditLog() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);
        when(dataService.hasReportData(any(), any(), any())).thenReturn(true);

        when(dataService.getReportData(any(), any())).thenReturn(new OperationalReportData(
                new OperationalSummaryResult(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 3),
                        3L,
                        new BigDecimal("80000"),
                        "VND"
                ),
                new OperationalTimelineResult(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 3),
                        List.of(
                                new OperationalTimelineItemResult(LocalDate.of(2026, 8, 1), 2L, new BigDecimal("100000")),
                                new OperationalTimelineItemResult(LocalDate.of(2026, 8, 2), 0L, BigDecimal.ZERO),
                                new OperationalTimelineItemResult(LocalDate.of(2026, 8, 3), 1L, new BigDecimal("-20000"))
                        )
                )
        ));

        ExportOperationalReportService service = createService(dataService, auditService);

        OperationalReportExportResult result = service.export(
                ReportType.OPERATIONAL_REPORT,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3)
        );

        assertEquals(ReportType.OPERATIONAL_REPORT, result.reportType());
        assertEquals("operational-report-2026-08-01-to-2026-08-03.csv", result.fileName());
        assertEquals("text/csv; charset=UTF-8", result.contentType());
        assertArrayEquals("""
                \uFEFFOPERATIONAL REPORT
                From,2026-08-01
                To,2026-08-03
                Visit Count,3
                Revenue (VND),80000

                Date,Visit Count,Revenue (VND)
                2026-08-01,2,100000
                2026-08-02,0,0
                2026-08-03,1,-20000
                """.getBytes(StandardCharsets.UTF_8), result.content());
        verify(auditService).logExport(ReportType.OPERATIONAL_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));
    }

    @Test
    void exportsVisitCsvWithVisitDataOnly() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);
        when(dataService.hasReportData(any(), any(), any())).thenReturn(true);
        when(dataService.getReportData(any(), any())).thenReturn(sampleReportData());

        OperationalReportExportResult result = createService(dataService, auditService).export(
                ReportType.VISIT_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));

        assertEquals("visit-report-2026-08-01-to-2026-08-03.csv", result.fileName());
        assertArrayEquals("""
                \uFEFFVISIT REPORT
                From,2026-08-01
                To,2026-08-03
                Visit Count,3

                Date,Visit Count
                2026-08-01,2
                2026-08-02,0
                2026-08-03,1
                """.getBytes(StandardCharsets.UTF_8), result.content());
        verify(auditService).logExport(ReportType.VISIT_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));
    }

    @Test
    void exportsRevenueCsvWithRevenueDataOnly() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);
        when(dataService.hasReportData(any(), any(), any())).thenReturn(true);
        when(dataService.getReportData(any(), any())).thenReturn(sampleReportData());

        OperationalReportExportResult result = createService(dataService, auditService).export(
                ReportType.REVENUE_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));

        assertEquals("revenue-report-2026-08-01-to-2026-08-03.csv", result.fileName());
        assertArrayEquals("""
                \uFEFFREVENUE REPORT
                From,2026-08-01
                To,2026-08-03
                Revenue (VND),80000

                Date,Revenue (VND)
                2026-08-01,100000
                2026-08-02,0
                2026-08-03,-20000
                """.getBytes(StandardCharsets.UTF_8), result.content());
        verify(auditService).logExport(ReportType.REVENUE_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));
    }

    @Test
    void rejectsExportWithoutOperationalDataAndDoesNotWriteAuditLog() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);
        when(dataService.hasReportData(any(), any(), any())).thenReturn(false);

        ExportOperationalReportService service = createService(dataService, auditService);

        OperationalReportDataEmptyException exception = assertThrows(
                OperationalReportDataEmptyException.class,
                () -> service.export(ReportType.OPERATIONAL_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3))
        );

        assertEquals("No report data available for the selected period.", exception.getMessage());
        verify(dataService, never()).getReportData(any(), any());
        verifyNoInteractions(auditService);
    }

    @Test
    void exportsRevenueReportWhenInvoicesExistEvenIfNetRevenueIsZero() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);
        when(dataService.hasReportData(any(), any(), any())).thenReturn(true);
        when(dataService.getReportData(any(), any())).thenReturn(new OperationalReportData(
                new OperationalSummaryResult(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 2),
                        0L,
                        BigDecimal.ZERO,
                        "VND"
                ),
                new OperationalTimelineResult(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 2),
                        List.of(
                                new OperationalTimelineItemResult(
                                        LocalDate.of(2026, 8, 1), 0L, new BigDecimal("100000")),
                                new OperationalTimelineItemResult(
                                        LocalDate.of(2026, 8, 2), 0L, new BigDecimal("-100000"))
                        )
                )
        ));

        OperationalReportExportResult result = createService(dataService, auditService).export(
                ReportType.REVENUE_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2));

        assertEquals(ReportType.REVENUE_REPORT, result.reportType());
        assertEquals("revenue-report-2026-08-01-to-2026-08-02.csv", result.fileName());
        verify(auditService).logExport(ReportType.REVENUE_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2));
    }

    @Test
    void exportsDiseasePatternCsvWithoutDoctor() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);
        when(dataService.hasReportData(ReportType.DISEASE_PATTERN_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .thenReturn(true);

        UUID catalogId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        when(dataService.getDiseasePatterns(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, null))
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

        ExportOperationalReportService service = createService(dataService, auditService);
        OperationalReportExportResult result = service.export(
                ReportType.DISEASE_PATTERN_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertEquals("disease-pattern-report-2026-08-01-to-2026-08-31.csv", result.fileName());
        assertArrayEquals("""
                \uFEFFDISEASE PATTERN REPORT
                From,2026-08-01
                To,2026-08-31
                Doctor,All Doctors
                Total Diagnoses,50

                Rank,Disease Code,Disease Name,Disease Group,Diagnosis Count,Percentage
                1,J00,Viêm mũi họng cấp,Bệnh hệ hô hấp,50,100.00%
                """.getBytes(StandardCharsets.UTF_8), result.content());
        verify(auditService).logExport(ReportType.DISEASE_PATTERN_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
    }

    @Test
    void exportsDiseasePatternCsvWithDoctor() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);

        UUID doctorId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
        User doctor = mock(User.class);
        when(doctor.getFullName()).thenReturn("Dr. Nguyen Minh Anh");
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        when(dataService.hasReportData(ReportType.DISEASE_PATTERN_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), doctorId))
                .thenReturn(true);

        UUID catalogId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        when(dataService.getDiseasePatterns(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), doctorId, "Dr. Nguyen Minh Anh"))
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
                                "Tăng huyết áp",
                                "Bệnh hệ tuần hoàn",
                                20L,
                                100.0))
                ));

        ExportOperationalReportService service = createService(dataService, auditService);
        OperationalReportExportResult result = service.export(
                ReportType.DISEASE_PATTERN_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), doctorId);

        assertEquals("disease-pattern-report-2026-08-01-to-2026-08-31-doctor-aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2.csv", result.fileName());
        assertArrayEquals("""
                \uFEFFDISEASE PATTERN REPORT
                From,2026-08-01
                To,2026-08-31
                Doctor,Dr. Nguyen Minh Anh
                Total Diagnoses,20

                Rank,Disease Code,Disease Name,Disease Group,Diagnosis Count,Percentage
                1,I10,Tăng huyết áp,Bệnh hệ tuần hoàn,20,100.00%
                """.getBytes(StandardCharsets.UTF_8), result.content());
        verify(auditService).logExport(ReportType.DISEASE_PATTERN_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), doctorId);
    }

    @Test
    void rejectsExportDiseasePatternWhenUserIsNotManagerAndAuditsAccessDenied() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);

        ExportOperationalReportService service = createService(dataService, auditService);

        assertThrows(AccessDeniedException.class, () -> service.export(
                ReportType.DISEASE_PATTERN_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)));

        verify(auditService).logAccessDenied(
                ReportType.DISEASE_PATTERN_REPORT,
                "Only managers can export the disease pattern report."
        );
        verifyNoInteractions(dataService);
    }

    @Test
    void rejectsExportDiseasePatternWhenDoctorNotFound() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);
        UUID invalidDoctorId = UUID.randomUUID();
        when(userRepository.findById(invalidDoctorId)).thenReturn(Optional.empty());

        ExportOperationalReportService service = createService(dataService, auditService);

        ValidationException ex = assertThrows(ValidationException.class, () -> service.export(
                ReportType.DISEASE_PATTERN_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), invalidDoctorId));

        assertEquals("Doctor not found.", ex.getMessage());
        verifyNoInteractions(dataService);
        verify(auditService, never()).logExport(any(), any(), any(), any());
    }

    @Test
    void exportsVisitCsvWithMaskedPatientDetailsByDefault() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);
        when(dataService.hasReportData(any(), any(), any())).thenReturn(true);
        when(dataService.getReportData(any(), any())).thenReturn(sampleReportData());

        List<VisitReportDetailItem> details = List.of(
                new VisitReportDetailItem(
                        UUID.randomUUID(),
                        "VISIT-2026-001",
                        Instant.parse("2026-08-01T10:30:00Z"),
                        UUID.randomUUID(),
                        "BN-0001",
                        "Nguyễn Văn An",
                        "0912345678",
                        "123 Đường Lê Lợi, Quận 1, TP.HCM",
                        UUID.randomUUID(),
                        "Bác sĩ Trần Bình",
                        "COMPLETED"
                )
        );
        when(dataService.getCompletedVisitDetails(any(), any(), any())).thenReturn(details);

        ExportOperationalReportService service = createService(dataService, auditService);
        OperationalReportExportResult result = service.export(
                ReportType.VISIT_REPORT,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3)
        );

        String csv = new String(result.content(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("VISIT DETAILS"));
        assertTrue(csv.contains("BỆNH NHÂN #BN-0001"));
        assertTrue(csv.contains("09******78"));
        assertTrue(csv.contains("[ĐỊA CHỈ ĐÃ ẨN DANH]"));
        assertFalse(csv.contains("Nguyễn Văn An"));
        assertFalse(csv.contains("0912345678"));
        assertFalse(csv.contains("123 Đường Lê Lợi"));

        verify(auditService).logExport(ReportType.VISIT_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));
    }

    @Test
    void exportsVisitCsvWithUnmaskedPatientDetailsWhenUserHasPermissionAndValidReason() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);
        when(dataService.hasReportData(any(), any(), any())).thenReturn(true);
        when(dataService.getReportData(any(), any())).thenReturn(sampleReportData());
        when(currentUserPort.hasPermission("REPORT_UNMASKED_EXPORT")).thenReturn(true);

        List<VisitReportDetailItem> details = List.of(
                new VisitReportDetailItem(
                        UUID.randomUUID(),
                        "VISIT-2026-001",
                        Instant.parse("2026-08-01T10:30:00Z"),
                        UUID.randomUUID(),
                        "BN-0001",
                        "Nguyễn Văn An",
                        "0912345678",
                        "123 Đường Lê Lợi, Quận 1, TP.HCM",
                        UUID.randomUUID(),
                        "Bác sĩ Trần Bình",
                        "COMPLETED"
                )
        );
        when(dataService.getCompletedVisitDetails(any(), any(), any())).thenReturn(details);

        ExportOperationalReportService service = createService(dataService, auditService);
        OperationalReportExportResult result = service.export(
                ReportType.VISIT_REPORT,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                null,
                true,
                "Clinical research study #123"
        );

        String csv = new String(result.content(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("VISIT DETAILS"));
        assertTrue(csv.contains("Nguyễn Văn An"));
        assertTrue(csv.contains("0912345678"));
        assertTrue(csv.contains("123 Đường Lê Lợi, Quận 1, TP.HCM"));
        assertFalse(csv.contains("BỆNH NHÂN #BN-0001"));

        verify(auditService).logExport(
                ReportType.VISIT_REPORT,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                null,
                true,
                "Clinical research study #123"
        );
    }

    @Test
    void rejectsUnmaskedExportWhenUserLacksPermissionAndIsNotAdmin() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);
        when(currentUserPort.hasPermission("REPORT_UNMASKED_EXPORT")).thenReturn(false);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        ExportOperationalReportService service = createService(dataService, auditService);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> service.export(
                ReportType.VISIT_REPORT,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                null,
                true,
                "Clinical research study #123"
        ));

        assertEquals("User lacks permission to export unmasked patient data.", ex.getMessage());
        verify(auditService).logAccessDenied(
                ReportType.VISIT_REPORT,
                "User lacks high-privilege permission REPORT_UNMASKED_EXPORT to export unmasked patient data."
        );
        verifyNoInteractions(dataService);
    }

    @Test
    void rejectsUnmaskedExportWhenReasonIsBlankOrTooShort() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);
        when(currentUserPort.hasPermission("REPORT_UNMASKED_EXPORT")).thenReturn(true);

        ExportOperationalReportService service = createService(dataService, auditService);

        ValidationException ex1 = assertThrows(ValidationException.class, () -> service.export(
                ReportType.VISIT_REPORT,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                null,
                true,
                null
        ));
        assertEquals("Reason is required and must be at least 5 characters for unmasked export.", ex1.getMessage());

        ValidationException ex2 = assertThrows(ValidationException.class, () -> service.export(
                ReportType.VISIT_REPORT,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                null,
                true,
                "   "
        ));
        assertEquals("Reason is required and must be at least 5 characters for unmasked export.", ex2.getMessage());

        ValidationException ex3 = assertThrows(ValidationException.class, () -> service.export(
                ReportType.VISIT_REPORT,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                null,
                true,
                "1234"
        ));
        assertEquals("Reason is required and must be at least 5 characters for unmasked export.", ex3.getMessage());

        verifyNoInteractions(dataService);
    }

    @Test
    void allowsUnmaskedExportWhenUserHasAdminRole() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);
        when(dataService.hasReportData(any(), any(), any())).thenReturn(true);
        when(dataService.getReportData(any(), any())).thenReturn(sampleReportData());
        when(currentUserPort.hasPermission("REPORT_UNMASKED_EXPORT")).thenReturn(false);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);

        ExportOperationalReportService service = createService(dataService, auditService);
        OperationalReportExportResult result = service.export(
                ReportType.OPERATIONAL_REPORT,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                null,
                true,
                "Internal system audit"
        );

        assertEquals(ReportType.OPERATIONAL_REPORT, result.reportType());
        verify(auditService).logExport(
                ReportType.OPERATIONAL_REPORT,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                null,
                true,
                "Internal system audit"
        );
    }

    @Test
    void rejectsUnmaskedExportWhenReasonExceeds500Chars() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);
        when(currentUserPort.hasPermission("REPORT_UNMASKED_EXPORT")).thenReturn(true);

        ExportOperationalReportService service = createService(dataService, auditService);

        String tooLongReason = "A".repeat(501);
        ValidationException ex = assertThrows(ValidationException.class, () -> service.export(
                ReportType.VISIT_REPORT,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                null,
                true,
                tooLongReason
        ));
        assertEquals("Reason must not exceed 500 characters.", ex.getMessage());
        verifyNoInteractions(dataService);
    }

    @Test
    void allowsUnmaskedExportWhenReasonIsExactly500Chars() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);
        when(dataService.hasReportData(any(), any(), any())).thenReturn(true);
        when(dataService.getReportData(any(), any())).thenReturn(sampleReportData());
        when(currentUserPort.hasPermission("REPORT_UNMASKED_EXPORT")).thenReturn(true);
        when(dataService.getCompletedVisitDetails(any(), any(), any())).thenReturn(List.of());

        ExportOperationalReportService service = createService(dataService, auditService);

        String exact500Reason = "A".repeat(500);
        OperationalReportExportResult result = service.export(
                ReportType.VISIT_REPORT,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                null,
                true,
                exact500Reason
        );

        assertNotNull(result);
        verify(auditService).logExport(
                ReportType.VISIT_REPORT,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                null,
                true,
                exact500Reason
        );
    }

    @Test
    void exportsVisitCsvFilteredByDoctorIdInVisitDetails() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);
        UUID doctorId = UUID.randomUUID();
        when(dataService.hasReportData(any(), any(), any(), any())).thenReturn(true);
        when(dataService.getReportData(any(), any())).thenReturn(sampleReportData());

        List<VisitReportDetailItem> doctorVisits = List.of(
                new VisitReportDetailItem(
                        UUID.randomUUID(),
                        "VISIT-2026-DOC1",
                        Instant.parse("2026-08-01T10:30:00Z"),
                        UUID.randomUUID(),
                        "BN-0001",
                        "Bệnh nhân A",
                        "0912345678",
                        "Địa chỉ A",
                        doctorId,
                        "BS. Trần Bình",
                        "COMPLETED"
                )
        );
        when(dataService.getCompletedVisitDetails(eq(LocalDate.of(2026, 8, 1)), eq(LocalDate.of(2026, 8, 3)), eq(doctorId)))
                .thenReturn(doctorVisits);

        ExportOperationalReportService service = createService(dataService, auditService);
        OperationalReportExportResult result = service.export(
                ReportType.VISIT_REPORT,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                doctorId
        );

        assertNotNull(result);
        String csv = new String(result.content(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("VISIT-2026-DOC1"));
        verify(dataService).getCompletedVisitDetails(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3), doctorId);
    }

    private OperationalReportData sampleReportData() {
        return new OperationalReportData(
                new OperationalSummaryResult(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 3),
                        3L,
                        new BigDecimal("80000"),
                        "VND"
                ),
                new OperationalTimelineResult(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 3),
                        List.of(
                                new OperationalTimelineItemResult(LocalDate.of(2026, 8, 1), 2L, new BigDecimal("100000")),
                                new OperationalTimelineItemResult(LocalDate.of(2026, 8, 2), 0L, BigDecimal.ZERO),
                                new OperationalTimelineItemResult(LocalDate.of(2026, 8, 3), 1L, new BigDecimal("-20000"))
                        )
                )
        );
    }
}
