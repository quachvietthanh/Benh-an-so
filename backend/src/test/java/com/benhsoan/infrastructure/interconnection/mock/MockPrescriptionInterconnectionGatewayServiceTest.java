package com.benhsoan.infrastructure.interconnection.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.benhsoan.config.MockInterconnectionGatewayProperties;
import com.benhsoan.port.outbound.interconnection.PrescriptionInterconnectionGatewayRequest;

class MockPrescriptionInterconnectionGatewayServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-21T10:30:00Z");

    @Test
    void acceptsOnceAndReturnsTheSameReceiptForAnIdempotentReplay() {
        MockPrescriptionInterconnectionGatewayService service = service(MockInterconnectionGatewayMode.ACCEPT);
        var request = request("RX000123", "1 vien");

        var first = service.submit("RX000123", request);
        var replay = service.submit("RX000123", request);

        assertEquals("ACCEPTED", first.response().status());
        assertTrue(replay.idempotentReplay());
        assertEquals(first.response().receiptCode(), replay.response().receiptCode());
    }

    @Test
    void rejectsReusingAnAcceptedKeyWithADifferentPayload() {
        MockPrescriptionInterconnectionGatewayService service = service(MockInterconnectionGatewayMode.ACCEPT);
        service.submit("RX000123", request("RX000123", "1 vien"));

        MockInterconnectionGatewayException exception = assertThrows(
                MockInterconnectionGatewayException.class,
                () -> service.submit("RX000123", request("RX000123", "2 vien"))
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("IDEMPOTENCY_KEY_REUSED", exception.getCode());
    }

    @Test
    void supportsConfiguredFailureModes() {
        assertMode(MockInterconnectionGatewayMode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
        assertMode(MockInterconnectionGatewayMode.SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "MOCK_GATEWAY_ERROR");
        assertMode(MockInterconnectionGatewayMode.NO_RESPONSE, HttpStatus.GATEWAY_TIMEOUT, "MOCK_GATEWAY_NO_RESPONSE");
    }

    private void assertMode(MockInterconnectionGatewayMode mode, HttpStatus status, String code) {
        MockInterconnectionGatewayException exception = assertThrows(
                MockInterconnectionGatewayException.class,
                () -> service(mode).submit("RX000123", request("RX000123", "1 vien"))
        );
        assertEquals(status, exception.getStatus());
        assertEquals(code, exception.getCode());
    }

    private MockPrescriptionInterconnectionGatewayService service(MockInterconnectionGatewayMode mode) {
        return new MockPrescriptionInterconnectionGatewayService(
                () -> NOW,
                new MockInterconnectionGatewayProperties(true, mode, Duration.ZERO)
        );
    }

    private PrescriptionInterconnectionGatewayRequest request(String prescriptionCode, String dosage) {
        return new PrescriptionInterconnectionGatewayRequest(
                prescriptionCode,
                NOW,
                new PrescriptionInterconnectionGatewayRequest.Clinic("1", "Clinic", null, null),
                new PrescriptionInterconnectionGatewayRequest.Doctor(UUID.randomUUID(), "Doctor"),
                new PrescriptionInterconnectionGatewayRequest.Patient(UUID.randomUUID(), "BN000123", "Patient"),
                List.of(new PrescriptionInterconnectionGatewayRequest.Item(
                        UUID.randomUUID(), "Paracetamol 500 mg", "Paracetamol", "500 mg", "vien",
                        dosage, 3, "ORAL", 3, 9, null
                ))
        );
    }
}
