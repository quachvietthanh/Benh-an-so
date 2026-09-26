package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.inventory.CreateInventoryReceiptRequest;
import com.benhsoan.adapter.inbound.rest.request.inventory.ReceiptItemRequest;
import com.benhsoan.adapter.inbound.rest.response.inventory.InventoryReceiptResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.InventoryReceiptWarningResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.ReceiptItemResponse;
import com.benhsoan.port.dto.command.inventory.ReceiveStockCommand;
import com.benhsoan.port.dto.command.inventory.ReceiveStockItemCommand;
import com.benhsoan.port.dto.result.InventoryReceiptItemResult;
import com.benhsoan.port.dto.result.InventoryReceiptResult;
import com.benhsoan.port.dto.result.InventoryReceiptWarningResult;

@Component
public class InventoryReceiptRestMapper {

    public ReceiveStockCommand toCommand(CreateInventoryReceiptRequest request) {
        var items = request.items().stream()
                .map(this::toItemCommand)
                .toList();

        return new ReceiveStockCommand(request.note(), items);
    }

    private ReceiveStockItemCommand toItemCommand(ReceiptItemRequest request) {
        return new ReceiveStockItemCommand(
                request.medicineId(),
                request.batchNumber(),
                request.expiryDate(),
                request.quantity(),
                request.importPrice()
        );
    }

    public InventoryReceiptResponse toResponse(InventoryReceiptResult result) {
        var items = result.items().stream()
                .map(this::toItemResponse)
                .toList();
        var warnings = result.warnings().stream()
                .map(this::toWarningResponse)
                .toList();

        return new InventoryReceiptResponse(
                result.id(),
                result.receivedBy(),
                result.receivedAt(),
                result.note(),
                result.createdAt(),
                items,
                warnings
        );
    }

    private ReceiptItemResponse toItemResponse(InventoryReceiptItemResult result) {
        return new ReceiptItemResponse(
                result.id(),
                result.medicineId(),
                result.batchNumber(),
                result.expiryDate(),
                result.quantity(),
                result.importPrice(),
                result.totalValue()
        );
    }

    private InventoryReceiptWarningResponse toWarningResponse(InventoryReceiptWarningResult result) {
        return new InventoryReceiptWarningResponse(
                result.code(),
                result.medicineId(),
                result.batchNumber(),
                result.message()
        );
    }
}
