package com.benhsoan.domain.clinical;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;
import com.benhsoan.domain.clinical.enums.ClinicalResultStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultType;
import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClinicalResultHistory {

    private UUID id, clinicalResultId, changedBy;
    private ClinicalResultType oldResultType, newResultType;
    private BigDecimal oldNumericValue, newNumericValue;
    private String oldTextValue, newTextValue, oldUnit, newUnit, oldReferenceRange, newReferenceRange, oldConclusion, newConclusion, changeReason;
    private ClinicalResultAbnormalFlag oldAbnormalFlag, newAbnormalFlag;
    private ClinicalResultStatus oldStatus, newStatus;
    private Instant changedAt;

    private ClinicalResultHistory(UUID id, UUID resultId, ClinicalResultType ot, ClinicalResultType nt, BigDecimal on, BigDecimal nn, String ox, String nx, String ou, String nu, String or, String nr, ClinicalResultAbnormalFlag of, ClinicalResultAbnormalFlag nf, String oc, String nc, ClinicalResultStatus os, ClinicalResultStatus ns, String reason, UUID by, Instant at) {
        this.id = Objects.requireNonNull(id);
        clinicalResultId = Objects.requireNonNull(resultId);
        oldResultType = ot;
        newResultType = nt;
        oldNumericValue = on;
        newNumericValue = nn;
        oldTextValue = ox;
        newTextValue = nx;
        oldUnit = ou;
        newUnit = nu;
        oldReferenceRange = or;
        newReferenceRange = nr;
        oldAbnormalFlag = of;
        newAbnormalFlag = nf;
        oldConclusion = oc;
        newConclusion = nc;
        oldStatus = os;
        newStatus = ns;
        changeReason = Guard.require(reason, "Change reason");
        changedBy = Objects.requireNonNull(by);
        changedAt = Objects.requireNonNull(at);
    }

    public static ClinicalResultHistory create(ClinicalResult oldResult, ClinicalResult newResult, String reason, UUID by, Instant at) {
        Objects.requireNonNull(oldResult, "Old clinical result is required");
        Objects.requireNonNull(newResult, "New clinical result is required");
        if (!oldResult.getId().equals(newResult.getId())) {
            throw new ValidationException("Clinical result history must compare versions of the same result.");
        }
        return new ClinicalResultHistory(UUID.randomUUID(), oldResult.getId(), oldResult.getResultType(), newResult.getResultType(), oldResult.getNumericValue(), newResult.getNumericValue(), oldResult.getTextValue(), newResult.getTextValue(), oldResult.getUnit(), newResult.getUnit(), oldResult.getReferenceRange(), newResult.getReferenceRange(), oldResult.getAbnormalFlag(), newResult.getAbnormalFlag(), oldResult.getConclusion(), newResult.getConclusion(), oldResult.getStatus(), newResult.getStatus(), reason, by, at);
    }

    public static ClinicalResultHistory restore(UUID id, UUID resultId, ClinicalResultType oldResultType, ClinicalResultType newResultType, BigDecimal oldNumericValue, BigDecimal newNumericValue, String oldTextValue, String newTextValue, String oldUnit, String newUnit, String oldReferenceRange, String newReferenceRange, ClinicalResultAbnormalFlag oldAbnormalFlag, ClinicalResultAbnormalFlag newAbnormalFlag, String oldConclusion, String newConclusion, ClinicalResultStatus oldStatus, ClinicalResultStatus newStatus, String changeReason, UUID changedBy, Instant changedAt) {
        return new ClinicalResultHistory(id, resultId, oldResultType, newResultType, oldNumericValue, newNumericValue, oldTextValue, newTextValue, oldUnit, newUnit, oldReferenceRange, newReferenceRange, oldAbnormalFlag, newAbnormalFlag, oldConclusion, newConclusion, oldStatus, newStatus, changeReason, changedBy, changedAt);
    }
}
