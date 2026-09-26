package com.benhsoan.port.outbound.repository.inventory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.inventory.InventoryAlertLog;
import com.benhsoan.domain.inventory.enums.InventoryAlertType;

public interface InventoryAlertLogRepository {

    InventoryAlertLog save(InventoryAlertLog alertLog);

    List<InventoryAlertLog> saveAll(Collection<InventoryAlertLog> alertLogs);

    Optional<InventoryAlertLog> findActiveByMedicineIdAndAlertType(
            UUID medicineId,
            InventoryAlertType alertType
    );
}
