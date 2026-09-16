package com.benhsoan.application.ucservice.appointment;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.DoctorSchedule;
import com.benhsoan.domain.appointment.DoctorTimeOff;
import com.benhsoan.domain.appointment.DoctorWeeklySchedule;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.enums.SlotAvailabilityStatus;
import com.benhsoan.domain.appointment.exception.DoctorNotFoundException;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.constant.RoleConstants;
import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.dto.query.appointment.GetDoctorWeeklyScheduleTableQuery;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyTableResult;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyTableResult.AppointmentSummaryResult;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyTableResult.DoctorDayScheduleResult;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyTableResult.DoctorScheduleDayResult;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyTableResult.DoctorScheduleSlotResult;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyTableResult.DoctorSummaryResult;
import com.benhsoan.port.dto.result.appointment.DoctorWeeklyTableResult.DoctorTimeOffSummaryResult;
import com.benhsoan.port.inbound.appointment.GetDoctorWeeklyScheduleTableUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorScheduleRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorWeeklyScheduleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

/**
 * NCL-03-CN-010: Computes the tabular weekly schedule matrix for doctors.
 * Integrates doctor working hours (weekly recurring or date-specific
 * overrides),
 * unexpected time-offs (leaves - QTN-30), and scheduled appointments (QTN-04)
 * to project standard 30-minute slot availability for the reception dashboard.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetDoctorWeeklyScheduleTableService implements GetDoctorWeeklyScheduleTableUseCase {

        public static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
        private static final Duration SLOT_DURATION = Duration.ofMinutes(30);

        private static final LocalTime DEFAULT_CLINIC_START = LocalTime.of(7, 30);
        private static final LocalTime DEFAULT_CLINIC_END = LocalTime.of(17, 30);

        private static final Set<AppointmentStatus> DISPLAY_STATUSES = Set.of(
                        AppointmentStatus.SCHEDULED,
                        AppointmentStatus.CONFIRMED,
                        AppointmentStatus.CHECKED_IN,
                        AppointmentStatus.IN_PROGRESS,
                        AppointmentStatus.COMPLETED,
                        AppointmentStatus.NO_SHOW);

        private final UserRepository userRepository;
        private final DoctorWeeklyScheduleRepository weeklyScheduleRepository;
        private final DoctorScheduleRepository doctorScheduleRepository;
        private final DoctorTimeOffRepository doctorTimeOffRepository;
        private final AppointmentRepository appointmentRepository;
        private final PatientRepository patientRepository;
        private final ClinicConfigurationRepository clinicConfigurationRepository;
        private final ClockPort clockPort;

        @Override
        public DoctorWeeklyTableResult getWeeklyScheduleTable(GetDoctorWeeklyScheduleTableQuery query) {
                Instant now = clockPort.now();

                // 1. Resolve target week Monday to Sunday in CLINIC_ZONE
                LocalDate targetDate = (query != null && query.date() != null)
                                ? query.date()
                                : now.atZone(CLINIC_ZONE).toLocalDate();

                LocalDate weekStartDate = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate weekEndDate = weekStartDate.plusDays(6);

                Instant weekStartInstant = weekStartDate.atStartOfDay(CLINIC_ZONE).toInstant();
                Instant weekEndInstant = weekEndDate.plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant();

                // 2. Resolve Clinic Working Hours and 30-min time slots
                LocalTime clinicStart = DEFAULT_CLINIC_START;
                LocalTime clinicEnd = DEFAULT_CLINIC_END;
                Optional<ClinicConfiguration> clinicConfigOpt = clinicConfigurationRepository.find();
                if (clinicConfigOpt.isPresent()) {
                        ClinicConfiguration config = clinicConfigOpt.get();
                        if (config.getOpeningTime() != null && config.getClosingTime() != null
                                        && config.getClosingTime().isAfter(config.getOpeningTime())) {
                                clinicStart = config.getOpeningTime();
                                clinicEnd = config.getClosingTime();
                        }
                }

                List<LocalTime> timeSlots = generateTimeSlots(clinicStart, clinicEnd);

                // 3. Resolve Doctors
                List<User> doctors;
                if (query != null && query.doctorId() != null) {
                        User doctor = userRepository.findById(query.doctorId())
                                        .orElseThrow(() -> new DoctorNotFoundException(query.doctorId()));
                        if (!doctor.isActive() || !RoleConstants.DOCTOR.equals(doctor.getRoleId())) {
                                throw new DoctorNotFoundException(query.doctorId());
                        }
                        doctors = List.of(doctor);
                } else {
                        doctors = userRepository.findAllActiveByRoleId(RoleConstants.DOCTOR).stream()
                                        .sorted(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
                                        .toList();
                }

                List<DoctorSummaryResult> doctorSummaries = doctors.stream()
                                .map(d -> DoctorSummaryResult.builder()
                                                .id(d.getId())
                                                .fullName(d.getFullName())
                                                .username(d.getUsername())
                                                .specialtyName(null)
                                                .build())
                                .toList();

                if (doctors.isEmpty()) {
                        return DoctorWeeklyTableResult.builder()
                                        .weekStartDate(weekStartDate)
                                        .weekEndDate(weekEndDate)
                                        .clinicStartTime(clinicStart)
                                        .clinicEndTime(clinicEnd)
                                        .timeSlots(timeSlots)
                                        .doctors(List.of())
                                        .days(List.of())
                                        .build();
                }

                List<UUID> doctorIds = doctors.stream().map(User::getId).toList();

                // 4. Batch query schedules, time-offs, appointments, and patients
                Map<UUID, Map<DayOfWeek, DoctorWeeklySchedule>> weeklySchedulesByDoctor = weeklyScheduleRepository
                                .findByDoctorIdIn(doctorIds).stream()
                                .collect(Collectors.groupingBy(
                                                DoctorWeeklySchedule::getDoctorId,
                                                Collectors.toMap(DoctorWeeklySchedule::getDayOfWeek,
                                                                Function.identity(),
                                                                (existing, replacing) -> existing)));

                List<DoctorSchedule> dateSchedules = doctorScheduleRepository
                                .findByDoctorIdInAndScheduleDateBetween(doctorIds, weekStartDate, weekEndDate);
                Map<String, DoctorSchedule> dateSchedulesByDoctorDate = dateSchedules.stream()
                                .collect(Collectors.toMap(
                                                ds -> ds.getDoctorId() + "_" + ds.getScheduleDate(),
                                                Function.identity(),
                                                (existing, replacing) -> existing));

                List<DoctorTimeOff> activeTimeOffs = doctorTimeOffRepository
                                .findActiveOverlappingForDoctors(doctorIds, weekStartInstant, weekEndInstant);
                Map<UUID, List<DoctorTimeOff>> timeOffsByDoctor = activeTimeOffs.stream()
                                .collect(Collectors.groupingBy(DoctorTimeOff::getDoctorId));

                List<Appointment> appointments = appointmentRepository
                                .findAppointmentsForDoctorsBetween(doctorIds, weekStartInstant, weekEndInstant,
                                                DISPLAY_STATUSES);
                Map<UUID, List<Appointment>> appointmentsByDoctor = appointments.stream()
                                .collect(Collectors.groupingBy(Appointment::getDoctorId));

                Set<UUID> patientIds = appointments.stream()
                                .map(Appointment::getPatientId)
                                .collect(Collectors.toSet());
                Map<UUID, Patient> patientMap = patientIds.isEmpty()
                                ? Map.of()
                                : patientRepository.findAllById(patientIds).stream()
                                                .collect(Collectors.toMap(Patient::getId, Function.identity()));

                // 5. Project daily schedule grid for each day of week
                List<DoctorDayScheduleResult> days = new ArrayList<>(7);
                for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
                        LocalDate currentDate = weekStartDate.plusDays(dayOffset);
                        DayOfWeek dayOfWeek = currentDate.getDayOfWeek();

                        List<DoctorScheduleDayResult> doctorSchedulesForDay = new ArrayList<>(doctors.size());
                        for (User doctor : doctors) {
                                UUID docId = doctor.getId();

                                // Determine working hours for this doctor on currentDate
                                EffectiveWorkingHours workingHours = resolveDoctorWorkingHours(
                                                docId,
                                                currentDate,
                                                dayOfWeek,
                                                weeklySchedulesByDoctor.getOrDefault(docId, Map.of()),
                                                dateSchedulesByDoctorDate);

                                List<DoctorTimeOff> doctorTimeOffs = timeOffsByDoctor.getOrDefault(docId, List.of())
                                                .stream()
                                                .filter(to -> to.overlaps(
                                                                currentDate.atStartOfDay(CLINIC_ZONE).toInstant(),
                                                                currentDate.plusDays(1).atStartOfDay(CLINIC_ZONE)
                                                                                .toInstant()))
                                                .toList();

                                List<DoctorTimeOffSummaryResult> timeOffSummaries = doctorTimeOffs.stream()
                                                .map(to -> DoctorTimeOffSummaryResult.builder()
                                                                .id(to.getId())
                                                                .startTime(to.getStartTime())
                                                                .endTime(to.getEndTime())
                                                                .reason(to.getReason())
                                                                .build())
                                                .toList();

                                List<Appointment> doctorAppointments = appointmentsByDoctor
                                                .getOrDefault(docId, List.of()).stream()
                                                .filter(a -> overlaps(
                                                                a,
                                                                currentDate.atStartOfDay(CLINIC_ZONE).toInstant(),
                                                                currentDate.plusDays(1).atStartOfDay(CLINIC_ZONE)
                                                                                .toInstant()))
                                                .toList();

                                List<DoctorScheduleSlotResult> slots = buildSlotsForDay(
                                                currentDate,
                                                timeSlots,
                                                workingHours,
                                                doctorTimeOffs,
                                                doctorAppointments,
                                                patientMap,
                                                now);

                                doctorSchedulesForDay.add(DoctorScheduleDayResult.builder()
                                                .doctorId(docId)
                                                .doctorName(doctor.getFullName())
                                                .workingDay(workingHours.isWorkingDay())
                                                .workingStartTime(workingHours.startTime())
                                                .workingEndTime(workingHours.endTime())
                                                .slots(slots)
                                                .timeOffs(timeOffSummaries)
                                                .build());
                        }

                        days.add(DoctorDayScheduleResult.builder()
                                        .date(currentDate)
                                        .dayOfWeek(dayOfWeek)
                                        .doctorSchedules(doctorSchedulesForDay)
                                        .build());
                }

                return DoctorWeeklyTableResult.builder()
                                .weekStartDate(weekStartDate)
                                .weekEndDate(weekEndDate)
                                .clinicStartTime(clinicStart)
                                .clinicEndTime(clinicEnd)
                                .timeSlots(timeSlots)
                                .doctors(doctorSummaries)
                                .days(days)
                                .build();
        }

        private List<LocalTime> generateTimeSlots(LocalTime start, LocalTime end) {
                List<LocalTime> slots = new ArrayList<>();
                LocalTime current = start;
                while (current.isBefore(end)) {
                        LocalTime next = current.plus(SLOT_DURATION);
                        if (next.isAfter(end)) {
                                break;
                        }
                        slots.add(current);
                        current = next;
                }
                return slots;
        }

        private record EffectiveWorkingHours(boolean isWorkingDay, LocalTime startTime, LocalTime endTime) {
        }

        private EffectiveWorkingHours resolveDoctorWorkingHours(
                        UUID doctorId,
                        LocalDate date,
                        DayOfWeek dayOfWeek,
                        Map<DayOfWeek, DoctorWeeklySchedule> weeklyMap,
                        Map<String, DoctorSchedule> dateScheduleMap) {
                String key = doctorId + "_" + date;
                DoctorSchedule dateSchedule = dateScheduleMap.get(key);
                DoctorWeeklySchedule weeklySchedule = weeklyMap.get(dayOfWeek);

                // 1. Specific date schedule takes precedence as an override
                if (dateSchedule != null) {
                        if (!dateSchedule.isActive()) {
                                return new EffectiveWorkingHours(false, null, null);
                        }
                        return new EffectiveWorkingHours(true, dateSchedule.getStartTime(), dateSchedule.getEndTime());
                }

                // 2. Fallback to recurring weekly schedule
                if (weeklySchedule != null) {
                        if (!weeklySchedule.isActive()) {
                                return new EffectiveWorkingHours(false, null, null);
                        }
                        return new EffectiveWorkingHours(true, weeklySchedule.getStartTime(),
                                        weeklySchedule.getEndTime());
                }

                return new EffectiveWorkingHours(false, null, null);
        }

        private List<DoctorScheduleSlotResult> buildSlotsForDay(
                        LocalDate date,
                        List<LocalTime> timeSlots,
                        EffectiveWorkingHours workingHours,
                        List<DoctorTimeOff> timeOffs,
                        List<Appointment> doctorAppointments,
                        Map<UUID, Patient> patientMap,
                        Instant now) {
                List<DoctorScheduleSlotResult> slots = new ArrayList<>(timeSlots.size());

                for (LocalTime slotStartTime : timeSlots) {
                        LocalTime slotEndTime = slotStartTime.plus(SLOT_DURATION);
                        Instant slotStartInstant = date.atTime(slotStartTime).atZone(CLINIC_ZONE).toInstant();
                        Instant slotEndInstant = date.atTime(slotEndTime).atZone(CLINIC_ZONE).toInstant();

                        // 1. Check if an active appointment occupies this slot (Never hidden, even if
                        // doctor schedule changed)
                        Optional<Appointment> matchingAppt = doctorAppointments.stream()
                                        .filter(a -> overlaps(a, slotStartInstant, slotEndInstant))
                                        .findFirst();

                        if (matchingAppt.isPresent()) {
                                Appointment appt = matchingAppt.get();
                                Patient patient = patientMap.get(appt.getPatientId());
                                String patientName = patient != null ? patient.getFullName() : "Bệnh nhân";
                                String patientPhone = patient != null ? patient.getPhone() : "";
                                String patientCode = patient != null ? patient.getPatientCode() : null;

                                AppointmentSummaryResult apptSummary = AppointmentSummaryResult.builder()
                                                .id(appt.getId())
                                                .appointmentCode(appt.getAppointmentCode())
                                                .patientId(appt.getPatientId())
                                                .patientCode(patientCode)
                                                .patientName(patientName)
                                                .patientPhone(patientPhone)
                                                .status(appt.getStatus())
                                                .reason(appt.getReason())
                                                .build();

                                slots.add(DoctorScheduleSlotResult.builder()
                                                .startTime(slotStartInstant)
                                                .endTime(slotEndInstant)
                                                .slotStartTime(slotStartTime)
                                                .slotEndTime(slotEndTime)
                                                .status(SlotAvailabilityStatus.BOOKED)
                                                .isBookable(false)
                                                .appointment(apptSummary)
                                                .timeOffReason(null)
                                                .build());
                                continue;
                        }

                        // 2. Check if an active time-off occupies this slot (QTN-30)
                        Optional<DoctorTimeOff> matchingTimeOff = timeOffs.stream()
                                        .filter(to -> to.overlaps(slotStartInstant, slotEndInstant))
                                        .findFirst();

                        if (matchingTimeOff.isPresent()) {
                                slots.add(DoctorScheduleSlotResult.builder()
                                                .startTime(slotStartInstant)
                                                .endTime(slotEndInstant)
                                                .slotStartTime(slotStartTime)
                                                .slotEndTime(slotEndTime)
                                                .status(SlotAvailabilityStatus.ON_LEAVE)
                                                .isBookable(false)
                                                .appointment(null)
                                                .timeOffReason(matchingTimeOff.get().getReason())
                                                .build());
                                continue;
                        }

                        // 3. Check if off-duty (not a working day or outside working hours)
                        if (!workingHours.isWorkingDay()
                                        || slotStartTime.isBefore(workingHours.startTime())
                                        || slotEndTime.isAfter(workingHours.endTime())) {
                                slots.add(DoctorScheduleSlotResult.builder()
                                                .startTime(slotStartInstant)
                                                .endTime(slotEndInstant)
                                                .slotStartTime(slotStartTime)
                                                .slotEndTime(slotEndTime)
                                                .status(SlotAvailabilityStatus.OFF_DUTY)
                                                .isBookable(false)
                                                .appointment(null)
                                                .timeOffReason(null)
                                                .build());
                                continue;
                        }

                        // 4. Check if in the past
                        if (slotStartInstant.isBefore(now)) {
                                slots.add(DoctorScheduleSlotResult.builder()
                                                .startTime(slotStartInstant)
                                                .endTime(slotEndInstant)
                                                .slotStartTime(slotStartTime)
                                                .slotEndTime(slotEndTime)
                                                .status(SlotAvailabilityStatus.PAST)
                                                .isBookable(false)
                                                .appointment(null)
                                                .timeOffReason(null)
                                                .build());
                                continue;
                        }

                        // 5. Available for booking!
                        slots.add(DoctorScheduleSlotResult.builder()
                                        .startTime(slotStartInstant)
                                        .endTime(slotEndInstant)
                                        .slotStartTime(slotStartTime)
                                        .slotEndTime(slotEndTime)
                                        .status(SlotAvailabilityStatus.AVAILABLE)
                                        .isBookable(true)
                                        .appointment(null)
                                        .timeOffReason(null)
                                        .build());
                }

                return slots;
        }

        private boolean overlaps(Appointment appointment, Instant start, Instant end) {
                return appointment.getStartTime().isBefore(end) && appointment.getEndTime().isAfter(start);
        }
}
