package com.benhsoan.persistence.adapterRepository.servicecatalog;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.servicecatalog.ServicePrice;
import com.benhsoan.persistence.jpaRepository.servicecatalog.JpaServicePriceRepository;
import com.benhsoan.persistence.mapper.servicecatalog.ServicePricePersistenceMapper;
import com.benhsoan.port.outbound.repository.servicecatalog.ServicePriceRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ServicePriceRepositoryAdapter implements ServicePriceRepository {

    private final JpaServicePriceRepository jpaRepository;
    private final ServicePricePersistenceMapper mapper;

    @Override
    public ServicePrice save(ServicePrice servicePrice) {
        Objects.requireNonNull(servicePrice, "Service price must not be null.");
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(servicePrice)));
    }

    @Override
    public Optional<ServicePrice> findById(UUID id) {
        Objects.requireNonNull(id, "Service price id must not be null.");
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ServicePrice> findAllByServiceCatalogId(UUID serviceCatalogId) {
        Objects.requireNonNull(serviceCatalogId, "Service catalog id must not be null.");
        return jpaRepository.findAllByServiceCatalogIdOrderByEffectiveFromDesc(serviceCatalogId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ServicePrice> findEffectivePrice(UUID serviceCatalogId, LocalDate effectiveOn) {
        Objects.requireNonNull(serviceCatalogId, "Service catalog id must not be null.");
        Objects.requireNonNull(effectiveOn, "Effective date must not be null.");
        return jpaRepository
                .findFirstByServiceCatalogIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        serviceCatalogId,
                        effectiveOn
                )
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByServiceCatalogIdAndEffectiveFrom(UUID serviceCatalogId, LocalDate effectiveFrom) {
        Objects.requireNonNull(serviceCatalogId, "Service catalog id must not be null.");
        Objects.requireNonNull(effectiveFrom, "Effective date must not be null.");
        return jpaRepository.existsByServiceCatalogIdAndEffectiveFrom(serviceCatalogId, effectiveFrom);
    }
}
