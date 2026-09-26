package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.DoctorTimeOff;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.enums.TimeOffStatus;
import com.benhsoan.domain.appointment.exception.DoctorNotFoundException;
import com.benhsoan.domain.auth.User;
import com.benhsoan.port.dto.result.appointment.DoctorTimeOffResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

@ExtendWith(MockitoExtension.class)
class GetDoctorTimeOffsServiceTest {

    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID TIME_OFF_ID = UUID.randomUUID();
    private static final UUID CREATED_BY = UUID.randomUUID();
    private static final Instant START_TIME = Instant.parse("2026-09-15T08:00:00Z");
    private static final Instant END_TIME = Instant.parse("2026-09-15T12:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-09-10T02:00:00Z");

    @Mock
    private DoctorTimeOffRepository doctorTimeOffRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AppointmentRepository appointmentRepository;

    private GetDoctorTimeOffsService service;

    @BeforeEach
    void setUp() {
        service = new GetDoctorTimeOffsService(
                doctorTimeOffRepository,
                userRepository,
                appointmentRepository);
    }

    private User createDoctorUser() {
        return User.restore(DOCTOR_ID, "dr_test", "hash", "Dr. Test", "dr@test.com", "0901234567",
                UUID.randomUUID(), true, null, CREATED_AT);
    }

    @Test
    void rejectsWhenDoctorNotFound() {
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.empty());

        assertThrows(DoctorNotFoundException.class, () -> service.getTimeOffs(DOCTOR_ID));
        verify(doctorTimeOffRepository, never()).findByDoctorId(any());
    }

    @Test
    void returnsActiveTimeOffsWithPopulatedAffectedAppointments() {
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(createDoctorUser()));

        DoctorTimeOff activeTimeOff = DoctorTimeOff.restore(
                TIME_OFF_ID, DOCTOR_ID, START_TIME, END_TIME, "Nghi phep",
                TimeOffStatus.ACTIVE, CREATED_BY, CREATED_AT, null);
        when(doctorTimeOffRepository.findByDoctorId(DOCTOR_ID)).thenReturn(List.of(activeTimeOff));

        Appointment appt = mock(Appointment.class);
        UUID apptId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        when(appt.getId()).thenReturn(apptId);
        when(appt.getAppointmentCode()).thenReturn("APT000001");
        when(appt.getPatientId()).thenReturn(patientId);
        when(appt.getStartTime()).thenReturn(START_TIME);
        when(appt.getEndTime()).thenReturn(START_TIME.plusSeconds(1800));
        when(appt.getStatus()).thenReturn(AppointmentStatus.SCHEDULED);
        when(appt.getReason()).thenReturn("Kham tong quat");

        when(appointmentRepository.findActiveAppointmentsForDoctorBetween(DOCTOR_ID, START_TIME, END_TIME))
                .thenReturn(List.of(appt));

        List<DoctorTimeOffResult> results = service.getTimeOffs(DOCTOR_ID);

        assertEquals(1, results.size());
        DoctorTimeOffResult result = results.get(0);
        assertEquals(TIME_OFF_ID, result.id());
        assertEquals(TimeOffStatus.ACTIVE, result.status());
        assertNotNull(result.affectedAppointments());
        assertEquals(1, result.affectedAppointments().size());
        assertEquals("APT000001", result.affectedAppointments().get(0).appointmentCode());
        assertEquals(patientId, result.affectedAppointments().get(0).patientId());
    }

    @Test
    void returnsActiveTimeOffsWithEmptyAffectedAppointmentsWhenNoneExist() {
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(createDoctorUser()));

        DoctorTimeOff activeTimeOff = DoctorTimeOff.restore(
                TIME_OFF_ID, DOCTOR_ID, START_TIME, END_TIME, "Nghi phep",
                TimeOffStatus.ACTIVE, CREATED_BY, CREATED_AT, null);
        when(doctorTimeOffRepository.findByDoctorId(DOCTOR_ID)).thenReturn(List.of(activeTimeOff));
        when(appointmentRepository.findActiveAppointmentsForDoctorBetween(DOCTOR_ID, START_TIME, END_TIME))
                .thenReturn(List.of());

        List<DoctorTimeOffResult> results = service.getTimeOffs(DOCTOR_ID);

        assertEquals(1, results.size());
        assertTrue(results.get(0).affectedAppointments().isEmpty());
    }

    @Test
    void returnsCancelledTimeOffWithoutQueryingActiveAppointments() {
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(createDoctorUser()));

        DoctorTimeOff cancelledTimeOff = DoctorTimeOff.restore(
                TIME_OFF_ID, DOCTOR_ID, START_TIME, END_TIME, "Nghi phep",
                TimeOffStatus.CANCELLED, CREATED_BY, CREATED_AT, CREATED_AT);
        when(doctorTimeOffRepository.findByDoctorId(DOCTOR_ID)).thenReturn(List.of(cancelledTimeOff));

        List<DoctorTimeOffResult> results = service.getTimeOffs(DOCTOR_ID);

        assertEquals(1, results.size());
        assertEquals(TimeOffStatus.CANCELLED, results.get(0).status());
        assertTrue(results.get(0).affectedAppointments().isEmpty());
        verify(appointmentRepository, never()).findActiveAppointmentsForDoctorBetween(any(), any(), any());
    }
}
