package com.benhsoan.persistence.jpaRepository.clinical;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.clinical.ClinicalOrderEntity;

public interface JpaClinicalOrderRepository extends JpaRepository<ClinicalOrderEntity, UUID> {

    Page<ClinicalOrderEntity> findByVisitIdOrderByOrderedAtDesc(UUID visitId, Pageable pageable);

    boolean existsByOrderCode(String orderCode);
}
