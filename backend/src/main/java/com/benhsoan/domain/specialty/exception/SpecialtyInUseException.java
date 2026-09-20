package com.benhsoan.domain.specialty.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

import lombok.Getter;

@Getter
public class SpecialtyInUseException extends DomainException {

    private final long assignedDoctorCount;
    private final long activeTemplateCount;

    public SpecialtyInUseException(long assignedDoctorCount, long activeTemplateCount) {
        super(DomainErrorCode.SPECIALTY_IN_USE,
                String.format("Chuyên khoa đang được gán cho %d bác sĩ và %d mẫu bệnh án đang hoạt động. Cần xác nhận trước khi ngừng dùng.",
                        assignedDoctorCount, activeTemplateCount));
        this.assignedDoctorCount = assignedDoctorCount;
        this.activeTemplateCount = activeTemplateCount;
    }
}
