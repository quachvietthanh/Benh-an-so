package com.benhsoan.persistence.adapterRepository.billing;

import org.springframework.stereotype.Repository;

import com.benhsoan.port.outbound.repository.billing.InvoiceCodeSequenceRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class InvoiceCodeSequenceRepositoryAdapter
        implements InvoiceCodeSequenceRepository {

    private final EntityManager entityManager;

    @Override
    public long reserveNextValue(String prefix) {
        entityManager.createNativeQuery("""
                INSERT INTO invoice_code_sequences (code_prefix, `last_value`)
                VALUES (:prefix, LAST_INSERT_ID(1))
                ON DUPLICATE KEY UPDATE `last_value` = LAST_INSERT_ID(`last_value` + 1)
                """)
                .setParameter("prefix", prefix)
                .executeUpdate();

        Number value = (Number) entityManager
                .createNativeQuery("SELECT LAST_INSERT_ID()")
                .getSingleResult();
        return value.longValue();
    }
}
