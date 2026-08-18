package com.benhsoan.persistence.adapterRepository.servicecatalog;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.benhsoan.domain.servicecatalog.ServiceCatalog;
import com.benhsoan.persistence.jpaRepository.servicecatalog.JpaServiceCatalogRepository;
import com.benhsoan.persistence.mapper.servicecatalog.ServiceCatalogPersistenceMapper;
import com.benhsoan.port.outbound.repository.servicecatalog.ServiceCatalogRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ServiceCatalogRepositoryAdapter implements ServiceCatalogRepository {

    private final JpaServiceCatalogRepository jpaRepository;
    private final ServiceCatalogPersistenceMapper mapper;

    @Override
    public ServiceCatalog save(ServiceCatalog serviceCatalog) {
        Objects.requireNonNull(serviceCatalog, "Service catalog must not be null.");
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(serviceCatalog)));
    }

    @Override
    public Optional<ServiceCatalog> findById(UUID id) {
        Objects.requireNonNull(id, "Service catalog id must not be null.");
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ServiceCatalog> findByServiceCode(String serviceCode) {
        return jpaRepository.findByServiceCodeIgnoreCase(requireText(serviceCode, "Service code must not be blank."))
                .map(mapper::toDomain);
    }

    @Override
    public List<ServiceCatalog> findAll() {
        return jpaRepository.findAllByOrderByServiceNameAscServiceCodeAsc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<ServiceCatalog> search(String keyword, Boolean active, Pageable pageable) {
        Objects.requireNonNull(pageable, "Service catalog pageable must not be null.");
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        return jpaRepository.search(normalizedKeyword, active, pageable).map(mapper::toDomain);
    }

    @Override
    public boolean existsByServiceCode(String serviceCode) {
        return jpaRepository.existsByServiceCodeIgnoreCase(
                requireText(serviceCode, "Service code must not be blank.")
        );
    }

    @Override
    public boolean existsByNormalizedServiceName(String normalizedServiceName, UUID excludedId) {
        String normalized = requireText(normalizedServiceName, "Normalized service name must not be blank.")
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
        return jpaRepository.existsByNormalizedServiceName(normalized, excludedId);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
