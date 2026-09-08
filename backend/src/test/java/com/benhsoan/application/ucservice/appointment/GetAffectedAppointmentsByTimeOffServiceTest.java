package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import com.benhsoan.domain.appointment.exception.DoctorTimeOffNotFoundException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.dto.result.appointment.AffectedAppointmentResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

@ExtendWith(MockitoExtension.class)
class GetAffectedAppointmentsByTimeOffServiceTest {

    private static final UUID TIME_OFF_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID APPOINTMENT_ID = UUID.randomUUID();
    private static final Instant START = Instant.parse("2026-09-15T08:00:00Z");
    private static final Instant END = Instant.parse("2026-09-15T12:00:00Z");

    @Mock private DoctorTimeOffRepository doctorTimeOffRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PatientRepository patientRepository;

    private GetAffectedAppointmentsByTimeOffService service;

    @BeforeEach
    void setUp() {
        service = new GetAffectedAppointmentsByTimeOffService(
                doctorTimeOffRepository,
                appointmentRepository,
                patientRepository
        );
    }

    @Test
    void getAffectedAppointments_ActiveTimeOff_ReturnsConflictingAppointments() {
        DoctorTimeOff timeOff = DoctorTimeOff.restore(
                TIME_OFF_ID, DOCTOR_ID, START, END,
                "Hội nghị", TimeOffStatus.ACTIVE, UUID.randomUUID(), Instant.now(), null
        );
        when(doctorTimeOffRepository.findById(TIME_OFF_ID)).thenReturn(Optional.of(timeOff));

        Appointment appointment = Appointment.restore(
                APPOINTMENT_ID, "AP123456", PATIENT_ID, DOCTOR_ID,
                START.plusSeconds(1800), START.plusSeconds(3600), AppointmentStatus.SCHEDULED,
                "Khám định kỳ", null, null, null, UUID.randomUUID(), Instant.now()
        );
        when(appointmentRepository.findActiveAppointmentsForDoctorBetween(DOCTOR_ID, START, END))
                .thenReturn(List.of(appointment));

        Patient patient = mock(Patient.class);
        when(patient.getFullName()).thenReturn("Nguyen Van A");
        when(patient.getPhone()).thenReturn("0901234567");
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));

        List<AffectedAppointmentResult> results = service.getAffectedAppointments(TIME_OFF_ID);

        assertNotNull(results);
        assertEquals(1, results.size());
        AffectedAppointmentResult first = results.get(0);
        assertEquals(APPOINTMENT_ID, first.id());
        assertEquals("AP123456", first.appointmentCode());
        assertEquals("Nguyen Van A", first.patientFullName());
        assertEquals("0901234567", first.patientPhone());
    }

    @Test
    void getAffectedAppointments_CancelledTimeOff_ReturnsEmptyList_FINDING05() {
        DoctorTimeOff timeOff = DoctorTimeOff.restore(
                TIME_OFF_ID, DOCTOR_ID, START, END,
                "Hội nghị", TimeOffStatus.CANCELLED, UUID.randomUUID(), Instant.now(), Instant.now()
        );
        when(doctorTimeOffRepository.findById(TIME_OFF_ID)).thenReturn(Optional.of(timeOff));

        List<AffectedAppointmentResult> results = service.getAffectedAppointments(TIME_OFF_ID);

        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(appointmentRepository, never()).findActiveAppointmentsForDoctorBetween(any(), any(), any());
        verify(patientRepository, never()).findById(any());
    }

    @Test
    void getAffectedAppointments_NotFound_ThrowsException() {
        when(doctorTimeOffRepository.findById(TIME_OFF_ID)).thenReturn(Optional.empty());

        assertThrows(DoctorTimeOffNotFoundException.class, () -> service.getAffectedAppointments(TIME_OFF_ID));
    }

    private static Patient mock(Class<Patient> clazz) {
        return org.mockito.Mockito.mock(clazz);
    }
}
