package com.benhsoan.persistence.jpaRepository.specialty;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.specialty.SpecialtyEntity;

public interface JpaSpecialtyRepository extends JpaRepository<SpecialtyEntity, UUID> {

    List<SpecialtyEntity> findByActiveOrderByCodeAsc(boolean active);
}
