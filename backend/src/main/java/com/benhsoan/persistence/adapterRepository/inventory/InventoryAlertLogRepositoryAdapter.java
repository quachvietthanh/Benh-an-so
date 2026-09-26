package com.benhsoan.persistence.adapterRepository.inventory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.inventory.InventoryAlertLog;
import com.benhsoan.domain.inventory.enums.InventoryAlertType;
import com.benhsoan.persistence.jpaRepository.inventory.JpaInventoryAlertLogRepository;
import com.benhsoan.persistence.mapper.inventory.InventoryAlertLogPersistenceMapper;
import com.benhsoan.port.outbound.repository.inventory.InventoryAlertLogRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class InventoryAlertLogRepositoryAdapter implements InventoryAlertLogRepository {

    private final JpaInventoryAlertLogRepository jpaRepository;
    private final InventoryAlertLogPersistenceMapper mapper;

    @Override
    public InventoryAlertLog save(InventoryAlertLog alertLog) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(alertLog)));
    }

    @Override
    public List<InventoryAlertLog> saveAll(Collection<InventoryAlertLog> alertLogs) {
        return jpaRepository.saveAll(
                        alertLogs.stream().map(mapper::toEntity).toList()
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<InventoryAlertLog> findActiveByMedicineIdAndAlertType(
            UUID medicineId,
            InventoryAlertType alertType
    ) {
        return jpaRepository.findFirstByMedicineIdAndAlertTypeAndResolvedAtIsNullOrderByCreatedAtDesc(
                        medicineId,
                        alertType
                )
                .map(mapper::toDomain);
    }
}
