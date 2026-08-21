package com.benhsoan.adapter.inbound.rest.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.response.interconnection.MockInterconnectionGatewayErrorResponse;
import com.benhsoan.infrastructure.interconnection.mock.MockInterconnectionGatewayException;
import com.benhsoan.infrastructure.interconnection.mock.MockPrescriptionInterconnectionGatewayService;
import com.benhsoan.port.outbound.interconnection.PrescriptionInterconnectionGatewayRequest;
import com.benhsoan.port.outbound.interconnection.PrescriptionInterconnectionGatewayResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/mock-interconnection/v1")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "interconnection.mock-gateway", name = "enabled", havingValue = "true")
public class MockInterconnectionGatewayController {

    private final MockPrescriptionInterconnectionGatewayService gatewayService;

    @PostMapping("/prescriptions")
    public ResponseEntity<?> receive(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) PrescriptionInterconnectionGatewayRequest request
    ) {
        try {
            var result = gatewayService.submit(idempotencyKey, request);
            HttpStatus status = result.idempotentReplay() ? HttpStatus.OK : HttpStatus.CREATED;
            return ResponseEntity.status(status).body(result.response());
        } catch (MockInterconnectionGatewayException ex) {
            return ResponseEntity.status(ex.getStatus())
                    .body(new MockInterconnectionGatewayErrorResponse(ex.getCode(), ex.getMessage()));
        }
    }
}
