package com.benhsoan.persistence.jpaRepository.clinic;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.clinic.ClinicConfigurationEntity;

public interface JpaClinicConfigurationRepository extends JpaRepository<ClinicConfigurationEntity, Integer> {
}
