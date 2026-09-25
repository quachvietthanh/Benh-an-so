package com.benhsoan.application.ucservice.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.patient.enums.PatientStatus;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.time.ClockPort;

/**
 * NCL-14-CN-010 TC-03: guardian-link review sweep.
 *
 * The sweep must (a) identify adult dependents that still carry a guardian link using the
 * canonical 18-year {@code PatientMinorPolicy} boundary, (b) notify through the existing
 * patient-portal notification infrastructure, (c) stay idempotent across repeated runs, and
 * (d) never remove the guardian link itself, because TC-03 only proposes the removal.
 */
@ExtendWith(MockitoExtension.class)
class ReviewAdultGuardianLinksServiceTest {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Instant NOW = Instant.parse("2026-09-25T03:00:00Z");

    @Mock private PatientRepository patientRepository;
    @Mock private PatientPortalNotificationCreator notificationCreator;
    @Mock private ClockPort clockPort;

    private ReviewAdultGuardianLinksService service;

    @BeforeEach
    void setUp() {
        service = new ReviewAdultGuardianLinksService(
                patientRepository, notificationCreator, clockPort);
    }

    private LocalDate today() {
        return NOW.atZone(CLINIC_ZONE).toLocalDate();
    }

    private Patient dependent(
            UUID patientId,
            LocalDate dateOfBirth,
            UUID guardianUserId,
            boolean active,
            PatientStatus status
    ) {
        return Patient.restore(
                patientId, "BN000002", "Nguyen Van Con", dateOfBirth, Gender.MALE,
                null, null, "123 Street", null, null, BloodType.O_POSITIVE,
                "Nguyen Van Cha", "Bo", "0909998877",
                "Nguyen Van Cha", "Bo", "0909998877", null,
                guardianUserId, "Nguyen Van Cha",
                active, NOW, NOW, UUID.randomUUID(), UUID.randomUUID(),
                true, NOW, "v1.0", false, null, null, false,
                status, null, null, null, null);
    }

    @Test
    @DisplayName("TC-03: nguoi phu thuoc dung 18 tuoi duoc ra soat va thong bao")
    void reviewsDependentWhoIsExactlyEighteenToday() {
        UUID dependentId = UUID.randomUUID();
        Patient adult = dependent(dependentId, today().minusYears(18), UUID.randomUUID(),
                true, PatientStatus.ACTIVE);

        when(clockPort.now()).thenReturn(NOW);
        when(patientRepository.findGuardianLinkedProfiles()).thenReturn(List.of(adult));

        assertEquals(1, service.reviewAdultGuardianLinks());
        verify(notificationCreator).createGuardianLinkReview(adult, NOW);
    }

    @Test
    @DisplayName("TC-03: 17 tuoi 364 ngay (chua du 18) khong bi ra soat")
    void doesNotReviewDependentOneDayBeforeAdulthood() {
        Patient almostAdult = dependent(UUID.randomUUID(), today().minusYears(18).plusDays(1),
                UUID.randomUUID(), true, PatientStatus.ACTIVE);

        when(clockPort.now()).thenReturn(NOW);
        when(patientRepository.findGuardianLinkedProfiles()).thenReturn(List.of(almostAdult));

        assertEquals(0, service.reviewAdultGuardianLinks());
        verify(notificationCreator, never()).createGuardianLinkReview(any(), any());
    }

    @Test
    @DisplayName("TC-03: nguoi phu thuoc con nho khong bi ra soat")
    void doesNotReviewMinors() {
        Patient minor = dependent(UUID.randomUUID(), today().minusYears(10),
                UUID.randomUUID(), true, PatientStatus.ACTIVE);

        when(clockPort.now()).thenReturn(NOW);
        when(patientRepository.findGuardianLinkedProfiles()).thenReturn(List.of(minor));

        assertEquals(0, service.reviewAdultGuardianLinks());
        verify(notificationCreator, never()).createGuardianLinkReview(any(), any());
    }

    @Test
    @DisplayName("TC-03: khong co nguoi phu thuoc nao thi khong lam gi")
    void doesNothingWhenNoGuardianLinksExist() {
        when(clockPort.now()).thenReturn(NOW);
        when(patientRepository.findGuardianLinkedProfiles()).thenReturn(List.of());

        assertEquals(0, service.reviewAdultGuardianLinks());
        verify(notificationCreator, never()).createGuardianLinkReview(any(), any());
    }

    @Test
    @DisplayName("TC-03: lien ket giam ho KHONG bao gio bi tu dong go bo")
    void sweepNeverUnlinksTheGuardianRelationship() {
        Patient adult = dependent(UUID.randomUUID(), today().minusYears(20),
                UUID.randomUUID(), true, PatientStatus.ACTIVE);

        when(clockPort.now()).thenReturn(NOW);
        when(patientRepository.findGuardianLinkedProfiles()).thenReturn(List.of(adult));

        service.reviewAdultGuardianLinks();

        // TC-03 only proposes the removal; the relationship survives the sweep untouched.
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("TC-03: loi khi thong bao mot ho so khong lam hong ca luot ra soat")
    void oneFailingRecipientDoesNotAbortSweep() {
        Patient first = dependent(UUID.randomUUID(), today().minusYears(19), UUID.randomUUID(),
                true, PatientStatus.ACTIVE);
        Patient second = dependent(UUID.randomUUID(), today().minusYears(20), UUID.randomUUID(),
                true, PatientStatus.ACTIVE);

        when(clockPort.now()).thenReturn(NOW);
        when(patientRepository.findGuardianLinkedProfiles()).thenReturn(List.of(first, second));
        org.mockito.Mockito.doThrow(new IllegalStateException("boom"))
                .when(notificationCreator).createGuardianLinkReview(first, NOW);

        assertEquals(2, service.reviewAdultGuardianLinks());
        verify(notificationCreator).createGuardianLinkReview(second, NOW);
        verify(notificationCreator, times(1)).createGuardianLinkReview(first, NOW);
    }
}
