package com.benhsoan.persistence.jpaRepository.auditlog;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.benhsoan.persistence.entity.auditlog.AuditLogEntity;
import com.benhsoan.port.dto.command.auditlog.SearchAuditLogCommand;

import jakarta.persistence.criteria.Predicate;

public final class AuditLogSpecification {

    private AuditLogSpecification() {
    }

    public static Specification<AuditLogEntity> build(
            SearchAuditLogCommand command
    ) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (command.userId() != null) {
                predicates.add(
                        cb.equal(
                                root.get("userId"),
                                command.userId()
                        )
                );
            }

            if (command.actionType() != null) {
                predicates.add(
                        cb.equal(
                                root.get("actionType"),
                                command.actionType()
                        )
                );
            }

            if (command.resourceType() != null) {
                predicates.add(
                        cb.equal(
                                root.get("resourceType"),
                                command.resourceType()
                        )
                );
            }

            if (command.resourceId() != null) {
                predicates.add(
                        cb.equal(
                                root.get("resourceId"),
                                command.resourceId()
                        )
                );
            }

            if (command.fromCreatedAt() != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("createdAt"),
                                command.fromCreatedAt()
                        )
                );
            }

            if (command.toCreatedAt() != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(
                                root.get("createdAt"),
                                command.toCreatedAt()
                        )
                );
            }

            query.orderBy(
                    cb.desc(root.get("createdAt"))
            );

            return cb.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }
}
