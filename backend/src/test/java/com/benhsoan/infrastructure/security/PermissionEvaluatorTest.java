package com.benhsoan.infrastructure.security;

import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.enums.Permission;
import com.benhsoan.infrastructure.security.annotation.CheckPermission;
import com.benhsoan.infrastructure.security.annotation.PermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PermissionEvaluator Tests")
@ExtendWith(MockitoExtension.class)
class PermissionEvaluatorTest {

    private PermissionEvaluator evaluator;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionEvaluator mockEvaluator;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @BeforeEach
    void setUp() {
        evaluator = new PermissionEvaluator(roleRepository);
    }

    @Test
    @DisplayName("Admin should have all permissions")
    void adminHasAllPermissions() {
        Role adminRole = Role.create("ADMIN", "Admin role", true, Set.of(Permission.values()));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));

        authenticateAs("admin", "ADMIN");
        assertTrue(evaluator.hasPermission(Permission.USER_CREATE));
        assertTrue(evaluator.hasPermission(Permission.AUDIT_READ));
        assertTrue(evaluator.hasPermission(Permission.ROLE_CREATE));
        assertTrue(evaluator.hasPermission(Permission.PATIENT_CREATE));
    }

    @Test
    @DisplayName("Doctor should have medical permissions but not admin permissions")
    void doctorHasMedicalPermissions() {
        Role doctorRole = Role.create("DOCTOR", "Doctor role", true, Set.of(
                Permission.PATIENT_CREATE, Permission.PATIENT_READ, Permission.PATIENT_UPDATE,
                Permission.MEDICAL_RECORD_CREATE, Permission.MEDICAL_RECORD_READ, Permission.MEDICAL_RECORD_UPDATE, Permission.MEDICAL_RECORD_UPDATE_STATUS,
                Permission.PRESCRIPTION_CREATE, Permission.PRESCRIPTION_READ, Permission.PRESCRIPTION_UPDATE, Permission.PRESCRIPTION_DELETE,
                Permission.DIAGNOSIS_CREATE, Permission.DIAGNOSIS_READ, Permission.DIAGNOSIS_UPDATE,
                Permission.APPOINTMENT_CREATE, Permission.APPOINTMENT_READ, Permission.APPOINTMENT_UPDATE,
                Permission.VITAL_SIGN_CREATE, Permission.VITAL_SIGN_READ, Permission.VITAL_SIGN_UPDATE
        ));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(doctorRole));

        authenticateAs("doctor", "DOCTOR");
        assertTrue(evaluator.hasPermission(Permission.PATIENT_CREATE));
        assertTrue(evaluator.hasPermission(Permission.MEDICAL_RECORD_CREATE));
        assertTrue(evaluator.hasPermission(Permission.PRESCRIPTION_CREATE));
        assertTrue(evaluator.hasPermission(Permission.DIAGNOSIS_CREATE));

        assertFalse(evaluator.hasPermission(Permission.USER_CREATE));
        assertFalse(evaluator.hasPermission(Permission.AUDIT_READ));
        assertFalse(evaluator.hasPermission(Permission.ROLE_CREATE));
    }

    @Test
    @DisplayName("Nurse should have limited read permissions")
    void nurseHasLimitedPermissions() {
        Role nurseRole = Role.create("NURSE", "Nurse role", true, Set.of(
                Permission.PATIENT_READ,
                Permission.MEDICAL_RECORD_READ, Permission.MEDICAL_RECORD_UPDATE_STATUS,
                Permission.APPOINTMENT_READ,
                Permission.VITAL_SIGN_CREATE, Permission.VITAL_SIGN_READ, Permission.VITAL_SIGN_UPDATE
        ));
        when(roleRepository.findByName("NURSE")).thenReturn(Optional.of(nurseRole));

        authenticateAs("nurse", "NURSE");
        assertTrue(evaluator.hasPermission(Permission.PATIENT_READ));
        assertTrue(evaluator.hasPermission(Permission.MEDICAL_RECORD_READ));
        assertTrue(evaluator.hasPermission(Permission.VITAL_SIGN_CREATE));

        assertFalse(evaluator.hasPermission(Permission.PATIENT_CREATE));
        assertFalse(evaluator.hasPermission(Permission.MEDICAL_RECORD_CREATE));
        assertFalse(evaluator.hasPermission(Permission.PRESCRIPTION_READ));
    }

    @Test
    @DisplayName("Receptionist should have appointment and invoice permissions")
    void receptionistHasAppointmentPermissions() {
        Role receptionistRole = Role.create("RECEPTIONIST", "Receptionist role", true, Set.of(
                Permission.PATIENT_CREATE, Permission.PATIENT_READ, Permission.PATIENT_UPDATE,
                Permission.APPOINTMENT_CREATE, Permission.APPOINTMENT_READ, Permission.APPOINTMENT_UPDATE, Permission.APPOINTMENT_DELETE,
                Permission.INVOICE_CREATE, Permission.INVOICE_READ, Permission.INVOICE_UPDATE
        ));
        when(roleRepository.findByName("RECEPTIONIST")).thenReturn(Optional.of(receptionistRole));

        authenticateAs("receptionist", "RECEPTIONIST");
        assertTrue(evaluator.hasPermission(Permission.PATIENT_CREATE));
        assertTrue(evaluator.hasPermission(Permission.APPOINTMENT_CREATE));
        assertTrue(evaluator.hasPermission(Permission.INVOICE_CREATE));

        assertFalse(evaluator.hasPermission(Permission.MEDICAL_RECORD_READ));
        assertFalse(evaluator.hasPermission(Permission.PRESCRIPTION_READ));
        assertFalse(evaluator.hasPermission(Permission.VITAL_SIGN_READ));
    }

    @Test
    @DisplayName("Pharmacist should have pharmacy permissions")
    void pharmacistHasPharmacyPermissions() {
        Role pharmacistRole = Role.create("PHARMACIST", "Pharmacist role", true, Set.of(
                Permission.PRESCRIPTION_READ, Permission.PRESCRIPTION_UPDATE_STATUS,
                Permission.PHARMACY_CREATE, Permission.PHARMACY_READ, Permission.PHARMACY_UPDATE
        ));
        when(roleRepository.findByName("PHARMACIST")).thenReturn(Optional.of(pharmacistRole));

        authenticateAs("pharmacist", "PHARMACIST");
        assertTrue(evaluator.hasPermission(Permission.PRESCRIPTION_READ));
        assertTrue(evaluator.hasPermission(Permission.PRESCRIPTION_UPDATE_STATUS));
        assertTrue(evaluator.hasPermission(Permission.PHARMACY_CREATE));
        assertTrue(evaluator.hasPermission(Permission.PHARMACY_READ));
        assertTrue(evaluator.hasPermission(Permission.PHARMACY_UPDATE));

        assertFalse(evaluator.hasPermission(Permission.PATIENT_READ));
        assertFalse(evaluator.hasPermission(Permission.MEDICAL_RECORD_READ));
        assertFalse(evaluator.hasPermission(Permission.APPOINTMENT_READ));
    }

    @Test
    @DisplayName("No authentication should return empty permissions")
    void noAuthReturnsEmpty() {
        SecurityContextHolder.clearContext();
        assertTrue(evaluator.getCurrentUserPermissions().isEmpty());
        assertFalse(evaluator.hasPermission(Permission.PATIENT_READ));
        assertFalse(evaluator.hasAnyPermission(Permission.PATIENT_READ, Permission.USER_CREATE));
    }

    @Test
    @DisplayName("hasAnyRole should work correctly")
    void hasAnyRoleWorks() {
        authenticateAs("admin", "ADMIN");
        assertTrue(evaluator.hasAnyRole("ADMIN", "DOCTOR"));
        assertTrue(evaluator.hasRole("ADMIN"));
        assertFalse(evaluator.hasRole("DOCTOR"));
    }

    @Test
    @DisplayName("PermissionAspect should throw AccessDeniedException when user lacks permission with ANY operator")
    void aspectThrowsAccessDeniedForAnyOperator() throws Throwable {
        PermissionAspect aspect = new PermissionAspect(mockEvaluator);
        CheckPermission checkPermission = mock(CheckPermission.class);

        when(checkPermission.value()).thenReturn(new Permission[]{Permission.USER_CREATE});
        when(checkPermission.operator()).thenReturn(CheckPermission.Operator.ANY);
        when(mockEvaluator.hasAnyPermission(Permission.USER_CREATE)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> {
            aspect.checkPermission(joinPoint, checkPermission);
        });

        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("PermissionAspect should proceed when user has permission with ANY operator")
    void aspectProceedsForAnyOperator() throws Throwable {
        PermissionAspect aspect = new PermissionAspect(mockEvaluator);
        CheckPermission checkPermission = mock(CheckPermission.class);

        when(checkPermission.value()).thenReturn(new Permission[]{Permission.USER_CREATE});
        when(checkPermission.operator()).thenReturn(CheckPermission.Operator.ANY);
        when(mockEvaluator.hasAnyPermission(Permission.USER_CREATE)).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.checkPermission(joinPoint, checkPermission);

        assertEquals("success", result);
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("PermissionAspect should throw AccessDeniedException when user lacks all permissions with ALL operator")
    void aspectThrowsAccessDeniedForAllOperator() throws Throwable {
        PermissionAspect aspect = new PermissionAspect(mockEvaluator);
        CheckPermission checkPermission = mock(CheckPermission.class);

        when(checkPermission.value()).thenReturn(new Permission[]{Permission.USER_CREATE, Permission.PATIENT_CREATE});
        when(checkPermission.operator()).thenReturn(CheckPermission.Operator.ALL);
        when(mockEvaluator.hasAllPermissions(Permission.USER_CREATE, Permission.PATIENT_CREATE)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> {
            aspect.checkPermission(joinPoint, checkPermission);
        });

        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("PermissionAspect should proceed when user has all permissions with ALL operator")
    void aspectProceedsForAllOperator() throws Throwable {
        PermissionAspect aspect = new PermissionAspect(mockEvaluator);
        CheckPermission checkPermission = mock(CheckPermission.class);

        when(checkPermission.value()).thenReturn(new Permission[]{Permission.USER_CREATE, Permission.PATIENT_CREATE});
        when(checkPermission.operator()).thenReturn(CheckPermission.Operator.ALL);
        when(mockEvaluator.hasAllPermissions(Permission.USER_CREATE, Permission.PATIENT_CREATE)).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("proceeded");

        Object result = aspect.checkPermission(joinPoint, checkPermission);

        assertEquals("proceeded", result);
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("Doctor permissions should match PermissionEvaluator role mapping")
    void doctorPermissionsMatchRoleMapping() {
        Role doctorRole = Role.create("DOCTOR", "Doctor role", true, Set.of(
                Permission.PATIENT_CREATE, Permission.PATIENT_READ, Permission.PATIENT_UPDATE,
                Permission.MEDICAL_RECORD_CREATE, Permission.MEDICAL_RECORD_READ, Permission.MEDICAL_RECORD_UPDATE, Permission.MEDICAL_RECORD_UPDATE_STATUS,
                Permission.PRESCRIPTION_CREATE, Permission.PRESCRIPTION_READ, Permission.PRESCRIPTION_UPDATE, Permission.PRESCRIPTION_DELETE,
                Permission.DIAGNOSIS_CREATE, Permission.DIAGNOSIS_READ, Permission.DIAGNOSIS_UPDATE,
                Permission.VITAL_SIGN_CREATE, Permission.VITAL_SIGN_READ, Permission.VITAL_SIGN_UPDATE,
                Permission.APPOINTMENT_CREATE, Permission.APPOINTMENT_READ, Permission.APPOINTMENT_UPDATE
        ));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(doctorRole));

        authenticateAs("doctor", "DOCTOR");
        assertTrue(evaluator.hasPermission(Permission.PATIENT_CREATE));
        assertTrue(evaluator.hasPermission(Permission.PATIENT_READ));
        assertTrue(evaluator.hasPermission(Permission.PATIENT_UPDATE));
        assertTrue(evaluator.hasPermission(Permission.MEDICAL_RECORD_CREATE));
        assertTrue(evaluator.hasPermission(Permission.MEDICAL_RECORD_READ));
        assertTrue(evaluator.hasPermission(Permission.MEDICAL_RECORD_UPDATE));
        assertTrue(evaluator.hasPermission(Permission.MEDICAL_RECORD_UPDATE_STATUS));
        assertTrue(evaluator.hasPermission(Permission.DIAGNOSIS_CREATE));
        assertTrue(evaluator.hasPermission(Permission.DIAGNOSIS_READ));
        assertTrue(evaluator.hasPermission(Permission.DIAGNOSIS_UPDATE));
        assertTrue(evaluator.hasPermission(Permission.VITAL_SIGN_CREATE));
        assertTrue(evaluator.hasPermission(Permission.VITAL_SIGN_READ));
        assertTrue(evaluator.hasPermission(Permission.VITAL_SIGN_UPDATE));

        assertFalse(evaluator.hasPermission(Permission.USER_CREATE));
        assertFalse(evaluator.hasPermission(Permission.USER_DELETE));
        assertFalse(evaluator.hasPermission(Permission.PATIENT_DELETE));
        assertFalse(evaluator.hasPermission(Permission.PHARMACY_CREATE));
        assertFalse(evaluator.hasPermission(Permission.INVOICE_CREATE));
        assertFalse(evaluator.hasPermission(Permission.AUDIT_READ));
    }

    private void authenticateAs(String username, String role) {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                )
        );
    }
}
