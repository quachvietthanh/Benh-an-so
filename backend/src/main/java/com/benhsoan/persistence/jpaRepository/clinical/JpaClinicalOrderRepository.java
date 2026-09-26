package com.benhsoan.persistence.jpaRepository.clinical;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.clinical.ClinicalOrderEntity;

import jakarta.persistence.LockModeType;

public interface JpaClinicalOrderRepository extends JpaRepository<ClinicalOrderEntity, UUID> {

    Page<ClinicalOrderEntity> findByVisitIdOrderByOrderedAtDesc(UUID visitId, Pageable pageable);

    boolean existsByOrderCode(String orderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from ClinicalOrderEntity o where o.id = :id")
    Optional<ClinicalOrderEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("select order.id from ClinicalOrderEntity order where order.medicalRecordId = :medicalRecordId")
    List<UUID> findIdsByMedicalRecordId(@Param("medicalRecordId") UUID medicalRecordId);

    @Modifying
    @Query("delete from ClinicalOrderEntity order where order.medicalRecordId = :medicalRecordId")
    void deleteByMedicalRecordId(@Param("medicalRecordId") UUID medicalRecordId);
}
