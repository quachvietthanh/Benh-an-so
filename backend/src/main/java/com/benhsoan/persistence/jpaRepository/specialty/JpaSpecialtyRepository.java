package com.benhsoan.persistence.jpaRepository.specialty;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.specialty.SpecialtyEntity;

public interface JpaSpecialtyRepository extends JpaRepository<SpecialtyEntity, UUID> {

    List<SpecialtyEntity> findByActiveOrderByCodeAsc(boolean active);

    List<SpecialtyEntity> findAllByOrderByCodeAsc();

    Optional<SpecialtyEntity> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByNameKey(String nameKey);

    boolean existsByNameKeyAndIdNot(String nameKey, UUID id);

    @Query("""
            SELECT s FROM SpecialtyEntity s
            WHERE (:active IS NULL OR s.active = :active)
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY s.code ASC
            """)
    List<SpecialtyEntity> search(@Param("keyword") String keyword, @Param("active") Boolean active);
}
