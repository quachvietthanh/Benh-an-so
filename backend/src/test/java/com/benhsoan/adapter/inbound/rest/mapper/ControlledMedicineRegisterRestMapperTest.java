package com.benhsoan.adapter.inbound.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.benhsoan.adapter.inbound.rest.response.controlledmedicine.ControlledMedicineRegisterResponse;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.port.dto.result.ControlledMedicineRegisterResult;

class ControlledMedicineRegisterRestMapperTest {

    private static final Instant NOW = Instant.parse("2026-09-21T10:00:00Z");
    private static final UUID ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();

    private AnonymizationModeState state;
    private ControlledMedicineRegisterRestMapper mapper;

    @BeforeEach
    void setUp() {
        state = new AnonymizationModeState();
        mapper = new ControlledMedicineRegisterRestMapper(state);
    }

    @Test
    void returnsOriginalPatientNameWhenAnonymizationOff() {
        ControlledMedicineRegisterResponse response = mapper.toResponse(result("BN-0001", "Nguyen Van A"));

        assertEquals("Nguyen Van A", response.patientName());
        assertEquals("BN-0001", response.patientCode());
        assertEquals(PATIENT_ID, response.patientId());
    }

    @Test
    void masksPatientNameWithPatientCodeWhenAnonymizationOn() {
        state.setEnabled(true);

        ControlledMedicineRegisterResponse response = mapper.toResponse(result("BN-0001", "Nguyen Van A"));

        assertEquals("BỆNH NHÂN #BN-0001", response.patientName());
    }

    @Test
    void masksPatientNameWithGenericFallbackWhenPatientCodeIsNull() {
        state.setEnabled(true);

        ControlledMedicineRegisterResponse response = mapper.toResponse(result(null, "Nguyen Van A"));

        assertEquals("BỆNH NHÂN", response.patientName());
    }

    @Test
    void doesNotAlterPatientIdOrPatientCodeWhenAnonymizationOn() {
        state.setEnabled(true);

        ControlledMedicineRegisterResponse response = mapper.toResponse(result("BN-0001", "Nguyen Van A"));

        assertEquals(PATIENT_ID, response.patientId());
        assertEquals("BN-0001", response.patientCode());
    }

    private ControlledMedicineRegisterResult result(String patientCode, String patientName) {
        return new ControlledMedicineRegisterResult(
                ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Morphine 10mg",
                PATIENT_ID,
                patientCode,
                patientName,
                UUID.randomUUID(),
                "Dr. B",
                UUID.randomUUID(),
                "Pharmacist C",
                5,
                NOW
        );
    }
}
