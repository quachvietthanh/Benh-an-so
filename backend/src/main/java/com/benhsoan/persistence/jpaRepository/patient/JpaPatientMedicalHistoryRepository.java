package com.benhsoan.persistence.jpaRepository.patient;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.visit.VisitEntity;

public interface JpaPatientMedicalHistoryRepository extends Repository<VisitEntity, UUID> {

    @Query(
            value = """
                    select new com.benhsoan.persistence.jpaRepository.patient.PatientMedicalHistoryProjection(
                        visit.id, visit.visitCode, visit.visitType, visit.status, visit.visitAt,
                        visit.startedAt, visit.completedAt, visit.reason, visit.note,
                        doctor.id, doctor.fullName, medicalRecord.id, medicalRecord.status,
                        medicalRecord.chiefComplaint, medicalRecord.conclusion
                    )
                    from VisitEntity visit
                    left join MedicalRecordEntity medicalRecord on medicalRecord.visitId = visit.id
                    left join UserEntity doctor on doctor.id = visit.doctorId
                    where visit.patientId = :patientId
                      and (:fromTime is null or visit.visitAt >= :fromTime)
                      and (:toTime is null or visit.visitAt <= :toTime)
                    order by visit.visitAt desc
                    """,
            countQuery = """
                    select count(visit.id)
                    from VisitEntity visit
                    where visit.patientId = :patientId
                      and (:fromTime is null or visit.visitAt >= :fromTime)
                      and (:toTime is null or visit.visitAt <= :toTime)
                    """
    )
    Page<PatientMedicalHistoryProjection> findMedicalHistory(
            @Param("patientId") UUID patientId,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime,
            Pageable pageable
    );
}
