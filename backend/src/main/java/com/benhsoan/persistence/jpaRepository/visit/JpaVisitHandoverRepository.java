package com.benhsoan.persistence.jpaRepository.visit;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.visit.VisitHandoverEntity;

public interface JpaVisitHandoverRepository extends JpaRepository<VisitHandoverEntity, UUID> {

    List<VisitHandoverEntity> findByVisitIdOrderByHandedOverAtAsc(UUID visitId);

    List<VisitHandoverEntity> findByVisitIdOrderByHandedOverAtDesc(UUID visitId);
}
