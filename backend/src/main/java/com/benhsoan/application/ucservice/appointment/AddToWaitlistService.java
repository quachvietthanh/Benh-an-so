package com.benhsoan.application.ucservice.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.AppointmentWaitlist;
import com.benhsoan.domain.appointment.enums.TimePreference;
import com.benhsoan.domain.appointment.enums.WaitlistStatus;
import com.benhsoan.domain.appointment.exception.DoctorHasAvailableSlotsException;
import com.benhsoan.domain.appointment.exception.DoctorInactiveException;
import com.benhsoan.domain.appointment.exception.DoctorNotFoundException;
import com.benhsoan.domain.appointment.exception.DoctorNotWorkingException;
import com.benhsoan.domain.appointment.exception.PatientAlreadyInWaitlistException;
import com.benhsoan.domain.appointment.exception.UnauthorizedAppointmentOperationException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.appointment.AddToWaitlistCommand;
import com.benhsoan.port.dto.query.appointment.GetDoctorAvailableSlotsQuery;
import com.benhsoan.port.dto.result.appointment.AppointmentWaitlistResult;
import com.benhsoan.port.dto.result.appointment.DoctorAvailableSlotResult;
import com.benhsoan.port.inbound.appointment.AddToWaitlistUseCase;
import com.benhsoan.port.inbound.appointment.GetDoctorAvailableSlotsUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentWaitlistRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AddToWaitlistService implements AddToWaitlistUseCase {

    public static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final AppointmentWaitlistRepository appointmentWaitlistRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final GetDoctorAvailableSlotsUseCase getDoctorAvailableSlotsUseCase;
    private final DoctorScheduleValidator doctorScheduleValidator;
    private final AppointmentWaitlistResultMapper resultMapper;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public AppointmentWaitlistResult addToWaitlist(AddToWaitlistCommand command) {
        validatePermission();
        validateCommand(command);

        UUID currentUserId = currentUserPort.getCurrentUserId();
        TimePreference preference = command.timePreference() != null ? command.timePreference() : TimePreference.ANYTIME;

        // 1. Kiểm tra bác sĩ có lịch làm việc vào ngày mong muốn không
        var workingHoursOpt = doctorScheduleValidator.resolveWorkingHours(command.doctorId(), command.desiredDate());
        if (workingHoursOpt.isEmpty()) {
            throw new DoctorNotWorkingException("Bác sĩ không có lịch làm việc vào ngày " + command.desiredDate() + ".");
        }

        // 2. Precondition check: Bác sĩ đã kín lịch trong khung thời gian mong muốn
        List<DoctorAvailableSlotResult> slots = getDoctorAvailableSlotsUseCase.getAvailableSlots(
                new GetDoctorAvailableSlotsQuery(command.doctorId(), command.desiredDate())
        );

        boolean hasAvailableSlot = slots.stream()
                .filter(slot -> matchesTimePreference(slot, preference))
                .anyMatch(DoctorAvailableSlotResult::isAvailable);

        if (hasAvailableSlot) {
            throw new DoctorHasAvailableSlotsException(
                    "Bác sĩ vẫn còn khung giờ trống trong khoảng thời gian mong muốn, vui lòng đặt lịch trực tiếp thay vì ghi vào danh sách chờ."
            );
        }

        // 3. Kiểm tra chống trùng lặp danh sách chờ
        boolean alreadyInWaitlist = appointmentWaitlistRepository.existsByPatientIdAndDoctorIdAndDesiredDateAndStatus(
                command.patientId(),
                command.doctorId(),
                command.desiredDate(),
                WaitlistStatus.WAITING
        );
        if (alreadyInWaitlist) {
            throw new PatientAlreadyInWaitlistException(
                    "Bệnh nhân đã có tên trong danh sách chờ của bác sĩ vào ngày này."
            );
        }

        // 4. Tạo bản ghi danh sách chờ
        AppointmentWaitlist waitlist = AppointmentWaitlist.create(
                command.patientId(),
                command.doctorId(),
                command.desiredDate(),
                preference,
                command.note(),
                currentUserId,
                clockPort.now()
        );

        AppointmentWaitlist saved = appointmentWaitlistRepository.save(waitlist);

        // 5. Ghi AuditLog
        auditLogRepository.save(
                AuditLog.create(
                        currentUserId,
                        ActionType.CREATE,
                        ResourceType.APPOINTMENT_WAITLIST,
                        saved.getId(),
                        """
                        {
                          "patientId":"%s",
                          "doctorId":"%s",
                          "desiredDate":"%s",
                          "timePreference":"%s"
                        }
                        """.formatted(
                                saved.getPatientId(),
                                saved.getDoctorId(),
                                saved.getDesiredDate(),
                                saved.getTimePreference()
                        ),
                        null
                )
        );

        return resultMapper.toResult(saved);
    }

    private void validatePermission() {
        if (!currentUserPort.hasRole("ADMIN") && !currentUserPort.hasRole("RECEPTIONIST")) {
            throw new UnauthorizedAppointmentOperationException();
        }
    }

    private void validateCommand(AddToWaitlistCommand command) {
        Objects.requireNonNull(command.patientId(), "ID bệnh nhân không được để trống");
        Objects.requireNonNull(command.doctorId(), "ID bác sĩ không được để trống");
        Objects.requireNonNull(command.desiredDate(), "Ngày mong muốn khám không được để trống");

        patientRepository.findById(command.patientId())
                .orElseThrow(() -> new PatientNotFoundException(command.patientId()));

        User doctor = userRepository.findById(command.doctorId())
                .orElseThrow(() -> new DoctorNotFoundException(command.doctorId()));

        if (!doctor.isActive()) {
            throw new DoctorInactiveException(doctor.getId());
        }

        LocalDate today = clockPort.now().atZone(CLINIC_ZONE).toLocalDate();
        if (command.desiredDate().isBefore(today)) {
            throw new ValidationException("Ngày mong muốn khám không được ở trong quá khứ.");
        }
    }

    private boolean matchesTimePreference(DoctorAvailableSlotResult slot, TimePreference preference) {
        if (preference == null || preference == TimePreference.ANYTIME) {
            return true;
        }
        LocalTime slotStartTime = slot.startTime().atZone(CLINIC_ZONE).toLocalTime();
        if (preference == TimePreference.MORNING) {
            return slotStartTime.isBefore(LocalTime.NOON);
        }
        if (preference == TimePreference.AFTERNOON) {
            return !slotStartTime.isBefore(LocalTime.NOON);
        }
        return true;
    }
}
