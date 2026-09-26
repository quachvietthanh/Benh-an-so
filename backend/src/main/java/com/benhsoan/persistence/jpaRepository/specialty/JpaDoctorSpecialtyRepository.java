package com.benhsoan.persistence.jpaRepository.specialty;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.specialty.DoctorSpecialtyEntity;

public interface JpaDoctorSpecialtyRepository extends JpaRepository<DoctorSpecialtyEntity, UUID> {

    List<DoctorSpecialtyEntity> findBySpecialtyId(UUID specialtyId);

    List<DoctorSpecialtyEntity> findByDoctorId(UUID doctorId);

    long countBySpecialtyId(UUID specialtyId);

    void deleteBySpecialtyId(UUID specialtyId);

    @Query("SELECT ds.doctorId FROM DoctorSpecialtyEntity ds WHERE ds.specialtyId = :specialtyId")
    List<UUID> findDoctorIdsBySpecialtyId(@Param("specialtyId") UUID specialtyId);

    @Query("SELECT ds.specialtyId FROM DoctorSpecialtyEntity ds WHERE ds.doctorId = :doctorId")
    List<UUID> findSpecialtyIdsByDoctorId(@Param("doctorId") UUID doctorId);
}
