package com.benhsoan.application.ucservice.visit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.port.outbound.security.CurrentUserPort;

class VisitSummaryAuthorizationTest {

    private CurrentUserPort currentUserPort;
    private VisitSummaryAuthorization authorization;

    private static final UUID DOCTOR_A_ID = UUID.randomUUID();
    private static final UUID DOCTOR_B_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        currentUserPort = mock(CurrentUserPort.class);
        authorization = new VisitSummaryAuthorization(currentUserPort);
    }

    @Test
    @DisplayName("Doctor can access their own visit summary")
    void doctor_canAccessOwnVisitSummary() {
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_A_ID);

        assertDoesNotThrow(() -> authorization.requireSummaryAccess(DOCTOR_A_ID));
    }

    @Test
    @DisplayName("Doctor cannot access another doctor's visit summary (BOLA prevention)")
    void doctor_cannotAccessAnotherDoctorsVisitSummary() {
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_A_ID);

        assertThrows(MedicalRecordAccessDeniedException.class, () ->
                authorization.requireSummaryAccess(DOCTOR_B_ID)
        );
    }

    @Test
    @DisplayName("Admin can access any visit summary")
    void admin_canAccessAnyVisitSummary() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);

        assertDoesNotThrow(() -> authorization.requireSummaryAccess(DOCTOR_B_ID));
    }

    @Test
    @DisplayName("Manager can access any visit summary")
    void manager_canAccessAnyVisitSummary() {
        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);

        assertDoesNotThrow(() -> authorization.requireSummaryAccess(DOCTOR_B_ID));
    }

    @Test
    @DisplayName("Receptionist can access any visit summary to print at counter")
    void receptionist_canAccessAnyVisitSummary() {
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);

        assertDoesNotThrow(() -> authorization.requireSummaryAccess(DOCTOR_B_ID));
    }

    @Test
    @DisplayName("User with other roles (e.g. Pharmacist) is denied access")
    void otherRole_isDeniedAccess() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(false);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(false);

        assertThrows(MedicalRecordAccessDeniedException.class, () ->
                authorization.requireSummaryAccess(DOCTOR_A_ID)
        );
    }
}
