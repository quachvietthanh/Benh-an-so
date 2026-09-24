package com.benhsoan.application.ucservice.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.appointment.enums.TimePreference;
import com.benhsoan.domain.appointment.enums.WaitlistStatus;
import com.benhsoan.domain.appointment.exception.DoctorHasAvailableSlotsException;
import com.benhsoan.domain.appointment.exception.DoctorNotWorkingException;
import com.benhsoan.domain.appointment.exception.PatientAlreadyInWaitlistException;
import com.benhsoan.domain.appointment.exception.UnauthorizedAppointmentOperationException;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.appointment.AddToWaitlistCommand;
import com.benhsoan.port.dto.result.appointment.AppointmentWaitlistResult;
import com.benhsoan.port.dto.result.appointment.DoctorAvailableSlotResult;
import com.benhsoan.port.inbound.appointment.GetDoctorAvailableSlotsUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentWaitlistRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class AddToWaitlistServiceTest {

    private AppointmentWaitlistRepository waitlistRepository;
    private PatientRepository patientRepository;
    private UserRepository userRepository;
    private GetDoctorAvailableSlotsUseCase availableSlotsUseCase;
    private DoctorScheduleValidator scheduleValidator;
    private AppointmentWaitlistResultMapper resultMapper;
    private AuditLogRepository auditLogRepository;
    private CurrentUserPort currentUserPort;
    private ClockPort clockPort;

    private AddToWaitlistService service;

    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final UUID receptionistId = UUID.randomUUID();
    private final LocalDate desiredDate = LocalDate.of(2026, 10, 20);
    private final Instant now = Instant.parse("2026-10-01T08:00:00Z");

    @BeforeEach
    void setUp() {
        waitlistRepository = mock(AppointmentWaitlistRepository.class);
        patientRepository = mock(PatientRepository.class);
        userRepository = mock(UserRepository.class);
        availableSlotsUseCase = mock(GetDoctorAvailableSlotsUseCase.class);
        scheduleValidator = mock(DoctorScheduleValidator.class);
        resultMapper = new AppointmentWaitlistResultMapper(patientRepository, userRepository);
        auditLogRepository = mock(AuditLogRepository.class);
        currentUserPort = mock(CurrentUserPort.class);
        clockPort = mock(ClockPort.class);

        when(clockPort.now()).thenReturn(now);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(receptionistId);

        Patient patient = mock(Patient.class);
        when(patient.getFullName()).thenReturn("Nguyễn Văn A");
        when(patient.getPhone()).thenReturn("0901234567");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        User doctor = mock(User.class);
        when(doctor.getId()).thenReturn(doctorId);
        when(doctor.isActive()).thenReturn(true);
        when(doctor.getFullName()).thenReturn("Doctor One");
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        when(scheduleValidator.resolveWorkingHours(doctorId, desiredDate))
                .thenReturn(Optional.of(new DoctorScheduleValidator.EffectiveWorkingHours(
                        LocalTime.of(8, 0), LocalTime.of(17, 0))));

        service = new AddToWaitlistService(
                waitlistRepository,
                patientRepository,
                userRepository,
                availableSlotsUseCase,
                scheduleValidator,
                resultMapper,
                auditLogRepository,
                currentUserPort,
                clockPort
        );
    }

    @Test
    @DisplayName("NCL-03-CN-012-TC-01: Thêm bệnh nhân vào danh sách chờ thành công khi bác sĩ kín lịch")
    void shouldAddToWaitlistSuccessfullyWhenDoctorFullyBooked() {
        List<DoctorAvailableSlotResult> fullyBookedSlots = List.of(
                new DoctorAvailableSlotResult(Instant.parse("2026-10-20T01:00:00Z"), Instant.parse("2026-10-20T01:30:00Z"), false),
                new DoctorAvailableSlotResult(Instant.parse("2026-10-20T01:30:00Z"), Instant.parse("2026-10-20T02:00:00Z"), false)
        );
        when(availableSlotsUseCase.getAvailableSlots(any())).thenReturn(fullyBookedSlots);
        when(waitlistRepository.existsByPatientIdAndDoctorIdAndDesiredDateAndStatus(
                patientId, doctorId, desiredDate, WaitlistStatus.WAITING)).thenReturn(false);

        when(waitlistRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AddToWaitlistCommand command = AddToWaitlistCommand.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .desiredDate(desiredDate)
                .timePreference(TimePreference.ANYTIME)
                .note("Bệnh nhân cần khám dạ dày")
                .build();

        AppointmentWaitlistResult result = service.addToWaitlist(command);

        assertThat(result).isNotNull();
        assertThat(result.patientId()).isEqualTo(patientId);
        assertThat(result.doctorId()).isEqualTo(doctorId);
        assertThat(result.desiredDate()).isEqualTo(desiredDate);
        assertThat(result.status()).isEqualTo(WaitlistStatus.WAITING);
        assertThat(result.note()).isEqualTo("Bệnh nhân cần khám dạ dày");
        assertThat(result.patientName()).isEqualTo("Nguyễn Văn A");

        verify(waitlistRepository).save(any());
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("Từ chối thêm vào danh sách chờ khi bác sĩ vẫn còn khung giờ trống")
    void shouldRejectWhenDoctorStillHasAvailableSlots() {
        List<DoctorAvailableSlotResult> slots = List.of(
                new DoctorAvailableSlotResult(Instant.parse("2026-10-20T01:00:00Z"), Instant.parse("2026-10-20T01:30:00Z"), false),
                new DoctorAvailableSlotResult(Instant.parse("2026-10-20T01:30:00Z"), Instant.parse("2026-10-20T02:00:00Z"), true)
        );
        when(availableSlotsUseCase.getAvailableSlots(any())).thenReturn(slots);

        AddToWaitlistCommand command = AddToWaitlistCommand.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .desiredDate(desiredDate)
                .timePreference(TimePreference.ANYTIME)
                .build();

        assertThatThrownBy(() -> service.addToWaitlist(command))
                .isInstanceOf(DoctorHasAvailableSlotsException.class)
                .hasMessageContaining("vẫn còn khung giờ trống");
    }

    @Test
    @DisplayName("Từ chối khi bệnh nhân đã có tên trong danh sách chờ của bác sĩ vào ngày này")
    void shouldRejectWhenPatientAlreadyInWaitlist() {
        when(availableSlotsUseCase.getAvailableSlots(any())).thenReturn(List.of());
        when(waitlistRepository.existsByPatientIdAndDoctorIdAndDesiredDateAndStatus(
                patientId, doctorId, desiredDate, WaitlistStatus.WAITING)).thenReturn(true);

        AddToWaitlistCommand command = AddToWaitlistCommand.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .desiredDate(desiredDate)
                .timePreference(TimePreference.ANYTIME)
                .build();

        assertThatThrownBy(() -> service.addToWaitlist(command))
                .isInstanceOf(PatientAlreadyInWaitlistException.class)
                .hasMessageContaining("đã có tên trong danh sách chờ");
    }

    @Test
    @DisplayName("Từ chối khi bác sĩ không có ca làm việc vào ngày mong muốn")
    void shouldRejectWhenDoctorNotWorkingOnDesiredDate() {
        when(scheduleValidator.resolveWorkingHours(doctorId, desiredDate)).thenReturn(Optional.empty());

        AddToWaitlistCommand command = AddToWaitlistCommand.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .desiredDate(desiredDate)
                .build();

        assertThatThrownBy(() -> service.addToWaitlist(command))
                .isInstanceOf(DoctorNotWorkingException.class)
                .hasMessageContaining("không có lịch làm việc");
    }

    @Test
    @DisplayName("Từ chối khi ngày mong muốn ở trong quá khứ")
    void shouldRejectWhenDesiredDateIsInPast() {
        AddToWaitlistCommand command = AddToWaitlistCommand.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .desiredDate(LocalDate.of(2026, 9, 30))
                .build();

        assertThatThrownBy(() -> service.addToWaitlist(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("quá khứ");
    }

    @Test
    @DisplayName("Từ chối khi người dùng không có vai trò RECEPTIONIST hoặc ADMIN")
    void shouldRejectWhenUserUnauthorized() {
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(false);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        AddToWaitlistCommand command = AddToWaitlistCommand.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .desiredDate(desiredDate)
                .build();

        assertThatThrownBy(() -> service.addToWaitlist(command))
                .isInstanceOf(UnauthorizedAppointmentOperationException.class);
    }

    @Test
    @DisplayName("Ném PatientAlreadyInWaitlistException khi repository gặp xung đột đồng thời (concurrency race)")
    void shouldPropagatePatientAlreadyInWaitlistExceptionWhenRepositoryFailsDueToConcurrency() {
        when(availableSlotsUseCase.getAvailableSlots(any())).thenReturn(List.of());
        when(waitlistRepository.existsByPatientIdAndDoctorIdAndDesiredDateAndStatus(
                patientId, doctorId, desiredDate, WaitlistStatus.WAITING)).thenReturn(false);

        when(waitlistRepository.save(any()))
                .thenThrow(new PatientAlreadyInWaitlistException("Bệnh nhân đã có tên trong danh sách chờ của bác sĩ vào ngày này."));

        AddToWaitlistCommand command = AddToWaitlistCommand.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .desiredDate(desiredDate)
                .timePreference(TimePreference.ANYTIME)
                .build();

        assertThatThrownBy(() -> service.addToWaitlist(command))
                .isInstanceOf(PatientAlreadyInWaitlistException.class)
                .hasMessageContaining("đã có tên trong danh sách chờ");
    }
}

