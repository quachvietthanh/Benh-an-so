package com.benhsoan.persistence.jpaRepository.prescription;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.prescription.PrescriptionEntity;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.prescription.enums.InterconnectionStatus;

import jakarta.persistence.LockModeType;

public interface JpaPrescriptionRepository
        extends JpaRepository<PrescriptionEntity, UUID> {

    Optional<PrescriptionEntity> findByPrescriptionCode(String prescriptionCode);

    boolean existsByPrescriptionCode(String prescriptionCode);

    Optional<PrescriptionEntity> findTopByOrderByPrescriptionCodeDesc();

    List<PrescriptionEntity> findByMedicalRecordIdOrderByPrescribedAtDesc(
            UUID medicalRecordId
    );

    Page<PrescriptionEntity> findByStatus(
            PrescriptionStatus status,
            Pageable pageable
    );

    @Query("""
            select prescription from PrescriptionEntity prescription
            where prescription.interconnectionStatus = :status
              and (:fromInclusive is null or prescription.lastInterconnectionAt >= :fromInclusive)
              and (:toExclusive is null or prescription.lastInterconnectionAt < :toExclusive)
            """)
    Page<PrescriptionEntity> findByInterconnectionStatus(
            @Param("status") InterconnectionStatus status,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select prescription from PrescriptionEntity prescription "
            + "where prescription.id = :id")
    Optional<PrescriptionEntity> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select prescription from PrescriptionEntity prescription "
            + "where prescription.medicalRecordId = :medicalRecordId "
            + "and prescription.status = :status "
            + "order by prescription.prescribedAt")
    List<PrescriptionEntity> findByMedicalRecordIdAndStatusForUpdate(
            @Param("medicalRecordId") UUID medicalRecordId,
            @Param("status") PrescriptionStatus status);

    @Query("select prescription.id from PrescriptionEntity prescription "
            + "where prescription.medicalRecordId = :medicalRecordId")
    List<UUID> findIdsByMedicalRecordId(@Param("medicalRecordId") UUID medicalRecordId);

    @Query("""
            select new com.benhsoan.persistence.jpaRepository.prescription.PrescriptionCountProjection(
                prescription.medicalRecordId, count(prescription)
            )
            from PrescriptionEntity prescription
            where prescription.medicalRecordId in :medicalRecordIds
            group by prescription.medicalRecordId
            """)
    List<PrescriptionCountProjection> countByMedicalRecordIdIn(
            @Param("medicalRecordIds") java.util.Collection<UUID> medicalRecordIds
    );

    @Modifying
    @Query("delete from PrescriptionEntity prescription where prescription.medicalRecordId = :medicalRecordId")
    void deleteByMedicalRecordId(@Param("medicalRecordId") UUID medicalRecordId);

    /**
     * NCL-12-CN-007 CV-02 reconciliation projection.
     *
     * Period semantics (half open [from, to)): a prescription belongs to the period when
     * prescribedAt, lastInterconnectionAt, or any dispense item dispensedAt falls inside the
     * interval. Filtering on lastInterconnectionAt alone would hide prescriptions that were
     * dispensed but never successfully transmitted, which is one of the two discrepancies the
     * workbook requires.
     *
     * The patient/doctor display context is resolved with left joins inside the same query so
     * the reconciliation list does not issue one lookup per row. Ordering is explicit and
     * deterministic; callers pass an unsorted Pageable on purpose.
     *
     * The predicate block is duplicated in {@link #countByReconciliationFilter} so the paged
     * count matches the rows; both must be kept in sync.
     */
    @Query("""
            select new com.benhsoan.persistence.jpaRepository.prescription.PrescriptionReconciliationProjection(
                prescription.id, prescription.prescriptionCode, prescription.medicalRecordId,
                prescription.status, prescription.interconnectionStatus,
                prescription.prescribedAt, prescription.lastInterconnectionAt,
                prescription.lastInterconnectionError, prescription.interconnectionReceiptCode,
                patient.id, patient.patientCode, patient.fullName,
                prescription.prescribedBy, doctor.fullName
            )
            from PrescriptionEntity prescription
            left join MedicalRecordEntity medicalRecord on medicalRecord.id = prescription.medicalRecordId
            left join VisitEntity visit on visit.id = medicalRecord.visitId
            left join PatientEntity patient on patient.id = visit.patientId
            left join UserEntity doctor on doctor.id = prescription.prescribedBy
            where (:prescriptionCode is null or prescription.prescriptionCode = :prescriptionCode)
              and (
                    :periodUnbounded = true
                    or (
                        (:fromInclusive is null or prescription.prescribedAt >= :fromInclusive)
                        and (:toExclusive is null or prescription.prescribedAt < :toExclusive)
                    )
                    or (
                        (:fromInclusive is null or prescription.lastInterconnectionAt >= :fromInclusive)
                        and (:toExclusive is null or prescription.lastInterconnectionAt < :toExclusive)
                    )
                    or exists (
                        select dispense.id from PrescriptionDispenseItemEntity dispense
                        where dispense.prescriptionId = prescription.id
                          and (:fromInclusive is null or dispense.dispensedAt >= :fromInclusive)
                          and (:toExclusive is null or dispense.dispensedAt < :toExclusive)
                    )
              )
              and (
                    :filterByOutcome = false
                    or (prescription.status in :group1DispensingStatuses
                        and prescription.interconnectionStatus in :group1InterconnectionStatuses)
                    or (:group2Active = true
                        and prescription.status in :group2DispensingStatuses
                        and prescription.interconnectionStatus in :group2InterconnectionStatuses)
              )
            order by prescription.prescribedAt desc, prescription.id desc
            """)
    Page<PrescriptionReconciliationProjection> findByReconciliationFilter(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("periodUnbounded") boolean periodUnbounded,
            @Param("prescriptionCode") String prescriptionCode,
            @Param("filterByOutcome") boolean filterByOutcome,
            @Param("group1DispensingStatuses") Collection<PrescriptionStatus> group1DispensingStatuses,
            @Param("group1InterconnectionStatuses") Collection<InterconnectionStatus> group1InterconnectionStatuses,
            @Param("group2Active") boolean group2Active,
            @Param("group2DispensingStatuses") Collection<PrescriptionStatus> group2DispensingStatuses,
            @Param("group2InterconnectionStatuses") Collection<InterconnectionStatus> group2InterconnectionStatuses,
            Pageable pageable);

    /**
     * Paged count for {@link #findByReconciliationFilter}. The predicate block must stay
     * identical to the select query; the left joins are omitted because a left join cannot
     * remove rows, so the counted set is unchanged.
     */
    @Query("""
            select count(prescription)
            from PrescriptionEntity prescription
            where (:prescriptionCode is null or prescription.prescriptionCode = :prescriptionCode)
              and (
                    :periodUnbounded = true
                    or (
                        (:fromInclusive is null or prescription.prescribedAt >= :fromInclusive)
                        and (:toExclusive is null or prescription.prescribedAt < :toExclusive)
                    )
                    or (
                        (:fromInclusive is null or prescription.lastInterconnectionAt >= :fromInclusive)
                        and (:toExclusive is null or prescription.lastInterconnectionAt < :toExclusive)
                    )
                    or exists (
                        select dispense.id from PrescriptionDispenseItemEntity dispense
                        where dispense.prescriptionId = prescription.id
                          and (:fromInclusive is null or dispense.dispensedAt >= :fromInclusive)
                          and (:toExclusive is null or dispense.dispensedAt < :toExclusive)
                    )
              )
              and (
                    :filterByOutcome = false
                    or (prescription.status in :group1DispensingStatuses
                        and prescription.interconnectionStatus in :group1InterconnectionStatuses)
                    or (:group2Active = true
                        and prescription.status in :group2DispensingStatuses
                        and prescription.interconnectionStatus in :group2InterconnectionStatuses)
              )
            """)
    long countByReconciliationFilter(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("periodUnbounded") boolean periodUnbounded,
            @Param("prescriptionCode") String prescriptionCode,
            @Param("filterByOutcome") boolean filterByOutcome,
            @Param("group1DispensingStatuses") Collection<PrescriptionStatus> group1DispensingStatuses,
            @Param("group1InterconnectionStatuses") Collection<InterconnectionStatus> group1InterconnectionStatuses,
            @Param("group2Active") boolean group2Active,
            @Param("group2DispensingStatuses") Collection<PrescriptionStatus> group2DispensingStatuses,
            @Param("group2InterconnectionStatuses") Collection<InterconnectionStatus> group2InterconnectionStatuses);
}
