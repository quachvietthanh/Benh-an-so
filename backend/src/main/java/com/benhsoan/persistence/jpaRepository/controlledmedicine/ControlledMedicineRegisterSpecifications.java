package com.benhsoan.persistence.jpaRepository.controlledmedicine;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.benhsoan.persistence.entity.controlledmedicine.ControlledMedicineRegisterEntity;

public final class ControlledMedicineRegisterSpecifications {

    private ControlledMedicineRegisterSpecifications() {
    }

    public static Specification<ControlledMedicineRegisterEntity> hasPatientId(UUID patientId) {
        return (root, query, cb) -> patientId == null
                ? cb.conjunction()
                : cb.equal(root.get("patientId"), patientId);
    }

    public static Specification<ControlledMedicineRegisterEntity> hasMedicineId(UUID medicineId) {
        return (root, query, cb) -> medicineId == null
                ? cb.conjunction()
                : cb.equal(root.get("medicineId"), medicineId);
    }

    public static Specification<ControlledMedicineRegisterEntity> dispensedAtBetween(
            Instant fromInclusive,
            Instant toExclusive
    ) {
        return (root, query, cb) -> {
            if (fromInclusive == null && toExclusive == null) {
                return cb.conjunction();
            }
            if (fromInclusive == null) {
                return cb.lessThan(root.get("dispensedAt"), toExclusive);
            }
            if (toExclusive == null) {
                return cb.greaterThanOrEqualTo(root.get("dispensedAt"), fromInclusive);
            }
            return cb.and(
                    cb.greaterThanOrEqualTo(root.get("dispensedAt"), fromInclusive),
                    cb.lessThan(root.get("dispensedAt"), toExclusive)
            );
        };
    }
}
