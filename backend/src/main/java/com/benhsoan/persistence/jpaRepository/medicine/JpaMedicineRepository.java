package com.benhsoan.persistence.jpaRepository.medicine;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.benhsoan.persistence.entity.medicine.MedicineEntity;

public interface JpaMedicineRepository
        extends JpaRepository<MedicineEntity, UUID>,
        JpaSpecificationExecutor<MedicineEntity> {
}
