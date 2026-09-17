package com.benhsoan.domain.auditlog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;

/**
 * TC-04: administrative log records are immutable. The domain exposes no
 * mutators and the application port exposes no update/delete operations, so
 * there is no application-level path to edit or delete an existing record.
 */
class AuditLogImmutabilityTest {

    @Test
    void domainHasNoMutatorMethods() {
        Set<String> methodNames = Arrays.stream(AuditLog.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
        assertFalse(methodNames.stream().anyMatch(name -> name.startsWith("set")),
                "AuditLog domain must not expose setters");
    }

    @Test
    void repositoryPortExposesNoUpdateOrDelete() {
        Set<String> methodNames = Arrays.stream(AuditLogRepository.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
        assertFalse(methodNames.stream().anyMatch(
                        name -> name.equals("update") || name.equals("delete") || name.equals("remove")),
                "AuditLogRepository port must not expose update/delete operations");
        assertTrue(methodNames.contains("save"));
        assertTrue(methodNames.contains("findAdminOperationLogs"));
    }
}
