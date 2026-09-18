package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.VisitRestMapper;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.command.visit.HandoverPatientCommand;
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.dto.result.VisitEncounterResult;
import com.benhsoan.port.dto.result.VisitHandoverResult;
import com.benhsoan.port.inbound.user.GetDoctorsUseCase;
import com.benhsoan.port.inbound.visit.GetVisitEncounterUseCase;
import com.benhsoan.port.inbound.visit.GetVisitHandoversUseCase;
import com.benhsoan.port.inbound.visit.HandoverPatientUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = VisitController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({VisitRestMapper.class, AnonymizationModeState.class})
class VisitControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private GetVisitEncounterUseCase getVisitEncounterUseCase;
    @MockitoBean private HandoverPatientUseCase handoverPatientUseCase;
    @MockitoBean private GetVisitHandoversUseCase getVisitHandoversUseCase;
    @MockitoBean private GetDoctorsUseCase getDoctorsUseCase;
    @MockitoBean private com.benhsoan.port.inbound.visit.GetVisitSummaryUseCase getVisitSummaryUseCase;
    @MockitoBean private com.benhsoan.port.inbound.visit.ExportVisitSummaryUseCase exportVisitSummaryUseCase;

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

    @Test
    void handoverPatientReturns200AndResult() throws Exception {
        UUID visitId = UUID.randomUUID();
        UUID targetDoctorId = UUID.randomUUID();
        UUID fromDoctorId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-17T10:00:00Z");

        VisitHandoverResult handoverResult = new VisitHandoverResult(
                UUID.randomUUID(), visitId, fromDoctorId, "Dr. Alice", targetDoctorId, "Dr. Bob",
                "Can y kien chuyen khoa", now
        );

        when(handoverPatientUseCase.handover(eq(visitId), any(HandoverPatientCommand.class)))
                .thenReturn(handoverResult);

        String jsonPayload = """
                {
                    "targetDoctorId": "%s",
                    "reason": "Can y kien chuyen khoa"
                }
                """.formatted(targetDoctorId);

        mockMvc.perform(post("/visits/{visitId}/handover", visitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitId").value(visitId.toString()))
                .andExpect(jsonPath("$.fromDoctorName").value("Dr. Alice"))
                .andExpect(jsonPath("$.toDoctorName").value("Dr. Bob"))
                .andExpect(jsonPath("$.reason").value("Can y kien chuyen khoa"));
    }

    @Test
    void getHandoversReturnsList() throws Exception {
        UUID visitId = UUID.randomUUID();
        UUID doctorAId = UUID.randomUUID();
        UUID doctorBId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-17T10:00:00Z");

        VisitHandoverResult handover = new VisitHandoverResult(
                UUID.randomUUID(), visitId, doctorAId, "Dr. Alice", doctorBId, "Dr. Bob",
                "Chuyen vien", now
        );

        when(getVisitHandoversUseCase.getHandovers(visitId)).thenReturn(List.of(handover));

        mockMvc.perform(get("/visits/{visitId}/handovers", visitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].visitId").value(visitId.toString()))
                .andExpect(jsonPath("$[0].fromDoctorName").value("Dr. Alice"))
                .andExpect(jsonPath("$[0].toDoctorName").value("Dr. Bob"))
                .andExpect(jsonPath("$[0].reason").value("Chuyen vien"));
    }

    @Test
    void getHandoverDoctorsReturnsList() throws Exception {
        UUID doctorId = UUID.randomUUID();
        UserResult doc = new UserResult(
                doctorId, "doc1", "Dr. Doctor", "doc@example.com", "0900000000", "DOCTOR", true
        );
        when(getDoctorsUseCase.getAllActiveDoctors()).thenReturn(List.of(doc));

        mockMvc.perform(get("/visits/handover/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(doctorId.toString()))
                .andExpect(jsonPath("$[0].fullName").value("Dr. Doctor"))
                .andExpect(jsonPath("$[0].username").doesNotExist())
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].phone").doesNotExist());
    }

    @Test
    void returnsVisitSummary() throws Exception {
        UUID visitId = UUID.randomUUID();
        var summaryResult = new com.benhsoan.port.dto.result.VisitSummaryResult(
                visitId, "KB-2026-0001", Instant.parse("2026-08-20T08:00:00Z"),
                new com.benhsoan.port.dto.result.VisitSummaryResult.ClinicInfo("Phòng khám A", "Địa chỉ A", "0900000000"),
                new com.benhsoan.port.dto.result.VisitSummaryResult.PatientInfo(
                        UUID.randomUUID(), "BN-01", "Nguyễn Văn A", LocalDate.of(1990, 1, 1),
                        Gender.MALE, "0900000000", "012345678901"),
                new com.benhsoan.port.dto.result.VisitSummaryResult.DoctorInfo(UUID.randomUUID(), "BS. B"),
                new com.benhsoan.port.dto.result.VisitSummaryResult.MedicalRecordInfo(
                        UUID.randomUUID(), com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus.SIGNED,
                        Instant.parse("2026-08-20T08:30:00Z"), UUID.randomUUID(), "BS. B"),
                java.util.List.of(new com.benhsoan.port.dto.result.VisitSummaryResult.DiagnosisItem("J00", "Viêm mũi họng", true)),
                java.util.List.of(new com.benhsoan.port.dto.result.VisitSummaryResult.ClinicalOrderItemInfo(
                        "ORD-01", "XQ01", "X-quang ngực", "Thẳng", "COMPLETED")),
                "Nghỉ ngơi", "Uống thuốc", LocalDate.of(2026, 8, 27),
                java.util.List.of()
        );

        when(getVisitSummaryUseCase.getSummary(visitId)).thenReturn(summaryResult);

        mockMvc.perform(get("/visits/{visitId}/summary", visitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitId").value(visitId.toString()))
                .andExpect(jsonPath("$.visitCode").value("KB-2026-0001"))
                .andExpect(jsonPath("$.patient.fullName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.clinicalOrders[0].orderCode").value("ORD-01"))
                .andExpect(jsonPath("$.clinicalOrders[0].serviceCode").value("XQ01"))
                .andExpect(jsonPath("$.doctorInstructions").value("Nghỉ ngơi"))
                .andExpect(jsonPath("$.revisitDate").value("2026-08-27"));
    }

    @Test
    void printsVisitSummaryPdf() throws Exception {
        UUID visitId = UUID.randomUUID();
        byte[] fakePdf = new byte[]{1, 2, 3};
        var printResult = new com.benhsoan.port.dto.result.VisitSummaryPrintResult(
                "phieu-tom-tat-KB-001.pdf", "application/pdf", fakePdf
        );

        when(exportVisitSummaryUseCase.export(visitId)).thenReturn(printResult);

        mockMvc.perform(get("/visits/{visitId}/summary/print", visitId))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string(
                        "Content-Disposition", "attachment; filename=\"phieu-tom-tat-KB-001.pdf\""))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentTypeCompatibleWith(org.springframework.http.MediaType.APPLICATION_PDF))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes(fakePdf));
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
