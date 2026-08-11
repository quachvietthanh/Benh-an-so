package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.PrescriptionRestMapper;
import com.benhsoan.domain.druginteraction.enums.InteractionSeverity;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.port.dto.result.DispenseAllocationResult;
import com.benhsoan.port.dto.result.DispensePrescriptionResult;
import com.benhsoan.port.dto.result.DrugInteractionWarningResult;
import com.benhsoan.port.dto.result.PrescriptionItemResult;
import com.benhsoan.port.dto.result.PrescriptionResult;
import com.benhsoan.port.inbound.prescription.AmendPrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.CancelPrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.CheckDrugInteractionUseCase;
import com.benhsoan.port.inbound.prescription.CreatePrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.DispensePrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.GetPrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.GetPrescriptionsByMedicalRecordUseCase;
import com.benhsoan.port.inbound.prescription.SearchPrescriptionsUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = PrescriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PrescriptionRestMapper.class)
@DisplayName("PrescriptionController - MockMvc Tests")
class PrescriptionControllerTest {

    private static final Instant NOW = Instant.parse("2026-08-07T02:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreatePrescriptionUseCase createPrescriptionUseCase;

    @MockitoBean
    private AmendPrescriptionUseCase amendPrescriptionUseCase;

    @MockitoBean
    private GetPrescriptionUseCase getPrescriptionUseCase;

    @MockitoBean
    private GetPrescriptionsByMedicalRecordUseCase getPrescriptionsByMedicalRecordUseCase;

    @MockitoBean
    private SearchPrescriptionsUseCase searchPrescriptionsUseCase;

    @MockitoBean
    private DispensePrescriptionUseCase dispensePrescriptionUseCase;

    @MockitoBean
    private CancelPrescriptionUseCase cancelPrescriptionUseCase;

    @MockitoBean
    private CheckDrugInteractionUseCase checkDrugInteractionUseCase;

    @MockitoBean
    private CurrentUserPort currentUserPort;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserSessionRepository userSessionRepository;

    @MockitoBean
    private JwtTokenPort jwtTokenPort;

    @MockitoBean
    private ClockPort clockPort;

    @Test
    @DisplayName("GET /prescriptions - 200 with pending dispensing page")
    void searchReturnsPendingDispensingPage() throws Exception {
        UUID prescriptionId = UUID.randomUUID();
        PrescriptionResult prescription = new PrescriptionResult(
                prescriptionId,
                "RX-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "VISIT-001",
                UUID.randomUUID(),
                "PAT-001",
                "Nguyen Van A",
                PrescriptionStatus.PENDING_DISPENSE,
                null,
                UUID.randomUUID(),
                "Dr. B",
                NOW,
                null,
                null,
                List.of(),
                List.of()
        );
        when(searchPrescriptionsUseCase.search(any()))
                .thenReturn(new PageImpl<>(
                        List.of(prescription),
                        PageRequest.of(0, 20),
                        1
                ));

        mockMvc.perform(get("/prescriptions")
                        .param("status", "PENDING_DISPENSE")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(prescriptionId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("PENDING_DISPENSE"))
                .andExpect(jsonPath("$.content[0].patientName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /prescriptions - 400 for invalid status")
    void searchRejectsInvalidStatus() throws Exception {
        mockMvc.perform(get("/prescriptions")
                        .param("status", "PENDING_DISPENSING"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /prescriptions/check-interactions - 200 OK with sorted warnings")
    void checkInteractionsReturnsWarnings() throws Exception {
        UUID aspirinId = UUID.randomUUID();
        UUID warfarinId = UUID.randomUUID();

        when(checkDrugInteractionUseCase.check(any()))
                .thenReturn(List.of(
                        new DrugInteractionWarningResult(
                                UUID.randomUUID(),
                                aspirinId,
                                warfarinId,
                                InteractionSeverity.SEVERE,
                                "Aspirin làm tăng nguy cơ chảy máu khi phối hợp với Warfarin.",
                                "Cân nhắc ngừng Aspirin."
                        )
                ));

        mockMvc.perform(post("/prescriptions/check-interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"drugIds":["%s","%s"]}
                                """.formatted(aspirinId, warfarinId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].drugIdA").value(aspirinId.toString()))
                .andExpect(jsonPath("$[0].drugIdB").value(warfarinId.toString()))
                .andExpect(jsonPath("$[0].severity").value("SEVERE"))
                .andExpect(jsonPath("$[0].description").value(
                        "Aspirin làm tăng nguy cơ chảy máu khi phối hợp với Warfarin."))
                .andExpect(jsonPath("$[0].clinicalRecommendation").value(
                        "Cân nhắc ngừng Aspirin."));
    }

    @Test
    @DisplayName("POST /prescriptions/check-interactions - 200 OK with empty array when no interaction")
    void checkInteractionsReturnsEmptyArray() throws Exception {
        when(checkDrugInteractionUseCase.check(any())).thenReturn(List.of());

        mockMvc.perform(post("/prescriptions/check-interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"drugIds":["%s","%s"]}
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("POST /prescriptions/check-interactions - 400 when drugIds is empty")
    void checkInteractionsRejectsEmptyDrugIds() throws Exception {
        mockMvc.perform(post("/prescriptions/check-interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"drugIds":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /prescriptions/check-interactions - 400 when drugIds is missing")
    void checkInteractionsRejectsMissingDrugIds() throws Exception {
        mockMvc.perform(post("/prescriptions/check-interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /prescriptions/{id}/dispense - 200 OK with allocation payload")
    void dispenseReturnsAllocationPayload() throws Exception {
        UUID prescriptionId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        UUID prescriptionItemId = UUID.randomUUID();
        UUID dispenseItemId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID medicalRecordId = UUID.randomUUID();
        UUID pharmacistId = UUID.randomUUID();

        PrescriptionResult prescription = new PrescriptionResult(
                prescriptionId,
                "RX-001",
                medicalRecordId,
                visitId,
                "VISIT-001",
                patientId,
                "PAT-001",
                "Nguyen Van A",
                PrescriptionStatus.DISPENSED,
                "Cap phat ngay",
                doctorId,
                "Dr. B",
                NOW.minusSeconds(3600),
                pharmacistId,
                NOW,
                List.of(new PrescriptionItemResult(
                        prescriptionItemId,
                        prescriptionId,
                        medicineId,
                        "Paracetamol",
                        "Paracetamol",
                        "500 mg",
                        "viên",
                        "1 viên",
                        "2 lan/ngay",
                        AdministrationRoute.ORAL,
                        5,
                        20,
                        "Sau an",
                        NOW.minusSeconds(3600),
                        NOW
                )),
                List.of()
        );

        when(dispensePrescriptionUseCase.dispense(prescriptionId))
                .thenReturn(new DispensePrescriptionResult(
                        prescription,
                        pharmacistId,
                        NOW,
                        1,
                        20,
                        List.of(new DispenseAllocationResult(
                                dispenseItemId,
                                prescriptionItemId,
                                medicineId,
                                "MED-001",
                                "Paracetamol",
                                batchId,
                                "BATCH-001",
                                LocalDate.of(2026, 12, 31),
                                20,
                                15
                        ))
                ));

        mockMvc.perform(post("/prescriptions/{id}/dispense", prescriptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prescription.id").value(prescriptionId.toString()))
                .andExpect(jsonPath("$.prescription.status").value("DISPENSED"))
                .andExpect(jsonPath("$.dispensedBy").value(pharmacistId.toString()))
                .andExpect(jsonPath("$.allocationCount").value(1))
                .andExpect(jsonPath("$.totalDispensedQuantity").value(20))
                .andExpect(jsonPath("$.allocations[0].batchNumber").value("BATCH-001"))
                .andExpect(jsonPath("$.allocations[0].dispensedQuantity").value(20))
                .andExpect(jsonPath("$.allocations[0].batchQuantityRemaining").value(15));
    }
}
