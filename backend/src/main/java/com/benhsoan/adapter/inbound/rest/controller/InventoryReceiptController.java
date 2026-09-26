package com.benhsoan.adapter.inbound.rest.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.benhsoan.adapter.inbound.rest.mapper.InventoryReceiptRestMapper;
import com.benhsoan.adapter.inbound.rest.request.inventory.CreateInventoryReceiptRequest;
import com.benhsoan.adapter.inbound.rest.response.inventory.InventoryReceiptResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.inbound.inventory.ReceiveStockUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/inventory")
public class InventoryReceiptController {

    private final ReceiveStockUseCase receiveStockUseCase;
    private final InventoryReceiptRestMapper restMapper;

    @PostMapping("/receipts")
    @RequirePermission("PHARMACY_CREATE")
    public ResponseEntity<InventoryReceiptResponse> receiveStock(
            @Valid @RequestBody CreateInventoryReceiptRequest request
    ) {
        InventoryReceiptResponse response = restMapper.toResponse(
                receiveStockUseCase.receiveStock(restMapper.toCommand(request))
        );
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{receiptId}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }
}
