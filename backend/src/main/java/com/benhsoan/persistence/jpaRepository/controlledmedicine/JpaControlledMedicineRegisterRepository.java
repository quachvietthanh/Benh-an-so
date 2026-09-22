package com.benhsoan.persistence.jpaRepository.controlledmedicine;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.benhsoan.persistence.entity.controlledmedicine.ControlledMedicineRegisterEntity;

public interface JpaControlledMedicineRegisterRepository
        extends JpaRepository<ControlledMedicineRegisterEntity, UUID>,
        JpaSpecificationExecutor<ControlledMedicineRegisterEntity> {
}
