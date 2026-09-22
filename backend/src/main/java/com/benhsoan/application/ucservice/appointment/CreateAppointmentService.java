package com.benhsoan.application.ucservice.appointment;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.exception.AppointmentTimeConflictException;
import com.benhsoan.domain.appointment.exception.DoctorInactiveException;
import com.benhsoan.domain.appointment.exception.DoctorNotFoundException;
import com.benhsoan.domain.appointment.exception.UnauthorizedAppointmentOperationException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.appointment.CreateAppointmentCommand;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.inbound.appointment.CreateAppointmentUseCase;
import com.benhsoan.port.outbound.generator.AppointmentCodeGenerator;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.AppointmentWaitlistRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@Service
@Transactional
public class CreateAppointmentService implements CreateAppointmentUseCase {

        public static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

        private final AppointmentRepository appointmentRepository;
        private final PatientRepository patientRepository;
        private final UserRepository userRepository;
        private final AppointmentCodeGenerator appointmentCodeGenerator;
        private final CurrentUserPort currentUserPort;
        private final AppointmentResultMapper appointmentResultMapper;
        private final AuditLogRepository auditLogRepository;
        private final DoctorScheduleValidator doctorScheduleValidator;
        private final ClockPort clockPort;
        private final AppointmentWaitlistRepository appointmentWaitlistRepository;

        public CreateAppointmentService(
                AppointmentRepository appointmentRepository,
                PatientRepository patientRepository,
                UserRepository userRepository,
                AppointmentCodeGenerator appointmentCodeGenerator,
                CurrentUserPort currentUserPort,
                AppointmentResultMapper appointmentResultMapper,
                AuditLogRepository auditLogRepository,
                DoctorScheduleValidator doctorScheduleValidator,
                ClockPort clockPort
        ) {
                this(appointmentRepository, patientRepository, userRepository, appointmentCodeGenerator,
                        currentUserPort, appointmentResultMapper, auditLogRepository, doctorScheduleValidator,
                        clockPort, null);
        }

        @Autowired
        public CreateAppointmentService(
                AppointmentRepository appointmentRepository,
                PatientRepository patientRepository,
                UserRepository userRepository,
                AppointmentCodeGenerator appointmentCodeGenerator,
                CurrentUserPort currentUserPort,
                AppointmentResultMapper appointmentResultMapper,
                AuditLogRepository auditLogRepository,
                DoctorScheduleValidator doctorScheduleValidator,
                ClockPort clockPort,
                @Autowired(required = false) AppointmentWaitlistRepository appointmentWaitlistRepository
        ) {
                this.appointmentRepository = appointmentRepository;
                this.patientRepository = patientRepository;
                this.userRepository = userRepository;
                this.appointmentCodeGenerator = appointmentCodeGenerator;
                this.currentUserPort = currentUserPort;
                this.appointmentResultMapper = appointmentResultMapper;
                this.auditLogRepository = auditLogRepository;
                this.doctorScheduleValidator = doctorScheduleValidator;
                this.clockPort = clockPort;
                this.appointmentWaitlistRepository = appointmentWaitlistRepository;
        }

        @Override
        public AppointmentResult create(
                        CreateAppointmentCommand command) {

                validate(command);

                UUID currentUserId = currentUserPort.getCurrentUserId();

                String appointmentCode = appointmentCodeGenerator.generate();

                Appointment appointment = Appointment.create(
                                appointmentCode,
                                command.patientId(),
                                command.doctorId(),
                                command.startTime(),
                                command.endTime(),
                                command.reason(),
                                currentUserId);

                Appointment saved = appointmentRepository.save(appointment);

                auditLogRepository.save(
                                AuditLog.create(
                                                currentUserId,
                                                ActionType.CREATE,
                                                ResourceType.APPOINTMENT,
                                                saved.getId(),
                                                """
                                                                {
                                                                "appointmentCode":"%s",
                                                                "patientId":"%s",
                                                                "doctorId":"%s",
                                                                "startTime":"%s",
                                                                "endTime":"%s"
                                                                }
                                                                """.formatted(
                                                                saved.getAppointmentCode(),
                                                                saved.getPatientId(),
                                                                saved.getDoctorId(),
                                                                saved.getStartTime(),
                                                                saved.getEndTime()),
                                                null));

                // NCL-03-CN-012-TC-03: Tự động chuyển trạng thái mục chờ thành SCHEDULED
                if (appointmentWaitlistRepository != null && command.startTime() != null) {
                        LocalDate appointmentDate = command.startTime().atZone(CLINIC_ZONE).toLocalDate();
                        appointmentWaitlistRepository.findActiveByPatientAndDoctorAndDate(
                                        command.patientId(),
                                        command.doctorId(),
                                        appointmentDate
                        ).ifPresent(waitlist -> {
                                waitlist.markScheduled(saved.getId(), clockPort.now());
                                appointmentWaitlistRepository.save(waitlist);
                        });
                }

                return appointmentResultMapper.toResult(saved);
        }

        private void validate(
                        CreateAppointmentCommand command) {

                patientRepository.findById(command.patientId())
                                .orElseThrow(() -> new PatientNotFoundException(command.patientId()));

                User doctor = userRepository.findByIdForUpdate(command.doctorId())
                                .orElseThrow(() -> new DoctorNotFoundException(
                                                command.doctorId()));

                if (!doctor.isActive()) {
                        throw new DoctorInactiveException(
                                        doctor.getId());
                }

                if (!currentUserPort.hasRole("ADMIN")
                                && !currentUserPort.hasRole("RECEPTIONIST")) {
                        throw new UnauthorizedAppointmentOperationException();
                }

                if (!command.endTime().isAfter(command.startTime())) {
                        throw new ValidationException("Appointment end time must be after start time.");
                }

                if (command.startTime().isBefore(clockPort.now())) {
                        throw new ValidationException("Thời gian đặt lịch không được ở trong quá khứ.");
                }

                // QTN-30 / TC-02: Check doctor schedule and active time-off
                doctorScheduleValidator.validateDoctorWorkingAndAvailable(
                                command.doctorId(),
                                command.startTime(),
                                command.endTime());

                if (appointmentRepository.existsActiveAppointmentConflict(
                                command.doctorId(),
                                command.startTime(),
                                command.endTime())) {
                        throw new AppointmentTimeConflictException();
                }
        }

}
