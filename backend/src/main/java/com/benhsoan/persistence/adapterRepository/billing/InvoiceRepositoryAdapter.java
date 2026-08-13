package com.benhsoan.persistence.adapterRepository.billing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.persistence.entity.billing.InvoiceEntity;
import com.benhsoan.persistence.entity.billing.InvoiceLineEntity;
import com.benhsoan.persistence.jpaRepository.billing.JpaInvoiceLineRepository;
import com.benhsoan.persistence.jpaRepository.billing.JpaInvoiceRepository;
import com.benhsoan.persistence.jpaRepository.billing.PayableEncounterProjection;
import com.benhsoan.persistence.mapper.billing.InvoiceLinePersistenceMapper;
import com.benhsoan.persistence.mapper.billing.InvoicePersistenceMapper;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.repository.billing.InvoiceSearchCriteria;
import com.benhsoan.port.outbound.repository.billing.PayableEncounterSummary;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class InvoiceRepositoryAdapter implements InvoiceRepository {

    private final JpaInvoiceRepository jpaRepository;

    private final JpaInvoiceLineRepository lineJpaRepository;

    private final InvoicePersistenceMapper mapper;

    private final InvoiceLinePersistenceMapper lineMapper;

    @Override
    @Transactional
    public Invoice save(Invoice invoice) {
        InvoiceEntity savedEntity = jpaRepository.save(mapper.toEntity(invoice));

        lineJpaRepository.deleteAllByInvoiceId(savedEntity.getId());

        List<InvoiceLineEntity> lineEntities = invoice.getLines()
                .stream()
                .map(lineMapper::toEntity)
                .toList();

        List<InvoiceLineEntity> savedLineEntities = lineJpaRepository.saveAll(lineEntities);

        return mapper.toDomain(savedEntity, savedLineEntities);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Invoice> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Invoice> findOriginalByVisitId(UUID visitId) {
        return jpaRepository.findByVisitIdAndType(visitId, InvoiceType.ORIGINAL)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Invoice> findByPaymentId(UUID paymentId) {
        return jpaRepository.findByPaymentId(paymentId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Invoice> findCreatedBetween(Instant fromInclusive, Instant toExclusive) {
        return jpaRepository.findCreatedBetween(fromInclusive, toExclusive).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayableEncounterSummary> findPayableEncounters(Pageable pageable) {
        return jpaRepository.findPayableEncounters(pageable)
                .map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Invoice> search(
            InvoiceSearchCriteria criteria,
            Pageable pageable
    ) {
        return jpaRepository.search(
                        criteria.invoiceCode(),
                        criteria.invoiceType(),
                        criteria.visitId(),
                        criteria.createdFrom(),
                        criteria.createdTo(),
                        pageable
                )
                .map(this::toDomain);
    }

    private Invoice toDomain(InvoiceEntity entity) {
        List<InvoiceLineEntity> lineEntities = lineJpaRepository
                .findByInvoiceIdOrderByCreatedAtAsc(entity.getId());

        return mapper.toDomain(entity, lineEntities);
    }

    private PayableEncounterSummary toSummary(PayableEncounterProjection projection) {
        return new PayableEncounterSummary(
                projection.getVisitId(),
                projection.getVisitCode(),
                projection.getPatientId(),
                projection.getPatientCode(),
                projection.getPatientName(),
                projection.getReason(),
                projection.getCompletedAt()
        );
    }
}
