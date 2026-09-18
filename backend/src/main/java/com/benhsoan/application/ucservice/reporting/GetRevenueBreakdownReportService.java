package com.benhsoan.application.ucservice.reporting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.billing.enums.InvoiceLineType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;
import com.benhsoan.port.dto.result.DoctorRevenueResult;
import com.benhsoan.port.dto.result.RevenueBreakdownReportResult;
import com.benhsoan.port.dto.result.ServiceGroupRevenueResult;
import com.benhsoan.port.inbound.reporting.GetRevenueBreakdownReportUseCase;
import com.benhsoan.port.outbound.repository.reporting.InvoiceLineReportDetail;
import com.benhsoan.port.outbound.repository.reporting.OperationalReportQueryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRevenueBreakdownReportService implements GetRevenueBreakdownReportUseCase {

    private static final String DEFAULT_CURRENCY = "VND";
    private static final UUID UNASSIGNED_DOC_KEY = new UUID(0L, 0L);

    private final OperationalReportQueryRepository operationalReportQueryRepository;

    @Override
    public RevenueBreakdownReportResult getRevenueBreakdown(LocalDate from, LocalDate to) {
        ReportingTimeRange range = ReportingTimeRange.of(from, to);
        List<InvoiceLineReportDetail> lines = operationalReportQueryRepository
                .findInvoiceLineReportDetails(range.fromInclusive(), range.toExclusive());

        if (lines.isEmpty()) {
            return new RevenueBreakdownReportResult(
                    from,
                    to,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    DEFAULT_CURRENCY,
                    List.of(),
                    List.of()
            );
        }

        BigDecimal totalNetRevenue = BigDecimal.ZERO;
        BigDecimal totalExamRevenue = BigDecimal.ZERO;
        BigDecimal totalClinicalServiceRevenue = BigDecimal.ZERO;
        BigDecimal totalMedicationRevenue = BigDecimal.ZERO;
        BigDecimal totalAdjustmentRevenue = BigDecimal.ZERO;

        Map<String, ServiceGroupAccumulator> groupMap = initializeServiceGroups();
        Map<UUID, DoctorAccumulator> doctorMap = new HashMap<>();

        for (InvoiceLineReportDetail line : lines) {
            BigDecimal amount = line.amount() != null ? line.amount() : BigDecimal.ZERO;
            totalNetRevenue = totalNetRevenue.add(amount);

            if (line.lineType() == InvoiceLineType.ADJUSTMENT) {
                totalAdjustmentRevenue = totalAdjustmentRevenue.add(amount);
            }

            accumulateServiceGroup(line, amount, groupMap);
            accumulateDoctor(line, amount, doctorMap);

            if (line.lineType() == InvoiceLineType.EXAM_FEE) {
                totalExamRevenue = totalExamRevenue.add(amount);
            } else if (line.lineType() == InvoiceLineType.SERVICE_FEE) {
                totalClinicalServiceRevenue = totalClinicalServiceRevenue.add(amount);
            } else if (line.lineType() == InvoiceLineType.MEDICINE_FEE) {
                totalMedicationRevenue = totalMedicationRevenue.add(amount);
            } else if (line.lineType() == InvoiceLineType.ADJUSTMENT) {
                if (line.clinicalServiceType() != null || line.targetLineType() == InvoiceLineType.SERVICE_FEE) {
                    totalClinicalServiceRevenue = totalClinicalServiceRevenue.add(amount);
                } else if (line.targetLineType() == InvoiceLineType.MEDICINE_FEE) {
                    totalMedicationRevenue = totalMedicationRevenue.add(amount);
                } else if (line.targetLineType() == InvoiceLineType.EXAM_FEE) {
                    totalExamRevenue = totalExamRevenue.add(amount);
                } else {
                    totalClinicalServiceRevenue = totalClinicalServiceRevenue.add(amount);
                }
            }
        }

        List<ServiceGroupRevenueResult> serviceGroupResults = new ArrayList<>();
        for (ServiceGroupAccumulator acc : groupMap.values()) {
            BigDecimal percentage = calculatePercentage(acc.revenue, totalNetRevenue);
            serviceGroupResults.add(new ServiceGroupRevenueResult(
                    acc.groupCode,
                    acc.groupName,
                    acc.revenue,
                    percentage
            ));
        }

        List<DoctorRevenueResult> doctorResults = new ArrayList<>();
        for (DoctorAccumulator doc : doctorMap.values()) {
            BigDecimal totalDocRevenue = doc.examRevenue
                    .add(doc.clinicalServiceRevenue)
                    .add(doc.medicationRevenue)
                    .add(doc.adjustmentRevenue);
            BigDecimal percentage = calculatePercentage(totalDocRevenue, totalNetRevenue);

            doctorResults.add(new DoctorRevenueResult(
                    doc.doctorId,
                    doc.doctorCode,
                    doc.doctorName,
                    doc.examRevenue,
                    doc.clinicalServiceRevenue,
                    doc.medicationRevenue,
                    doc.adjustmentRevenue,
                    totalDocRevenue,
                    percentage
            ));
        }

        doctorResults.sort(
                Comparator.comparing(DoctorRevenueResult::totalRevenue).reversed()
                        .thenComparing(DoctorRevenueResult::doctorName, Comparator.nullsLast(Comparator.naturalOrder()))
        );

        return new RevenueBreakdownReportResult(
                from,
                to,
                totalNetRevenue,
                totalExamRevenue,
                totalClinicalServiceRevenue,
                totalMedicationRevenue,
                totalAdjustmentRevenue,
                DEFAULT_CURRENCY,
                serviceGroupResults,
                doctorResults
        );
    }

    private Map<String, ServiceGroupAccumulator> initializeServiceGroups() {
        Map<String, ServiceGroupAccumulator> map = new LinkedHashMap<>();
        map.put("EXAMINATION", new ServiceGroupAccumulator("EXAMINATION", "Khám bệnh"));
        map.put("LAB_TEST", new ServiceGroupAccumulator("LAB_TEST", "Xét nghiệm"));
        map.put("IMAGING", new ServiceGroupAccumulator("IMAGING", "Chẩn đoán hình ảnh"));
        map.put("OTHER", new ServiceGroupAccumulator("OTHER", "Dịch vụ khác"));
        map.put("MEDICATION", new ServiceGroupAccumulator("MEDICATION", "Thuốc / Dược phẩm"));
        return map;
    }

    private void accumulateServiceGroup(
            InvoiceLineReportDetail line,
            BigDecimal amount,
            Map<String, ServiceGroupAccumulator> groupMap
    ) {
        if (line.lineType() == InvoiceLineType.EXAM_FEE) {
            groupMap.get("EXAMINATION").revenue = groupMap.get("EXAMINATION").revenue.add(amount);
        } else if (line.lineType() == InvoiceLineType.SERVICE_FEE) {
            String code = resolveServiceGroupCode(line.clinicalServiceType());
            groupMap.get(code).revenue = groupMap.get(code).revenue.add(amount);
        } else if (line.lineType() == InvoiceLineType.MEDICINE_FEE) {
            groupMap.get("MEDICATION").revenue = groupMap.get("MEDICATION").revenue.add(amount);
        } else if (line.lineType() == InvoiceLineType.ADJUSTMENT) {
            if (line.clinicalServiceType() != null || line.targetLineType() == InvoiceLineType.SERVICE_FEE) {
                String code = resolveServiceGroupCode(line.clinicalServiceType());
                groupMap.get(code).revenue = groupMap.get(code).revenue.add(amount);
            } else if (line.targetLineType() == InvoiceLineType.MEDICINE_FEE) {
                groupMap.get("MEDICATION").revenue = groupMap.get("MEDICATION").revenue.add(amount);
            } else if (line.targetLineType() == InvoiceLineType.EXAM_FEE) {
                groupMap.get("EXAMINATION").revenue = groupMap.get("EXAMINATION").revenue.add(amount);
            } else {
                groupMap.get("OTHER").revenue = groupMap.get("OTHER").revenue.add(amount);
            }
        }
    }

    private void accumulateDoctor(
            InvoiceLineReportDetail line,
            BigDecimal amount,
            Map<UUID, DoctorAccumulator> doctorMap
    ) {
        UUID docId = line.doctorId();
        UUID mapKey = docId != null ? docId : UNASSIGNED_DOC_KEY;

        DoctorAccumulator doc = doctorMap.computeIfAbsent(
                mapKey,
                id -> new DoctorAccumulator(
                        docId,
                        docId != null ? line.doctorCode() : "UNASSIGNED",
                        docId != null ? line.doctorName() : "Chưa phân bổ bác sĩ"
                )
        );

        if (line.lineType() == InvoiceLineType.EXAM_FEE) {
            doc.examRevenue = doc.examRevenue.add(amount);
        } else if (line.lineType() == InvoiceLineType.SERVICE_FEE) {
            doc.clinicalServiceRevenue = doc.clinicalServiceRevenue.add(amount);
        } else if (line.lineType() == InvoiceLineType.MEDICINE_FEE) {
            doc.medicationRevenue = doc.medicationRevenue.add(amount);
        } else if (line.lineType() == InvoiceLineType.ADJUSTMENT) {
            doc.adjustmentRevenue = doc.adjustmentRevenue.add(amount);
        }
    }

    private String resolveServiceGroupCode(ClinicalServiceType serviceType) {
        if (serviceType == null) {
            return "OTHER";
        }
        return switch (serviceType) {
            case LAB_TEST -> "LAB_TEST";
            case IMAGING -> "IMAGING";
            case OTHER -> "OTHER";
        };
    }

    private BigDecimal calculatePercentage(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0 || part == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return part.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    private static class ServiceGroupAccumulator {
        final String groupCode;
        final String groupName;
        BigDecimal revenue = BigDecimal.ZERO;

        ServiceGroupAccumulator(String groupCode, String groupName) {
            this.groupCode = groupCode;
            this.groupName = groupName;
        }
    }

    private static class DoctorAccumulator {
        final UUID doctorId;
        final String doctorCode;
        final String doctorName;
        BigDecimal examRevenue = BigDecimal.ZERO;
        BigDecimal clinicalServiceRevenue = BigDecimal.ZERO;
        BigDecimal medicationRevenue = BigDecimal.ZERO;
        BigDecimal adjustmentRevenue = BigDecimal.ZERO;

        DoctorAccumulator(UUID doctorId, String doctorCode, String doctorName) {
            this.doctorId = doctorId;
            this.doctorCode = doctorCode;
            this.doctorName = doctorName;
        }
    }
}
