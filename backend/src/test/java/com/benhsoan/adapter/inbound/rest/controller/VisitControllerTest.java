package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.VisitRestMapper;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.result.VisitEncounterResult;
import com.benhsoan.port.inbound.visit.GetVisitEncounterUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = VisitController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(VisitRestMapper.class)
class VisitControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private GetVisitEncounterUseCase getVisitEncounterUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private ClockPort clockPort;

    @Test
    void returnsEncounterSnapshot() throws Exception {
        UUID visitId = UUID.randomUUID();
        when(getVisitEncounterUseCase.getEncounter(visitId)).thenReturn(encounter(visitId));

        mockMvc.perform(get("/visits/{visitId}/encounter", visitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visit.id").value(visitId.toString()))
                .andExpect(jsonPath("$.patient.patientCode").value("BN000100"))
                .andExpect(jsonPath("$.doctor.fullName").value("Bac si Nguyen Van B"))
                .andExpect(jsonPath("$.room.roomNumber").value("P101"))
                .andExpect(jsonPath("$.queueItem.queueNumber").value(3))
                .andExpect(jsonPath("$.appointment.appointmentCode").value("AP000100"))
                .andExpect(jsonPath("$.medicalRecord.status").value("DRAFT"));
    }

    private VisitEncounterResult encounter(UUID visitId) {
        Instant now = Instant.parse("2026-08-03T02:00:00Z");
        return new VisitEncounterResult(
                new VisitEncounterResult.VisitInfo(
                        visitId, "VIS000100", VisitType.APPOINTMENT, VisitStatus.IN_PROGRESS,
                        now, now.plusSeconds(60), "Kham tong quat", "Theo doi"),
                new VisitEncounterResult.PatientInfo(
                        UUID.randomUUID(), "BN000100", "Nguyen Van A", LocalDate.of(1990, 1, 1),
                        Gender.MALE, "0900000000"),
                new VisitEncounterResult.DoctorInfo(UUID.randomUUID(), "Bac si Nguyen Van B"),
                new VisitEncounterResult.RoomInfo(UUID.randomUUID(), "P101"),
                new VisitEncounterResult.QueueItemInfo(UUID.randomUUID(), 3,
                        com.benhsoan.domain.queue.enums.QueueItemStatus.IN_PROGRESS, now, now.plusSeconds(60)),
                new VisitEncounterResult.AppointmentInfo(UUID.randomUUID(), "AP000100",
                        com.benhsoan.domain.appointment.enums.AppointmentStatus.IN_PROGRESS),
                new VisitEncounterResult.MedicalRecordInfo(UUID.randomUUID(),
                        com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus.DRAFT, null));
    }
}
