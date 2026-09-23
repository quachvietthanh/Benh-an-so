package com.benhsoan.application.ucservice.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.enums.TimePreference;
import com.benhsoan.domain.appointment.enums.WaitlistStatus;
import com.benhsoan.domain.appointment.exception.PatientAlreadyInWaitlistException;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.persistence.adapterRepository.appointment.AppointmentRepositoryAdapter;
import com.benhsoan.persistence.adapterRepository.appointment.AppointmentWaitlistRepositoryAdapter;
import com.benhsoan.persistence.adapterRepository.auditlog.AuditLogRepositoryAdapter;
import com.benhsoan.persistence.adapterRepository.auth.UserRepositoryAdapter;
import com.benhsoan.persistence.adapterRepository.patient.PatientRepositoryAdapter;
import com.benhsoan.persistence.entity.auth.RoleEntity;
import com.benhsoan.persistence.jpaRepository.auth.JpaRoleRepository;
import com.benhsoan.persistence.mapper.appointment.AppointmentPersistenceMapper;
import com.benhsoan.persistence.mapper.appointment.AppointmentWaitlistPersistenceMapper;
import com.benhsoan.persistence.mapper.auditlog.AuditLogPersistenceMapper;
import com.benhsoan.persistence.mapper.auth.UserPersistenceMapper;
import com.benhsoan.persistence.mapper.patient.PatientPersistenceMapper;
import com.benhsoan.port.dto.command.appointment.AddToWaitlistCommand;
import com.benhsoan.port.dto.command.appointment.CancelAppointmentCommand;
import com.benhsoan.port.dto.command.appointment.CreateAppointmentCommand;
import com.benhsoan.port.dto.query.appointment.GetAppointmentWaitlistQuery;
import com.benhsoan.port.dto.query.appointment.GetWaitlistSuggestionQuery;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.dto.result.appointment.AppointmentWaitlistResult;
import com.benhsoan.port.dto.result.appointment.DoctorAvailableSlotResult;
import com.benhsoan.port.inbound.appointment.GetDoctorAvailableSlotsUseCase;
import com.benhsoan.port.outbound.generator.AppointmentCodeGenerator;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.AppointmentWaitlistRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({
        AppointmentWaitlistRepositoryAdapter.class,
        AppointmentWaitlistPersistenceMapper.class,
        AppointmentRepositoryAdapter.class,
        AppointmentPersistenceMapper.class,
        PatientRepositoryAdapter.class,
        PatientPersistenceMapper.class,
        UserRepositoryAdapter.class,
        UserPersistenceMapper.class,
        AuditLogRepositoryAdapter.class,
        AuditLogPersistenceMapper.class,
        AppointmentWaitlistResultMapper.class,
        AppointmentResultMapper.class,
        AddToWaitlistService.class,
        GetAppointmentWaitlistService.class,
        GetWaitlistSuggestionService.class,
        CancelWaitlistEntryService.class,
        CancelAppointmentService.class,
        CreateAppointmentService.class
})
class AppointmentWaitlistAcceptanceIntegrationTest {

    @Autowired private AppointmentWaitlistRepository waitlistRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JpaRoleRepository jpaRoleRepository;

    @Autowired private AddToWaitlistService addToWaitlistService;
    @Autowired private GetAppointmentWaitlistService getAppointmentWaitlistService;
    @Autowired private GetWaitlistSuggestionService getWaitlistSuggestionService;
    @Autowired private CancelAppointmentService cancelAppointmentService;
    @Autowired private CreateAppointmentService createAppointmentService;

    @MockitoBean private GetDoctorAvailableSlotsUseCase getDoctorAvailableSlotsUseCase;
    @MockitoBean private DoctorScheduleValidator doctorScheduleValidator;
    @MockitoBean private AppointmentCodeGenerator appointmentCodeGenerator;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private QueueItemRepository queueItemRepository;
    @MockitoBean private VisitRepository visitRepository;

    private UUID receptionistId;
    private UUID doctorId;
    private UUID patient1Id;
    private UUID patient2Id;
    private final LocalDate desiredDate = LocalDate.of(2026, 11, 20);
    private final Instant now = Instant.parse("2026-11-01T08:00:00Z");

