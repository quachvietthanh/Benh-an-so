package com.benhsoan.persistence.jpaRepository.clinical;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.clinical.ClinicalOrderEntity;

public interface JpaClinicalOrderRepository extends JpaRepository<ClinicalOrderEntity, UUID> {

    Page<ClinicalOrderEntity> findByVisitIdOrderByOrderedAtDesc(UUID visitId, Pageable pageable);

    boolean existsByOrderCode(String orderCode);

    @Query("select order.id from ClinicalOrderEntity order where order.medicalRecordId = :medicalRecordId")
    List<UUID> findIdsByMedicalRecordId(@Param("medicalRecordId") UUID medicalRecordId);

    @Modifying
    @Query("delete from ClinicalOrderEntity order where order.medicalRecordId = :medicalRecordId")
    void deleteByMedicalRecordId(@Param("medicalRecordId") UUID medicalRecordId);
}
