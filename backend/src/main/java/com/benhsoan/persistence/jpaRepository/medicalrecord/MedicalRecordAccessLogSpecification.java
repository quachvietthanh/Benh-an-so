package com.benhsoan.persistence.jpaRepository.medicalrecord;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAccessLogEntity;
import com.benhsoan.port.dto.command.medicalrecord.GetMedicalRecordAccessLogsQuery;

public final class MedicalRecordAccessLogSpecification {

    private MedicalRecordAccessLogSpecification() {
    }

    public static Specification<MedicalRecordAccessLogEntity> build(
            GetMedicalRecordAccessLogsQuery query
    ) {
        return (root, criteriaQuery, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (query.accessedBy() != null) {
                predicates.add(cb.equal(root.get("accessedBy"), query.accessedBy()));
            }
            if (query.patientId() != null) {
                predicates.add(cb.equal(root.get("patientId"), query.patientId()));
            }
            if (query.medicalRecordId() != null) {
                predicates.add(cb.equal(root.get("medicalRecordId"), query.medicalRecordId()));
            }
            if (query.visitId() != null) {
                predicates.add(cb.equal(root.get("visitId"), query.visitId()));
            }
            if (query.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("accessedAt"), query.from()));
            }
            if (query.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("accessedAt"), query.to()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
