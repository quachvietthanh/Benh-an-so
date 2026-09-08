package com.benhsoan.adapter.inbound.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.benhsoan.adapter.inbound.rest.response.queue.QueueItemResponse;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.port.dto.result.QueueItemResult;

class QueueRestMapperTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID QUEUE_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID ROOM_ID = UUID.randomUUID();
    private static final UUID VISIT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-08T10:00:00Z");

    private AnonymizationModeState state;
    private QueueRestMapper mapper;

    @BeforeEach
    void setUp() {
        state = new AnonymizationModeState();
        mapper = new QueueRestMapper(state);
    }

    @Test
    void returnsOriginalPatientNameWhenDisabled() {
        QueueItemResponse response = mapper.toResponse(result("BN001", "Nguyễn Văn A"));

        assertEquals("Nguyễn Văn A", response.patientName());
    }

    @Test
    void masksPatientNameWithPatientCodeWhenEnabled() {
        state.setEnabled(true);

        QueueItemResponse response = mapper.toResponse(result("BN001", "Nguyễn Văn A"));

        assertEquals("BỆNH NHÂN #BN001", response.patientName());
    }

    @Test
    void masksPatientNameWithGenericFallbackWhenPatientCodeIsNull() {
        state.setEnabled(true);

        QueueItemResponse response = mapper.toResponse(result(null, "Nguyễn Văn A"));

        assertEquals("BỆNH NHÂN", response.patientName());
    }

    @Test
    void toggleIsReflectedAtRuntime() {
        QueueItemResponse original = mapper.toResponse(result("BN001", "Nguyễn Văn A"));
        assertEquals("Nguyễn Văn A", original.patientName());

        state.setEnabled(true);
        QueueItemResponse masked = mapper.toResponse(result("BN001", "Nguyễn Văn A"));
        assertEquals("BỆNH NHÂN #BN001", masked.patientName());

        state.setEnabled(false);
        QueueItemResponse restored = mapper.toResponse(result("BN001", "Nguyễn Văn A"));
        assertEquals("Nguyễn Văn A", restored.patientName());
    }

    private static QueueItemResult result(String patientCode, String patientName) {
        return new QueueItemResult(
                ID, QUEUE_ID, PATIENT_ID, patientCode, patientName,
                DOCTOR_ID, "Bác sĩ B", ROOM_ID, "P101", null,
                VISIT_ID, "VIS001", QueueItemSourceType.WALK_IN, QueueItemStatus.WAITING,
                1, LocalDate.of(2026, 9, 8), NOW, null, null, null, null, null, null
        );
    }
}
