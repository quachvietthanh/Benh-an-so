package com.benhsoan.adapter.inbound.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.benhsoan.adapter.inbound.rest.response.patient.PatientResponse;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.port.dto.result.PatientResult;

class PatientRestMapperTest {

    private static final UUID ID = UUID.randomUUID();

    private AnonymizationModeState state;
    private PatientRestMapper mapper;

    @BeforeEach
    void setUp() {
        state = new AnonymizationModeState();
        mapper = new PatientRestMapper(state);
    }

    @Test
    void returnsOriginalPiiWhenDisabled() {
        PatientResponse response = mapper.toResponse(result());

        assertEquals("Nguyễn Văn A", response.fullName());
        assertEquals("0912345678", response.phone());
        assertEquals("123 Nguyễn Trãi, Hà Nội", response.address());
    }

    @Test
    void masksPiiWhenEnabled() {
        state.setEnabled(true);

        PatientResponse response = mapper.toResponse(result());

        assertEquals("BỆNH NHÂN #BN001", response.fullName());
        assertEquals("09******78", response.phone());
        assertEquals("[ĐỊA CHỈ ĐÃ ẨN DANH]", response.address());
    }

    @Test
    void toggleIsReflectedAtRuntime() {
        PatientResponse original = mapper.toResponse(result());
        assertEquals("Nguyễn Văn A", original.fullName());

        state.setEnabled(true);
        PatientResponse masked = mapper.toResponse(result());
        assertEquals("BỆNH NHÂN #BN001", masked.fullName());

        state.setEnabled(false);
        PatientResponse restored = mapper.toResponse(result());
        assertEquals("Nguyễn Văn A", restored.fullName());
    }

    private static PatientResult result() {
        return new PatientResult(
                ID, "BN001", "Nguyễn Văn A", null, null,
                "0912345678", null, "123 Nguyễn Trãi, Hà Nội",
                null, null, null, null, null,
                true, null, null, false, null, null,
                false, null, null, false
        );
    }
}
