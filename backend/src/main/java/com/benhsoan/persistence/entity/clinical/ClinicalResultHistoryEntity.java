package com.benhsoan.persistence.entity.clinical;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.benhsoan.domain.clinical.enums.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clinical_result_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicalResultHistoryEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    UUID id;
    @Column(name = "clinical_result_id", nullable = false, columnDefinition = "BINARY(16)")
    UUID clinicalResultId;
    @Enumerated(EnumType.STRING)
    @Column(name = "old_result_type", length = 30)
    ClinicalResultType oldResultType;
    @Enumerated(EnumType.STRING)
    @Column(name = "new_result_type", length = 30)
    ClinicalResultType newResultType;
    @Column(name = "old_numeric_value", precision = 18, scale = 4)
    BigDecimal oldNumericValue;
    @Column(name = "new_numeric_value", precision = 18, scale = 4)
    BigDecimal newNumericValue;
    @Column(name = "old_text_value", columnDefinition = "TEXT")
    String oldTextValue;
    @Column(name = "new_text_value", columnDefinition = "TEXT")
    String newTextValue;
    @Column(name = "old_unit", length = 50)
    String oldUnit;
    @Column(name = "new_unit", length = 50)
    String newUnit;
    @Column(name = "old_reference_range", length = 255)
    String oldReferenceRange;
    @Column(name = "new_reference_range", length = 255)
    String newReferenceRange;
    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    ClinicalResultStatus oldStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 30)
    ClinicalResultStatus newStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "old_abnormal_flag", length = 30)
    ClinicalResultAbnormalFlag oldAbnormalFlag;
    @Enumerated(EnumType.STRING)
    @Column(name = "new_abnormal_flag", length = 30)
    ClinicalResultAbnormalFlag newAbnormalFlag;
    @Column(name = "old_conclusion", columnDefinition = "TEXT")
    String oldConclusion;
    @Column(name = "new_conclusion", columnDefinition = "TEXT")
    String newConclusion;
    @Column(name = "change_reason", nullable = false, columnDefinition = "TEXT")
    String changeReason;
    @Column(name = "changed_by", nullable = false, columnDefinition = "BINARY(16)")
    UUID changedBy;
    @Column(name = "changed_at", nullable = false)
    Instant changedAt;
}
