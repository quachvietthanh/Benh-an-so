package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.InventoryRestMapper;
import com.benhsoan.adapter.inbound.rest.response.inventory.InventoryBatchResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.InventoryStockResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.LowStockMedicineResponse;
import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.port.dto.query.inventory.ListInventoryBatchesQuery;
import com.benhsoan.port.dto.query.inventory.ListInventoryStocksQuery;
import com.benhsoan.port.inbound.inventory.ListInventoryBatchesUseCase;
import com.benhsoan.port.inbound.inventory.ListLowStockMedicinesUseCase;
import com.benhsoan.port.inbound.inventory.ListInventoryStocksUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final ListInventoryStocksUseCase listInventoryStocksUseCase;
    private final ListInventoryBatchesUseCase listInventoryBatchesUseCase;
    private final ListLowStockMedicinesUseCase listLowStockMedicinesUseCase;
    private final InventoryRestMapper mapper;

    @GetMapping("/stocks")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public List<InventoryStockResponse> listStocks(
            @RequestParam(required = false) Boolean active
    ) {
        return mapper.toStockResponses(
                listInventoryStocksUseCase.list(new ListInventoryStocksQuery(active))
        );
    }

    @GetMapping("/batches")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public List<InventoryBatchResponse> listBatches(
            @RequestParam(required = false) UUID medicineId,
            @RequestParam(required = false) BatchStatus status,
            @RequestParam(required = false) Boolean eligibleForDispense
    ) {
        return mapper.toBatchResponses(
                listInventoryBatchesUseCase.list(
                        new ListInventoryBatchesQuery(medicineId, status, eligibleForDispense)
                )
        );
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public List<LowStockMedicineResponse> listLowStockMedicines() {
        return mapper.toLowStockResponses(
                listLowStockMedicinesUseCase.list()
        );
    }
}
