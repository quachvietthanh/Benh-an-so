package com.benhsoan.persistence.jpaRepository.clinical;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.clinical.MedicalAttachmentEntity;

public interface JpaMedicalAttachmentRepository extends JpaRepository<MedicalAttachmentEntity, UUID> {
    boolean existsByClinicalResultId(UUID clinicalResultId);
    List<MedicalAttachmentEntity> findByClinicalResultIdOrderByUploadedAtDesc(UUID clinicalResultId);
    List<MedicalAttachmentEntity> findByClinicalResultIdIn(Collection<UUID> clinicalResultIds);

    @Modifying
    @Query("delete from MedicalAttachmentEntity attachment where attachment.medicalRecordId = :medicalRecordId")
    void deleteByMedicalRecordId(@Param("medicalRecordId") UUID medicalRecordId);

    @Modifying
    @Query("delete from MedicalAttachmentEntity attachment where attachment.clinicalResultId in :resultIds")
    void deleteByClinicalResultIdIn(@Param("resultIds") Collection<UUID> resultIds);
}