    @BeforeEach
    void setUp() {
        when(clockPort.now()).thenReturn(now);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        // Lưu Role vào database
        RoleEntity roleEntity = RoleEntity.builder()
                .id(UUID.randomUUID())
                .name("RECEPTIONIST")
                .description("Receptionist")
                .isSystem(false)
                .createdAt(now)
                .build();
        jpaRoleRepository.save(roleEntity);

        // Tạo người dùng Receptionist & Doctor
        User receptionist = User.create(
                "receptionist1",
                "hashed",
                "Lễ tân Quầy 1",
                "rec@test.com",
                "0901000001",
                roleEntity.getId()
        );
        receptionist = userRepository.save(receptionist);
        receptionistId = receptionist.getId();
        when(currentUserPort.getCurrentUserId()).thenReturn(receptionistId);

        User doctor = User.create(
                "doctor1",
                "hashed",
                "BS. Nguyễn Văn A",
                "doc@test.com",
                "0901000002",
                roleEntity.getId()
        );
        doctor = userRepository.save(doctor);
        doctorId = doctor.getId();

        // Tạo 2 bệnh nhân
        Patient patient1 = Patient.create(
                "BN000001",
                "Trần Văn Một",
                LocalDate.of(1990, 5, 10),
                Gender.MALE,
                "0911111111",
                "p1@example.com",
                "Address 1",
                "079090000001",
                "DN4790000000001",
                BloodType.O_POSITIVE,
                "Emergency Contact",
                "0909998877",
                true,
                "v1.0",
                receptionistId
        );
        patient1 = patientRepository.save(patient1);
        patient1Id = patient1.getId();

        Patient patient2 = Patient.create(
                "BN000002",
                "Lê Thị Hai",
                LocalDate.of(1992, 8, 15),
                Gender.FEMALE,
                "0922222222",
                "p2@example.com",
                "Address 2",
                "079090000002",
                "DN4790000000002",
                BloodType.A_POSITIVE,
                "Emergency Contact",
                "0909998878",
                true,
                "v1.0",
                receptionistId
        );
        patient2 = patientRepository.save(patient2);
        patient2Id = patient2.getId();

        // Giả lập lịch làm việc của bác sĩ
        when(doctorScheduleValidator.resolveWorkingHours(doctorId, desiredDate))
                .thenReturn(Optional.of(new DoctorScheduleValidator.EffectiveWorkingHours(LocalTime.of(8, 0), LocalTime.of(17, 0))));

        when(appointmentCodeGenerator.generate()).thenReturn("APT-20261120-001", "APT-20261120-002");
    }

