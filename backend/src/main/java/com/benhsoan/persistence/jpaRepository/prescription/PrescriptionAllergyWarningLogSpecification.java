package com.benhsoan.persistence.jpaRepository.prescription;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.benhsoan.persistence.entity.prescription.PrescriptionAllergyWarningLogEntity;
import com.benhsoan.port.dto.command.prescription.SearchPrescriptionAllergyWarningLogsQuery;

public final class PrescriptionAllergyWarningLogSpecification {

    private PrescriptionAllergyWarningLogSpecification() {
    }

    public static Specification<PrescriptionAllergyWarningLogEntity> build(
            SearchPrescriptionAllergyWarningLogsQuery query
    ) {
        return (root, criteriaQuery, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (query.doctorId() != null) {
                predicates.add(cb.equal(root.get("handledBy"), query.doctorId()));
            }
            if (query.patientId() != null) {
                predicates.add(cb.equal(root.get("patientId"), query.patientId()));
            }
            if (query.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("handledAt"), query.from()));
            }
            if (query.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("handledAt"), query.to()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
