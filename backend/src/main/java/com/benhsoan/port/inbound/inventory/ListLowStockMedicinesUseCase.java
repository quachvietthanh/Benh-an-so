package com.benhsoan.port.inbound.inventory;

import java.util.List;

import com.benhsoan.port.dto.result.LowStockMedicineResult;

public interface ListLowStockMedicinesUseCase {

    List<LowStockMedicineResult> list();
}
