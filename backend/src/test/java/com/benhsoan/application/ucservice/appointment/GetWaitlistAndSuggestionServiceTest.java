package com.benhsoan.application.ucservice.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.appointment.AppointmentWaitlist;
import com.benhsoan.domain.appointment.enums.TimePreference;
import com.benhsoan.domain.appointment.enums.WaitlistStatus;
import com.benhsoan.port.dto.command.appointment.CancelWaitlistEntryCommand;
import com.benhsoan.port.dto.query.appointment.GetAppointmentWaitlistQuery;
import com.benhsoan.port.dto.query.appointment.GetWaitlistSuggestionQuery;
import com.benhsoan.port.dto.result.appointment.AppointmentWaitlistResult;
import com.benhsoan.port.dto.result.appointment.WaitlistSuggestionResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentWaitlistRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class GetWaitlistAndSuggestionServiceTest {

    private final AppointmentWaitlistRepository waitlistRepository = mock(AppointmentWaitlistRepository.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final AppointmentWaitlistResultMapper mapper = new AppointmentWaitlistResultMapper(patientRepository, userRepository);

    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final UUID receptionistId = UUID.randomUUID();
    private final LocalDate desiredDate = LocalDate.of(2026, 10, 20);
    private final Instant now = Instant.parse("2026-10-01T08:00:00Z");

    @Test
    @DisplayName("NCL-03-CN-012-TC-02: Gợi ý người chờ đầu tiên theo thứ tự đăng ký (FIFO)")
    void shouldSuggestFirstWaitingPatient() {
        AppointmentWaitlist firstEntry = AppointmentWaitlist.create(
                patientId, doctorId, desiredDate, TimePreference.ANYTIME, "Cần khám sớm",
                receptionistId, now
        );

        when(waitlistRepository.findFirstWaitingByDoctorAndDate(doctorId, desiredDate))
                .thenReturn(Optional.of(firstEntry));

        GetWaitlistSuggestionService suggestionService = new GetWaitlistSuggestionService(waitlistRepository, mapper);

        Optional<WaitlistSuggestionResult> suggestionOpt = suggestionService.getSuggestion(
                new GetWaitlistSuggestionQuery(doctorId, desiredDate)
        );

        assertThat(suggestionOpt).isPresent();
        assertThat(suggestionOpt.get().waitlistId()).isEqualTo(firstEntry.getId());
        assertThat(suggestionOpt.get().patientId()).isEqualTo(patientId);
        assertThat(suggestionOpt.get().doctorId()).isEqualTo(doctorId);
        assertThat(suggestionOpt.get().note()).isEqualTo("Cần khám sớm");
    }

    @Test
    @DisplayName("Tra cứu danh sách chờ lọc theo trạng thái và sắp xếp FIFO")
    void shouldGetWaitlistWithFiltering() {
        AppointmentWaitlist entry1 = AppointmentWaitlist.create(
                patientId, doctorId, desiredDate, TimePreference.ANYTIME, "Chờ 1", receptionistId, now);
        AppointmentWaitlist entry2 = AppointmentWaitlist.create(
                UUID.randomUUID(), doctorId, desiredDate, TimePreference.ANYTIME, "Chờ 2", receptionistId, now.plusSeconds(60));

        when(waitlistRepository.findWaitlist(doctorId, desiredDate, WaitlistStatus.WAITING))
                .thenReturn(List.of(entry1, entry2));

        GetAppointmentWaitlistService service = new GetAppointmentWaitlistService(waitlistRepository, mapper);

        List<AppointmentWaitlistResult> results = service.getWaitlist(
                new GetAppointmentWaitlistQuery(doctorId, desiredDate, WaitlistStatus.WAITING)
        );

        assertThat(results).hasSize(2);
        assertThat(results.get(0).id()).isEqualTo(entry1.getId());
        assertThat(results.get(1).id()).isEqualTo(entry2.getId());
    }

    @Test
    @DisplayName("Hủy mục chờ thành công và ghi log kiểm toán")
    void shouldCancelWaitlistEntrySuccessfully() {
        AppointmentWaitlist entry = AppointmentWaitlist.create(
                patientId, doctorId, desiredDate, TimePreference.ANYTIME, "Chờ", receptionistId, now);

        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(receptionistId);
        when(clockPort.now()).thenReturn(now.plusSeconds(300));
        when(waitlistRepository.findByIdForUpdate(entry.getId())).thenReturn(Optional.of(entry));
        when(waitlistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CancelWaitlistEntryService cancelService = new CancelWaitlistEntryService(
                waitlistRepository, mapper, auditLogRepository, currentUserPort, clockPort);

        AppointmentWaitlistResult result = cancelService.cancel(
                new CancelWaitlistEntryCommand(entry.getId(), "Bệnh nhân báo không đến được")
        );

        assertThat(result.status()).isEqualTo(WaitlistStatus.CANCELLED);
        assertThat(result.cancelReason()).isEqualTo("Bệnh nhân báo không đến được");
        verify(waitlistRepository).save(any());
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("Tra cứu danh sách chờ không truyền ngày sẽ tự động giới hạn từ ngày hôm nay trở đi")
    void shouldFilterFromTodayWhenDesiredDateIsNull() {
        AppointmentWaitlist entry = AppointmentWaitlist.create(
                patientId, doctorId, desiredDate, TimePreference.ANYTIME, "Chờ tương lai", receptionistId, now);

        LocalDate today = now.atZone(GetAppointmentWaitlistService.CLINIC_ZONE).toLocalDate();

        when(clockPort.now()).thenReturn(now);
        when(waitlistRepository.findWaitlist(doctorId, null, today, WaitlistStatus.WAITING))
                .thenReturn(List.of(entry));

        GetAppointmentWaitlistService service = new GetAppointmentWaitlistService(waitlistRepository, mapper, clockPort);

        List<AppointmentWaitlistResult> results = service.getWaitlist(
                new GetAppointmentWaitlistQuery(doctorId, null, WaitlistStatus.WAITING)
        );

        assertThat(results).hasSize(1);
        verify(waitlistRepository).findWaitlist(doctorId, null, today, WaitlistStatus.WAITING);
    }
}

