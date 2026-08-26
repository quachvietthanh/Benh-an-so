package com.benhsoan.persistence.jpaRepository.medicalrecord;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordTemplateEntity;

import jakarta.persistence.LockModeType;

public interface JpaMedicalRecordTemplateRepository extends JpaRepository<MedicalRecordTemplateEntity, UUID> {

    List<MedicalRecordTemplateEntity> findBySpecialtyIdAndActiveOrderByNameAsc(UUID specialtyId, boolean active);

    boolean existsBySpecialtyIdAndNameKey(UUID specialtyId, String nameKey);

    boolean existsBySpecialtyIdAndNameKeyAndIdNot(UUID specialtyId, String nameKey, UUID id);

    @Query("""
            select template
            from MedicalRecordTemplateEntity template
            where (:specialtyId is null or template.specialtyId = :specialtyId)
              and (:active is null or template.active = :active)
            order by template.specialtyId asc, template.defaultTemplate desc, template.name asc
            """)
    List<MedicalRecordTemplateEntity> search(@Param("specialtyId") UUID specialtyId, @Param("active") Boolean active);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select template from MedicalRecordTemplateEntity template where template.id = :id")
    Optional<MedicalRecordTemplateEntity> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select template
            from MedicalRecordTemplateEntity template
            where template.specialtyId = :specialtyId and template.active = true
            order by template.id
            """)
    List<MedicalRecordTemplateEntity> findActiveBySpecialtyIdForUpdate(@Param("specialtyId") UUID specialtyId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update MedicalRecordTemplateEntity template
            set template.defaultTemplate = false, template.updatedBy = :updatedBy, template.updatedAt = :updatedAt
            where template.specialtyId = :specialtyId and template.defaultTemplate = true
            """)
    int clearDefaultBySpecialtyId(@Param("specialtyId") UUID specialtyId, @Param("updatedBy") UUID updatedBy,
            @Param("updatedAt") Instant updatedAt);
}
