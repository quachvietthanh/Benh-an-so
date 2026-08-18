package com.benhsoan.persistence.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@JdbcTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never"
})
class PaymentRefundMigrationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Test
    void addsRefundMetadataColumnsAndIndex() {
        jdbcTemplate.execute("create table users (id BINARY(16) primary key)");
        jdbcTemplate.execute("create table payments (id BINARY(16) primary key)");
        new ResourceDatabasePopulator(
                new ClassPathResource(
                        "db/migration/V32__add_payment_refund_metadata.sql"
                )
        ).execute(dataSource);

        List<String> columns = jdbcTemplate.queryForList(
                """
                select column_name
                from information_schema.columns
                where table_name = 'PAYMENTS'
                  and column_name in ('REFUND_REASON', 'REFUNDED_BY', 'REFUNDED_AT')
                """,
                String.class
        );
        Integer indexCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.indexes
                where table_name = 'PAYMENTS'
                  and index_name = 'IDX_PAYMENTS_REFUNDED_AT'
                """,
                Integer.class
        );

        assertEquals(3, columns.size());
        assertEquals(1, indexCount);
    }
}
