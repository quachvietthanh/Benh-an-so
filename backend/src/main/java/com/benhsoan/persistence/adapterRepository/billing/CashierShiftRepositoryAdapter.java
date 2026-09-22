package com.benhsoan.persistence.adapterRepository.billing;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.benhsoan.domain.billing.CashierShift;
import com.benhsoan.domain.billing.enums.CashierShiftStatus;
import com.benhsoan.persistence.entity.billing.CashierShiftEntity;
import com.benhsoan.persistence.jpaRepository.billing.JpaCashierShiftRepository;
import com.benhsoan.persistence.mapper.billing.CashierShiftPersistenceMapper;
import com.benhsoan.port.outbound.repository.billing.CashierShiftRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CashierShiftRepositoryAdapter implements CashierShiftRepository {

    private final JpaCashierShiftRepository jpaRepository;
    private final CashierShiftPersistenceMapper mapper;

    @Override
    public CashierShift save(CashierShift shift) {
        CashierShiftEntity entity = mapper.toEntity(shift);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<CashierShift> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<CashierShift> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public Optional<CashierShift> findByShiftCode(String shiftCode) {
        return jpaRepository.findByShiftCode(shiftCode).map(mapper::toDomain);
    }

    @Override
    public Page<CashierShift> search(
            UUID cashierId,
            CashierShiftStatus status,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        return jpaRepository.search(cashierId, status, from, to, pageable)
                .map(mapper::toDomain);
    }
}