    @Test
    @DisplayName("Chạy quy trình toàn diện Acceptance Test: TC-01 -> TC-02 -> TC-03")
    void testEndToEndWaitlistLifecycle() {
        // =========================================================================
        // BƯỚC 1: TC-01 - Bác sĩ kín lịch -> Lễ tân thêm 2 bệnh nhân vào danh sách chờ theo thứ tự FIFO
        // =========================================================================
        when(getDoctorAvailableSlotsUseCase.getAvailableSlots(any())).thenReturn(List.of(
                new DoctorAvailableSlotResult(Instant.parse("2026-11-20T01:00:00Z"), Instant.parse("2026-11-20T01:30:00Z"), false),
                new DoctorAvailableSlotResult(Instant.parse("2026-11-20T01:30:00Z"), Instant.parse("2026-11-20T02:00:00Z"), false)
        ));

        // Thêm Bệnh nhân 1 lúc 08:00
        when(clockPort.now()).thenReturn(Instant.parse("2026-11-01T08:00:00Z"));
        AppointmentWaitlistResult waitlist1 = addToWaitlistService.addToWaitlist(AddToWaitlistCommand.builder()
                .patientId(patient1Id)
                .doctorId(doctorId)
                .desiredDate(desiredDate)
                .timePreference(TimePreference.ANYTIME)
                .note("Cần khám tiêu hóa")
                .build());

        assertThat(waitlist1).isNotNull();
        assertThat(waitlist1.patientName()).isEqualTo("Trần Văn Một");
        assertThat(waitlist1.status()).isEqualTo(WaitlistStatus.WAITING);

        // Kiểm tra chặn đăng ký trùng cho Bệnh nhân 1
        assertThatThrownBy(() -> addToWaitlistService.addToWaitlist(AddToWaitlistCommand.builder()
                .patientId(patient1Id)
                .doctorId(doctorId)
                .desiredDate(desiredDate)
                .build()))
                .isInstanceOf(PatientAlreadyInWaitlistException.class);

        // Thêm Bệnh nhân 2 lúc 08:05 (đăng ký sau Bệnh nhân 1)
        when(clockPort.now()).thenReturn(Instant.parse("2026-11-01T08:05:00Z"));
        AppointmentWaitlistResult waitlist2 = addToWaitlistService.addToWaitlist(AddToWaitlistCommand.builder()
                .patientId(patient2Id)
                .doctorId(doctorId)
                .desiredDate(desiredDate)
                .timePreference(TimePreference.ANYTIME)
                .note("Cần khám tổng quát")
                .build());

        assertThat(waitlist2).isNotNull();
        assertThat(waitlist2.patientName()).isEqualTo("Lê Thị Hai");
        assertThat(waitlist2.status()).isEqualTo(WaitlistStatus.WAITING);

        // Kiểm tra danh sách chờ được sắp xếp đúng thứ tự FIFO (Bệnh nhân 1 trước, Bệnh nhân 2 sau)
        List<AppointmentWaitlistResult> activeWaitlist = getAppointmentWaitlistService.getWaitlist(
                new GetAppointmentWaitlistQuery(doctorId, desiredDate, WaitlistStatus.WAITING));
        assertThat(activeWaitlist).hasSize(2);
        assertThat(activeWaitlist.get(0).patientId()).isEqualTo(patient1Id);
        assertThat(activeWaitlist.get(1).patientId()).isEqualTo(patient2Id);

        // =========================================================================
        // BƯỚC 2: TC-02 - Một lịch hẹn cùng ngày bị hủy -> Hệ thống gợi ý Bệnh nhân 1 (người chờ đầu tiên)
        // =========================================================================
        Instant appointmentStart = Instant.parse("2026-11-20T01:00:00Z");
        Instant appointmentEnd = Instant.parse("2026-11-20T01:30:00Z");
        Appointment existingAppointment = Appointment.create(
                "APT-EXISTING",
                patient1Id,
                doctorId,
                appointmentStart,
                appointmentEnd,
                "Khám định kỳ",
                receptionistId
        );
        existingAppointment = appointmentRepository.save(existingAppointment);

        // Lễ tân hủy lịch hẹn này
        when(clockPort.now()).thenReturn(Instant.parse("2026-11-19T10:00:00Z"));
        AppointmentResult cancelResult = cancelAppointmentService.cancel(
                existingAppointment.getId(),
                new CancelAppointmentCommand("Bệnh nhân có việc bận đột xuất")
        );

        assertThat(cancelResult.status()).isEqualTo(AppointmentStatus.CANCELLED);
        // Kiểm tra gợi ý người chờ đầu tiên được đính kèm ngay trong cancelResult
        assertThat(cancelResult.suggestedWaitlistEntry()).isNotNull();
        assertThat(cancelResult.suggestedWaitlistEntry().patientId()).isEqualTo(patient1Id);
        assertThat(cancelResult.suggestedWaitlistEntry().patientName()).isEqualTo("Trần Văn Một");
        assertThat(cancelResult.suggestedWaitlistEntry().note()).isEqualTo("Cần khám tiêu hóa");

        // Đồng thời kiểm tra qua query gợi ý độc lập
        var suggestionOpt = getWaitlistSuggestionService.getSuggestion(
                new GetWaitlistSuggestionQuery(doctorId, desiredDate));
        assertThat(suggestionOpt).isPresent();
        assertThat(suggestionOpt.get().patientId()).isEqualTo(patient1Id);

        // =========================================================================
        // BƯỚC 3: TC-03 - Lễ tân đặt lịch cho Bệnh nhân 1 -> Bệnh nhân 1 không còn trong danh sách chờ
        // =========================================================================
        when(clockPort.now()).thenReturn(Instant.parse("2026-11-19T10:15:00Z"));
        CreateAppointmentCommand bookCommand = CreateAppointmentCommand.builder()
                .patientId(patient1Id)
                .doctorId(doctorId)
                .startTime(appointmentStart)
                .endTime(appointmentEnd)
                .reason("Đặt lịch từ danh sách chờ")
                .build();

        AppointmentResult bookedResult = createAppointmentService.create(bookCommand);
        assertThat(bookedResult).isNotNull();
        assertThat(bookedResult.patientId()).isEqualTo(patient1Id);

        // Xem lại danh sách chờ: Bệnh nhân 1 đã chuyển sang SCHEDULED, không còn trong danh sách WAITING!
        List<AppointmentWaitlistResult> waitlistAfterBooking = getAppointmentWaitlistService.getWaitlist(
                new GetAppointmentWaitlistQuery(doctorId, desiredDate, WaitlistStatus.WAITING));
        assertThat(waitlistAfterBooking).hasSize(1);
        assertThat(waitlistAfterBooking.get(0).patientId()).isEqualTo(patient2Id);
        assertThat(waitlistAfterBooking.get(0).patientName()).isEqualTo("Lê Thị Hai");

        // Bản ghi của Bệnh nhân 1 đã chuyển thành SCHEDULED và liên kết với bookedAppointmentId
        var patient1WaitlistOpt = waitlistRepository.findById(waitlist1.id());
        assertThat(patient1WaitlistOpt).isPresent();
        assertThat(patient1WaitlistOpt.get().getStatus()).isEqualTo(WaitlistStatus.SCHEDULED);
        assertThat(patient1WaitlistOpt.get().getBookedAppointmentId()).isEqualTo(bookedResult.id());

        // Bây giờ người chờ đầu tiên tiếp theo trong hàng là Bệnh nhân 2!
        var nextSuggestionOpt = getWaitlistSuggestionService.getSuggestion(
                new GetWaitlistSuggestionQuery(doctorId, desiredDate));
        assertThat(nextSuggestionOpt).isPresent();
        assertThat(nextSuggestionOpt.get().patientId()).isEqualTo(patient2Id);
        assertThat(nextSuggestionOpt.get().patientName()).isEqualTo("Lê Thị Hai");
    }
}
