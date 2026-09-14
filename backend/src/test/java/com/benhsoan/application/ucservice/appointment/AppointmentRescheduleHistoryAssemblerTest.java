package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.appointment.AppointmentRescheduleLog;
import com.benhsoan.domain.auth.User;
import com.benhsoan.port.dto.result.appointment.AppointmentRescheduleHistoryResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRescheduleLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

@ExtendWith(MockitoExtension.class)
class AppointmentRescheduleHistoryAssemblerTest {

    @Mock
    private AppointmentRescheduleLogRepository rescheduleLogRepository;

    @Mock
    private UserRepository userRepository;

    @Captor
    private ArgumentCaptor<List<UUID>> userIdsCaptor;

    private AppointmentRescheduleHistoryAssembler assembler;

    private final Instant fixedNow = Instant.parse("2026-09-14T08:00:00Z");

    @BeforeEach
    void setUp() {
        assembler = new AppointmentRescheduleHistoryAssembler(rescheduleLogRepository, userRepository);
    }

    private User createUser(UUID id, String fullName) {
        return User.restore(
                id, "user_" + id, "hash", fullName, "user@test.com", "0900000000",
                UUID.randomUUID(), true, null, fixedNow
        );
    }

    @Test
    void resolvesHistoriesWithSingleBatchQueryEliminatingNPlusOne() {
        UUID appointmentId = UUID.randomUUID();
        UUID oldDoctorId = UUID.randomUUID();
        UUID newDoctor1Id = UUID.randomUUID();
        UUID newDoctor2Id = UUID.randomUUID();
        UUID receptionistId = UUID.randomUUID();

        AppointmentRescheduleLog log1 = AppointmentRescheduleLog.create(
                appointmentId, oldDoctorId, newDoctor1Id,
                fixedNow.plusSeconds(3600), fixedNow.plusSeconds(5400),
                fixedNow.plusSeconds(7200), fixedNow.plusSeconds(9000),
                "Dời lần 1", receptionistId, fixedNow.minusSeconds(1800)
        );

        AppointmentRescheduleLog log2 = AppointmentRescheduleLog.create(
                appointmentId, newDoctor1Id, newDoctor2Id,
                fixedNow.plusSeconds(7200), fixedNow.plusSeconds(9000),
                fixedNow.plusSeconds(10800), fixedNow.plusSeconds(12600),
                "Dời lần 2", receptionistId, fixedNow.minusSeconds(900)
        );

        when(rescheduleLogRepository.findByAppointmentId(appointmentId)).thenReturn(List.of(log1, log2));

        User oldDoc = createUser(oldDoctorId, "Dr. Old");
        User newDoc1 = createUser(newDoctor1Id, "Dr. Mid");
        User newDoc2 = createUser(newDoctor2Id, "Dr. New");
        User rec = createUser(receptionistId, "Receptionist Anna");

        when(userRepository.findAllById(any())).thenReturn(List.of(oldDoc, newDoc1, newDoc2, rec));

        List<AppointmentRescheduleHistoryResult> results = assembler.getHistoriesForAppointment(appointmentId);

        assertEquals(2, results.size());

        // Verify N+1 eliminated: findAllById was called exactly ONCE, findById was NEVER called
        verify(userRepository, times(1)).findAllById(userIdsCaptor.capture());
        verify(userRepository, never()).findById(any());

        List<UUID> capturedIds = userIdsCaptor.getValue();
        assertTrue(capturedIds.contains(oldDoctorId));
        assertTrue(capturedIds.contains(newDoctor1Id));
        assertTrue(capturedIds.contains(newDoctor2Id));
        assertTrue(capturedIds.contains(receptionistId));

        AppointmentRescheduleHistoryResult res1 = results.get(0);
        assertEquals("Dr. Old", res1.oldDoctorName());
        assertEquals("Dr. Mid", res1.newDoctorName());
        assertEquals("Receptionist Anna", res1.rescheduledByName());

        AppointmentRescheduleHistoryResult res2 = results.get(1);
        assertEquals("Dr. Mid", res2.oldDoctorName());
        assertEquals("Dr. New", res2.newDoctorName());
        assertEquals("Receptionist Anna", res2.rescheduledByName());
    }

    @Test
    void returnsEmptyListWhenNoLogsFound() {
        UUID appointmentId = UUID.randomUUID();
        when(rescheduleLogRepository.findByAppointmentId(appointmentId)).thenReturn(List.of());

        List<AppointmentRescheduleHistoryResult> results = assembler.getHistoriesForAppointment(appointmentId);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(userRepository, never()).findAllById(any());
        verify(userRepository, never()).findById(any());
    }
}
