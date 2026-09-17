package com.benhsoan.domain.medicalrecord.exception;

import java.util.List;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PendingClinicalOrdersWarningException extends MedicalRecordException {

    private final List<String> pendingServices;

    public PendingClinicalOrdersWarningException(List<String> pendingServices) {
        super(DomainErrorCode.PENDING_CLINICAL_ORDERS_WARNING,
                "Lượt khám còn chỉ định cận lâm sàng chưa có kết quả ("
                        + (pendingServices != null && !pendingServices.isEmpty() ? String.join(", ", pendingServices) : "đang chờ kết quả")
                        + "). Vui lòng xác nhận trước khi ký bệnh án (QTN-17).");
        this.pendingServices = pendingServices != null ? List.copyOf(pendingServices) : List.of();
    }

    public List<String> getPendingServices() {
        return pendingServices;
    }
}
