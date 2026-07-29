package com.benhsoan.domain.clinical;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;
import com.benhsoan.domain.clinical.enums.ClinicalResultStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultType;
import com.benhsoan.domain.clinical.exception.ClinicalResultAlreadyFinalizedException;
import com.benhsoan.domain.clinical.exception.ClinicalResultInvalidStatusException;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClinicalResult {

    private UUID id, clinicalOrderItemId, visitId, enteredBy, updatedBy;
    private ClinicalResultType resultType;
    private BigDecimal numericValue;
    private String textValue, unit, referenceRange, conclusion;
    private ClinicalResultAbnormalFlag abnormalFlag;
    private ClinicalResultStatus status;
    private Instant enteredAt, updatedAt;

    private ClinicalResult(UUID id, UUID item, UUID visit, ClinicalResultType type, BigDecimal number, String text, String unit, String range, ClinicalResultAbnormalFlag flag, String conclusion, ClinicalResultStatus status, UUID enteredBy, Instant enteredAt, UUID updatedBy, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        clinicalOrderItemId = Objects.requireNonNull(item);
        visitId = Objects.requireNonNull(visit);
        this.resultType = Objects.requireNonNull(type);
        numericValue = number;
        textValue = text;
        this.unit = unit;
        referenceRange = range;
        abnormalFlag = flag == null ? ClinicalResultAbnormalFlag.UNKNOWN : flag;
        this.conclusion = conclusion;
        this.status = Objects.requireNonNull(status);
        this.enteredBy = Objects.requireNonNull(enteredBy);
        this.enteredAt = Objects.requireNonNull(enteredAt);
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
        validate();
    }

    public static ClinicalResult create(UUID item, UUID visit, ClinicalResultType type, BigDecimal number, String text, String unit, String range, ClinicalResultAbnormalFlag flag, String conclusion, UUID by, Instant at) {
        return new ClinicalResult(UUID.randomUUID(), item, visit, type, number, text, unit, range, flag, conclusion, ClinicalResultStatus.DRAFT, by, at, null, null);
    }

    public static ClinicalResult restore(UUID id, UUID item, UUID visit, ClinicalResultType type, BigDecimal number, String text, String unit, String range, ClinicalResultAbnormalFlag flag, String conclusion, ClinicalResultStatus status, UUID enteredBy, Instant enteredAt, UUID updatedBy, Instant updatedAt) {
        return new ClinicalResult(id, item, visit, type, number, text, unit, range, flag, conclusion, status, enteredBy, enteredAt, updatedBy, updatedAt);
    }

    public void updateResult(BigDecimal n, String t, String u, String r, ClinicalResultAbnormalFlag f, String c, UUID by, Instant at) {
        if (status == ClinicalResultStatus.FINAL) {
            throw new ClinicalResultAlreadyFinalizedException();
        
        }numericValue = n;
        textValue = t;
        unit = u;
        referenceRange = r;
        abnormalFlag = f == null ? ClinicalResultAbnormalFlag.UNKNOWN : f;
        conclusion = c;
        updatedBy = Objects.requireNonNull(by);
        updatedAt = Objects.requireNonNull(at);
        validate();
    }

    public void finalizeResult(UUID by, Instant at) {
        if (status != ClinicalResultStatus.DRAFT && status != ClinicalResultStatus.CORRECTED) {
            throw new ClinicalResultInvalidStatusException("Only draft or corrected results can be finalized.");
        
        }status = ClinicalResultStatus.FINAL;
        updatedBy = Objects.requireNonNull(by);
        updatedAt = Objects.requireNonNull(at);
    }

    public void markCorrected(UUID by, Instant at) {
        if (status != ClinicalResultStatus.FINAL) {
            throw new ClinicalResultInvalidStatusException("Only finalized results can be corrected.");
        
        }status = ClinicalResultStatus.CORRECTED;
        updatedBy = Objects.requireNonNull(by);
        updatedAt = Objects.requireNonNull(at);
    }

    private void validate() {
        if (resultType == ClinicalResultType.NUMBER && numericValue == null) {
            throw new ValidationException("Numeric result requires numeric value.");
        
        }if (resultType == ClinicalResultType.TEXT && (textValue == null || textValue.isBlank())) {
            throw new ValidationException("Text result requires text value.");
        
        }if (resultType == ClinicalResultType.MIXED && numericValue == null && (textValue == null || textValue.isBlank()) && (conclusion == null || conclusion.isBlank())) {
            throw new ValidationException("Mixed result requires content.");
    
        }}
}
