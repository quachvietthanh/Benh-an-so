package com.benhsoan.domain.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Role permission model tests")
class RolePermissionTest {

    @Test
    void roleUsesPersistedPermissionCodesRatherThanEnumValues() {
        Role role = Role.create("TEST_ROLE", "Test role", false,
                Set.of(Permission.fromCode("PATIENT_READ"), Permission.fromCode("PATIENT_CREATE")));

        assertTrue(role.hasPermission("PATIENT_READ"));
        assertTrue(role.hasAnyPermission("ROLE_READ", "PATIENT_CREATE"));
        assertTrue(role.hasAllPermissions("PATIENT_READ", "PATIENT_CREATE"));
        assertFalse(role.hasPermission("PATIENT_DELETE"));
    }
}
