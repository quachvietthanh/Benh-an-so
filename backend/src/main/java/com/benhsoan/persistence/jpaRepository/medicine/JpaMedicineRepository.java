package com.benhsoan.persistence.jpaRepository.medicine;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.medicine.MedicineEntity;

public interface JpaMedicineRepository
        extends JpaRepository<MedicineEntity, UUID>,
        JpaSpecificationExecutor<MedicineEntity> {

    @Modifying
    @Query("UPDATE MedicineEntity m SET m.stockQuantity = m.stockQuantity + :delta WHERE m.id = :id")
    int addStockQuantity(@Param("id") UUID id, @Param("delta") int delta);
}
