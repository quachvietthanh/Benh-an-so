package com.benhsoan.application.ucservice.appointment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.AppointmentSeries;
import com.benhsoan.domain.appointment.AppointmentSeriesConflictDetail;
import com.benhsoan.domain.appointment.exception.AppointmentSeriesConflictException;
import com.benhsoan.domain.appointment.exception.DoctorInactiveException;
import com.benhsoan.domain.appointment.exception.DoctorNotFoundException;
import com.benhsoan.domain.appointment.exception.UnauthorizedAppointmentOperationException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.appointment.CreateAppointmentSeriesCommand;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.dto.result.appointment.AppointmentSeriesResult;
import com.benhsoan.port.inbound.appointment.CreateAppointmentSeriesUseCase;
import com.benhsoan.port.outbound.generator.AppointmentCodeGenerator;
import com.benhsoan.port.outbound.generator.AppointmentSeriesCodeGenerator;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.AppointmentSeriesRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateAppointmentSeriesService implements CreateAppointmentSeriesUseCase {

    private final AppointmentSeriesRepository appointmentSeriesRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final VisitRepository visitRepository;
    private final AppointmentCodeGenerator appointmentCodeGenerator;
    private final AppointmentSeriesCodeGenerator appointmentSeriesCodeGenerator;
    private final AppointmentSeriesValidator appointmentSeriesValidator;
    private final AppointmentSeriesResultMapper appointmentSeriesResultMapper;
    private final AppointmentResultMapper appointmentResultMapper;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final AuditLogRepository auditLogRepository;
    private final AppointmentAccessDeniedAuditWriter appointmentAccessDeniedAuditWriter;

    @Override
    public AppointmentSeriesResult create(CreateAppointmentSeriesCommand command) {
        UUID currentUserId = currentUserPort.getCurrentUserId();

        if (!currentUserPort.hasRole("ADMIN") && !currentUserPort.hasRole("RECEPTIONIST")) {
            appointmentAccessDeniedAuditWriter.writeSeriesCreateDenied(
                    currentUserId,
                    clockPort.now(),
                    "User lacks RECEPTIONIST or ADMIN role to create appointment series"
            );
            throw new UnauthorizedAppointmentOperationException();
        }

        if (command.totalSessions() < 2) {
            throw new ValidationException("Số buổi của liệu trình phải từ 2 trở lên.");
        }
        if (command.intervalDays() < 1) {
            throw new ValidationException("Khoảng cách giữa các buổi phải từ 1 ngày trở lên.");
        }

        patientRepository.findById(command.patientId())
                .orElseThrow(() -> new PatientNotFoundException(command.patientId()));

        User doctor = userRepository.findByIdForUpdate(command.doctorId())
                .orElseThrow(() -> new DoctorNotFoundException(command.doctorId()));

        if (!doctor.isActive()) {
            throw new DoctorInactiveException(doctor.getId());
        }

        if (command.medicalRecordId() != null) {
            var record = medicalRecordRepository.findById(command.medicalRecordId())
                    .orElseThrow(() -> new ValidationException("Không tìm thấy bệnh án với ID: " + command.medicalRecordId()));
            var visit = visitRepository.findById(record.getVisitId())
                    .orElseThrow(() -> new ValidationException("Không tìm thấy lượt khám liên quan đến bệnh án."));
            if (!visit.getPatientId().equals(command.patientId())) {
                throw new ValidationException("Bệnh án không thuộc về bệnh nhân này.");
            }
        }

        List<AppointmentSeriesValidator.SessionSlot> sessionSlots = new ArrayList<>();
        if (command.sessions() != null && !command.sessions().isEmpty()) {
            if (command.sessions().size() != command.totalSessions()) {
                throw new ValidationException("Số lượng buổi trong danh sách (" + command.sessions().size()
                        + ") không khớp với tổng số buổi (" + command.totalSessions() + ").");
            }
            for (var s : command.sessions()) {
                sessionSlots.add(new AppointmentSeriesValidator.SessionSlot(
                        s.sequenceNumber(),
                        s.startTime(),
                        s.endTime()
                ));
            }
        } else {
            throw new ValidationException("Danh sách các buổi của liệu trình không được để trống.");
        }

        appointmentSeriesValidator.validateSessionStructure(sessionSlots, command.totalSessions(), command.intervalDays());
        sessionSlots.sort(java.util.Comparator.comparingInt(AppointmentSeriesValidator.SessionSlot::sequenceNumber));

        List<AppointmentSeriesConflictDetail> conflicts = appointmentSeriesValidator.validateSessions(command.doctorId(), sessionSlots);
        if (!conflicts.isEmpty()) {
            throw new AppointmentSeriesConflictException(conflicts);
        }

        String seriesCode = appointmentSeriesCodeGenerator.generate();
        Instant now = clockPort.now();

        AppointmentSeries series = AppointmentSeries.create(
                seriesCode,
                command.patientId(),
                command.doctorId(),
                command.medicalRecordId(),
                command.totalSessions(),
                command.intervalDays(),
                command.title(),
                command.notes(),
                currentUserId,
                now
        );
        AppointmentSeries savedSeries = appointmentSeriesRepository.save(series);

        List<String> appointmentCodes = appointmentCodeGenerator.generateBatch(command.totalSessions());
        List<AppointmentResult> appointmentResults = new ArrayList<>();

        for (int i = 0; i < sessionSlots.size(); i++) {
            var slot = sessionSlots.get(i);
            String aptCode = appointmentCodes.get(i);
            Appointment appointment = Appointment.createSeriesAppointment(
                    aptCode,
                    command.patientId(),
                    command.doctorId(),
                    slot.startTime(),
                    slot.endTime(),
                    command.title(),
                    currentUserId,
                    "RECEPTION_COUNTER",
                    savedSeries.getId(),
                    slot.sequenceNumber(),
                    now
            );
            Appointment savedApt = appointmentRepository.save(appointment);
            appointmentResults.add(appointmentResultMapper.toResult(savedApt));
        }

        auditLogRepository.save(AuditLog.create(
                currentUserId,
                ActionType.CREATE,
                ResourceType.APPOINTMENT,
                savedSeries.getId(),
                """
                {
                "seriesCode":"%s",
                "patientId":"%s",
                "doctorId":"%s",
                "totalSessions":%d,
                "intervalDays":%d
                }
                """.formatted(
                        savedSeries.getSeriesCode(),
                        savedSeries.getPatientId(),
                        savedSeries.getDoctorId(),
                        savedSeries.getTotalSessions(),
                        savedSeries.getIntervalDays()
                ),
                null
        ));

        return appointmentSeriesResultMapper.toResult(savedSeries, appointmentResults);
    }
}
