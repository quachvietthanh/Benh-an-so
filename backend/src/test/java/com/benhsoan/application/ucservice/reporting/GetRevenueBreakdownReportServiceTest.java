package com.benhsoan.application.ucservice.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.billing.enums.InvoiceLineType;
import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;
import com.benhsoan.port.dto.result.DoctorRevenueResult;
import com.benhsoan.port.dto.result.RevenueBreakdownReportResult;
import com.benhsoan.port.dto.result.ServiceGroupRevenueResult;
import com.benhsoan.port.outbound.repository.reporting.InvoiceLineReportDetail;
import com.benhsoan.port.outbound.repository.reporting.OperationalReportQueryRepository;

class GetRevenueBreakdownReportServiceTest {

    private final OperationalReportQueryRepository queryRepository = mock(OperationalReportQueryRepository.class);
    private final GetRevenueBreakdownReportService service = new GetRevenueBreakdownReportService(queryRepository);

    private static final UUID DOC_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    private static final UUID DOC_2 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3");

    @Test
    void returnsEmptyReportWhenNoLinesExist() {
        when(queryRepository.findInvoiceLineReportDetails(any(), any())).thenReturn(List.of());

        RevenueBreakdownReportResult result = service.getRevenueBreakdown(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
        );

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.totalNetRevenue());
        assertEquals(BigDecimal.ZERO, result.totalExamRevenue());
        assertEquals(BigDecimal.ZERO, result.totalClinicalServiceRevenue());
        assertEquals(BigDecimal.ZERO, result.totalMedicationRevenue());
        assertEquals(BigDecimal.ZERO, result.totalAdjustmentRevenue());
        assertEquals("VND", result.currency());
        assertTrue(result.serviceGroups().isEmpty());
        assertTrue(result.doctors().isEmpty());
    }

    @Test
    void calculatesBreakdownWithMultipleServicesAndDoctorsCorrectly() {
        UUID invoice1 = UUID.randomUUID();
        UUID visit1 = UUID.randomUUID();

        List<InvoiceLineReportDetail> lines = List.of(
                // Doctor 1: Exam fee 100,000
                new InvoiceLineReportDetail(
                        UUID.randomUUID(), invoice1, InvoiceType.ORIGINAL, InvoiceLineType.EXAM_FEE,
                        "Phi kham", new BigDecimal("100000"), visit1, visit1, DOC_1, "doctor1", "Dr. Nguyen Minh Anh", null
                ),
                // Doctor 2: Lab test 150,000
                new InvoiceLineReportDetail(
                        UUID.randomUUID(), invoice1, InvoiceType.ORIGINAL, InvoiceLineType.SERVICE_FEE,
                        "Xet nghiem mau", new BigDecimal("150000"), UUID.randomUUID(), visit1, DOC_2, "doctor2", "Dr. Tran Quang Huy", ClinicalServiceType.LAB_TEST
                ),
                // Doctor 1: Imaging 250,000
                new InvoiceLineReportDetail(
                        UUID.randomUUID(), invoice1, InvoiceType.ORIGINAL, InvoiceLineType.SERVICE_FEE,
                        "Chup X-quang", new BigDecimal("250000"), UUID.randomUUID(), visit1, DOC_1, "doctor1", "Dr. Nguyen Minh Anh", ClinicalServiceType.IMAGING
                ),
                // Doctor 1: Medicine fee 200,000
                new InvoiceLineReportDetail(
                        UUID.randomUUID(), invoice1, InvoiceType.ORIGINAL, InvoiceLineType.MEDICINE_FEE,
                        "Tien thuoc", new BigDecimal("200000"), UUID.randomUUID(), visit1, DOC_1, "doctor1", "Dr. Nguyen Minh Anh", null
                )
        );

        when(queryRepository.findInvoiceLineReportDetails(any(), any())).thenReturn(lines);

        RevenueBreakdownReportResult result = service.getRevenueBreakdown(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
        );

        // Total net revenue: 100k + 150k + 250k + 200k = 700k
        assertEquals(new BigDecimal("700000"), result.totalNetRevenue());
        assertEquals(new BigDecimal("100000"), result.totalExamRevenue());
        assertEquals(new BigDecimal("400000"), result.totalClinicalServiceRevenue());
        assertEquals(new BigDecimal("200000"), result.totalMedicationRevenue());
        assertEquals(BigDecimal.ZERO, result.totalAdjustmentRevenue());

        // Service groups
        ServiceGroupRevenueResult examGroup = result.serviceGroups().stream()
                .filter(g -> "EXAMINATION".equals(g.groupCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("100000"), examGroup.revenue());
        assertEquals(new BigDecimal("14.29"), examGroup.percentage()); // 100k / 700k * 100 = 14.29%

        ServiceGroupRevenueResult labGroup = result.serviceGroups().stream()
                .filter(g -> "LAB_TEST".equals(g.groupCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("150000"), labGroup.revenue());
        assertEquals(new BigDecimal("21.43"), labGroup.percentage());

        ServiceGroupRevenueResult imgGroup = result.serviceGroups().stream()
                .filter(g -> "IMAGING".equals(g.groupCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("250000"), imgGroup.revenue());
        assertEquals(new BigDecimal("35.71"), imgGroup.percentage());

        // Doctors (Doc 1: 100k exam + 250k img + 200k med = 550k; Doc 2: 150k lab)
        assertEquals(2, result.doctors().size());
        DoctorRevenueResult firstDoc = result.doctors().get(0);
        assertEquals(DOC_1, firstDoc.doctorId());
        assertEquals(new BigDecimal("550000"), firstDoc.totalRevenue());
        assertEquals(new BigDecimal("78.57"), firstDoc.percentage()); // 550k / 700k * 100 = 78.57%

        DoctorRevenueResult secondDoc = result.doctors().get(1);
        assertEquals(DOC_2, secondDoc.doctorId());
        assertEquals(new BigDecimal("150000"), secondDoc.totalRevenue());
        assertEquals(new BigDecimal("21.43"), secondDoc.percentage());
    }

    @Test
    void deductsAdjustmentsAndRefundsCorrectlyAndMatchesNetRevenue() {
        UUID invoice1 = UUID.randomUUID();
        UUID adjInvoice = UUID.randomUUID();
        UUID visit1 = UUID.randomUUID();

        List<InvoiceLineReportDetail> lines = List.of(
                // Original lines: Exam 100k, Medicine 150k
                new InvoiceLineReportDetail(
                        UUID.randomUUID(), invoice1, InvoiceType.ORIGINAL, InvoiceLineType.EXAM_FEE,
                        "Phi kham", new BigDecimal("100000"), visit1, visit1, DOC_1, "doctor1", "Dr. Nguyen Minh Anh", null
                ),
                new InvoiceLineReportDetail(
                        UUID.randomUUID(), invoice1, InvoiceType.ORIGINAL, InvoiceLineType.MEDICINE_FEE,
                        "Tien thuoc", new BigDecimal("150000"), UUID.randomUUID(), visit1, DOC_1, "doctor1", "Dr. Nguyen Minh Anh", null
                ),
                // Adjustment line: -20,000 (Dieu chinh giam tien thuoc)
                new InvoiceLineReportDetail(
                        UUID.randomUUID(), adjInvoice, InvoiceType.ADJUSTMENT, InvoiceLineType.ADJUSTMENT,
                        InvoiceLineType.MEDICINE_FEE,
                        "Dieu chinh giam tien thuoc", new BigDecimal("-20000"), UUID.randomUUID(), visit1, DOC_1, "doctor1", "Dr. Nguyen Minh Anh", null
                )
        );

        when(queryRepository.findInvoiceLineReportDetails(any(), any())).thenReturn(lines);

        RevenueBreakdownReportResult result = service.getRevenueBreakdown(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
        );

        // 100k + 150k - 20k = 230k
        assertEquals(new BigDecimal("230000"), result.totalNetRevenue());
        assertEquals(new BigDecimal("100000"), result.totalExamRevenue());
        assertEquals(new BigDecimal("130000"), result.totalMedicationRevenue());
        assertEquals(new BigDecimal("-20000"), result.totalAdjustmentRevenue());

        // Doctor Doc 1 total revenue: 230k (100% of net revenue)
        assertEquals(1, result.doctors().size());
        DoctorRevenueResult doc = result.doctors().get(0);
        assertEquals(new BigDecimal("230000"), doc.totalRevenue());
        assertEquals(new BigDecimal("-20000"), doc.adjustmentRevenue());
        assertEquals(new BigDecimal("100.00"), doc.percentage());

        // Also check MEDICATION in serviceGroups
        ServiceGroupRevenueResult medGroup = result.serviceGroups().stream()
                .filter(g -> "MEDICATION".equals(g.groupCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("130000"), medGroup.revenue());
        assertEquals(new BigDecimal("56.52"), medGroup.percentage());
    }

    @Test
    void deductsMedicineAdjustmentWithBrandNameWithoutWordThuocCorrectly() {
        // Finding P1-02 / TC-REP-02: itemName "Giam gia Paracetamol 500mg" does NOT contain "thuoc"
        UUID invoice1 = UUID.randomUUID();
        UUID adjInvoice = UUID.randomUUID();
        UUID visit1 = UUID.randomUUID();

        List<InvoiceLineReportDetail> lines = List.of(
                new InvoiceLineReportDetail(
                        UUID.randomUUID(), invoice1, InvoiceType.ORIGINAL, InvoiceLineType.EXAM_FEE,
                        InvoiceLineType.EXAM_FEE, "Phi kham", new BigDecimal("100000"),
                        visit1, visit1, DOC_1, "doctor1", "Dr. Nguyen Minh Anh", null
                ),
                new InvoiceLineReportDetail(
                        UUID.randomUUID(), invoice1, InvoiceType.ORIGINAL, InvoiceLineType.MEDICINE_FEE,
                        InvoiceLineType.MEDICINE_FEE, "Paracetamol 500mg", new BigDecimal("80000"),
                        UUID.randomUUID(), visit1, DOC_2, "doctor2", "Dr. Tran Quang Huy", null
                ),
                new InvoiceLineReportDetail(
                        UUID.randomUUID(), adjInvoice, InvoiceType.ADJUSTMENT, InvoiceLineType.ADJUSTMENT,
                        InvoiceLineType.MEDICINE_FEE, "Giam gia Paracetamol 500mg", new BigDecimal("-30000"),
                        UUID.randomUUID(), visit1, DOC_2, "doctor2", "Dr. Tran Quang Huy", null
                )
        );

        when(queryRepository.findInvoiceLineReportDetails(any(), any())).thenReturn(lines);

        RevenueBreakdownReportResult result = service.getRevenueBreakdown(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
        );

        // 100k exam + 80k med - 30k med = 150k net
        assertEquals(new BigDecimal("150000"), result.totalNetRevenue());
        assertEquals(new BigDecimal("100000"), result.totalExamRevenue()); // Must NOT be deducted!
        assertEquals(new BigDecimal("50000"), result.totalMedicationRevenue());
        assertEquals(new BigDecimal("-30000"), result.totalAdjustmentRevenue());

        ServiceGroupRevenueResult examGroup = result.serviceGroups().stream()
                .filter(g -> "EXAMINATION".equals(g.groupCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("100000"), examGroup.revenue());

        ServiceGroupRevenueResult medGroup = result.serviceGroups().stream()
                .filter(g -> "MEDICATION".equals(g.groupCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("50000"), medGroup.revenue());
    }

    @Test
    void returnsZeroPercentageWhenTotalNetRevenueIsZeroOrNegative() {
        // Finding P2-01 / TC-REP-03: Gross 200k, Refund -500k -> totalNetRevenue = -300k
        UUID invoice1 = UUID.randomUUID();
        UUID adjInvoice = UUID.randomUUID();
        UUID visit1 = UUID.randomUUID();

        List<InvoiceLineReportDetail> lines = List.of(
                new InvoiceLineReportDetail(
                        UUID.randomUUID(), invoice1, InvoiceType.ORIGINAL, InvoiceLineType.EXAM_FEE,
                        InvoiceLineType.EXAM_FEE, "Phi kham", new BigDecimal("200000"),
                        visit1, visit1, DOC_1, "doctor1", "Dr. Nguyen Minh Anh", null
                ),
                new InvoiceLineReportDetail(
                        UUID.randomUUID(), adjInvoice, InvoiceType.ADJUSTMENT, InvoiceLineType.ADJUSTMENT,
                        InvoiceLineType.EXAM_FEE, "Hoan tien", new BigDecimal("-500000"),
                        visit1, visit1, DOC_1, "doctor1", "Dr. Nguyen Minh Anh", null
                )
        );

        when(queryRepository.findInvoiceLineReportDetails(any(), any())).thenReturn(lines);

        RevenueBreakdownReportResult result = service.getRevenueBreakdown(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
        );

        assertEquals(new BigDecimal("-300000"), result.totalNetRevenue());

        // All percentages must be 0.00
        for (ServiceGroupRevenueResult group : result.serviceGroups()) {
            assertEquals(new BigDecimal("0.00"), group.percentage());
        }
        for (DoctorRevenueResult doctor : result.doctors()) {
            assertEquals(new BigDecimal("0.00"), doctor.percentage());
        }
    }

    @Test
    void preservesTotalRevenueConservationWhenLineHasNullDoctorId() {
        // Finding P3-02 / TC-REP-06: Line with doctorId == null
        UUID invoice1 = UUID.randomUUID();
        UUID visit1 = UUID.randomUUID();

        List<InvoiceLineReportDetail> lines = List.of(
                new InvoiceLineReportDetail(
                        UUID.randomUUID(), invoice1, InvoiceType.ORIGINAL, InvoiceLineType.EXAM_FEE,
                        InvoiceLineType.EXAM_FEE, "Phi kham", new BigDecimal("100000"),
                        visit1, visit1, DOC_1, "doctor1", "Dr. Nguyen Minh Anh", null
                ),
                new InvoiceLineReportDetail(
                        UUID.randomUUID(), invoice1, InvoiceType.ORIGINAL, InvoiceLineType.SERVICE_FEE,
                        InvoiceLineType.SERVICE_FEE, "Phi vat tu ngoai", new BigDecimal("50000"),
                        UUID.randomUUID(), visit1, null, null, null, null
                )
        );

        when(queryRepository.findInvoiceLineReportDetails(any(), any())).thenReturn(lines);

        RevenueBreakdownReportResult result = service.getRevenueBreakdown(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
        );

        assertEquals(new BigDecimal("150000"), result.totalNetRevenue());

        // Total doctor revenues must equal totalNetRevenue: 100k + 50k = 150k
        BigDecimal sumDoctorRevenue = result.doctors().stream()
                .map(DoctorRevenueResult::totalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(result.totalNetRevenue(), sumDoctorRevenue);

        DoctorRevenueResult unassigned = result.doctors().stream()
                .filter(d -> "UNASSIGNED".equals(d.doctorCode())).findFirst().orElseThrow();
        assertEquals("Chưa phân bổ bác sĩ", unassigned.doctorName());
        assertEquals(new BigDecimal("50000"), unassigned.totalRevenue());
    }

    @Test
    void routesUnresolvedAdjustmentToOtherServiceGroupAndUnassignedDoctor() {
        // Finding P2-2: Unresolved adjustment must route to OTHER and UNASSIGNED doctor
        UUID invoice1 = UUID.randomUUID();
        UUID adjInvoice = UUID.randomUUID();
        UUID visit1 = UUID.randomUUID();

        List<InvoiceLineReportDetail> lines = List.of(
                new InvoiceLineReportDetail(
                        UUID.randomUUID(), invoice1, InvoiceType.ORIGINAL, InvoiceLineType.EXAM_FEE,
                        InvoiceLineType.EXAM_FEE, "Phi kham", new BigDecimal("100000"),
                        visit1, visit1, DOC_1, "doctor1", "Dr. Nguyen Minh Anh", null
                ),
                new InvoiceLineReportDetail(
                        UUID.randomUUID(), adjInvoice, InvoiceType.ADJUSTMENT, InvoiceLineType.ADJUSTMENT,
                        null, "Dieu chinh khong ro nguon goc", new BigDecimal("-30000"),
                        null, visit1, null, null, null, null
                )
        );

        when(queryRepository.findInvoiceLineReportDetails(any(), any())).thenReturn(lines);

        RevenueBreakdownReportResult result = service.getRevenueBreakdown(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
        );

        assertEquals(new BigDecimal("70000"), result.totalNetRevenue());
        assertEquals(new BigDecimal("100000"), result.totalExamRevenue()); // Must NOT be docked!
        assertEquals(new BigDecimal("-30000"), result.totalClinicalServiceRevenue());
        assertEquals(new BigDecimal("-30000"), result.totalAdjustmentRevenue());

        ServiceGroupRevenueResult examGroup = result.serviceGroups().stream()
                .filter(g -> "EXAMINATION".equals(g.groupCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("100000"), examGroup.revenue());

        ServiceGroupRevenueResult otherGroup = result.serviceGroups().stream()
                .filter(g -> "OTHER".equals(g.groupCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("-30000"), otherGroup.revenue());

        // Doctors: DOC_1 = 100k, UNASSIGNED = -30k
        DoctorRevenueResult doc1 = result.doctors().stream()
                .filter(d -> DOC_1.equals(d.doctorId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("100000"), doc1.totalRevenue());

        DoctorRevenueResult unassigned = result.doctors().stream()
                .filter(d -> "UNASSIGNED".equals(d.doctorCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("-30000"), unassigned.totalRevenue());
        assertEquals(new BigDecimal("-30000"), unassigned.adjustmentRevenue());

        // Conservation check
        BigDecimal sumServiceGroupRevenue = result.serviceGroups().stream()
                .map(ServiceGroupRevenueResult::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(result.totalNetRevenue(), sumServiceGroupRevenue);

        BigDecimal sumDoctorRevenue = result.doctors().stream()
                .map(DoctorRevenueResult::totalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(result.totalNetRevenue(), sumDoctorRevenue);
    }

    @Test
    void calculatesNegativePercentageWhenTotalNetRevenueIsPositive() {
        // Finding P2-3: Doc 1 = 1M, Doc 2 = -200k, Total = 800k (> 0)
        UUID invoice1 = UUID.randomUUID();
        UUID adjInvoice = UUID.randomUUID();
        UUID visit1 = UUID.randomUUID();

        List<InvoiceLineReportDetail> lines = List.of(
                new InvoiceLineReportDetail(
                        UUID.randomUUID(), invoice1, InvoiceType.ORIGINAL, InvoiceLineType.EXAM_FEE,
                        InvoiceLineType.EXAM_FEE, "Phi kham", new BigDecimal("1000000"),
                        visit1, visit1, DOC_1, "doctor1", "Dr. Nguyen Minh Anh", null
                ),
                new InvoiceLineReportDetail(
                        UUID.randomUUID(), adjInvoice, InvoiceType.ADJUSTMENT, InvoiceLineType.ADJUSTMENT,
                        InvoiceLineType.MEDICINE_FEE, "Hoan tra thuoc", new BigDecimal("-200000"),
                        UUID.randomUUID(), visit1, DOC_2, "doctor2", "Dr. Tran Quang Huy", null
                )
        );

        when(queryRepository.findInvoiceLineReportDetails(any(), any())).thenReturn(lines);

        RevenueBreakdownReportResult result = service.getRevenueBreakdown(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
        );

        assertEquals(new BigDecimal("800000"), result.totalNetRevenue());

        DoctorRevenueResult doc1 = result.doctors().stream()
                .filter(d -> DOC_1.equals(d.doctorId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("125.00"), doc1.percentage()); // 1,000,000 / 800,000 * 100 = 125.00%

        DoctorRevenueResult doc2 = result.doctors().stream()
                .filter(d -> DOC_2.equals(d.doctorId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("-25.00"), doc2.percentage()); // -200,000 / 800,000 * 100 = -25.00%

        ServiceGroupRevenueResult examGroup = result.serviceGroups().stream()
                .filter(g -> "EXAMINATION".equals(g.groupCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("125.00"), examGroup.percentage());

        ServiceGroupRevenueResult medGroup = result.serviceGroups().stream()
                .filter(g -> "MEDICATION".equals(g.groupCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("-25.00"), medGroup.percentage());

        // Sum of percentages equals 100.00%
        BigDecimal sumDoctorPercentages = result.doctors().stream()
                .map(DoctorRevenueResult::percentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("100.00"), sumDoctorPercentages);
    }
}
