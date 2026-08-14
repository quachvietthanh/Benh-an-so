package com.benhsoan.config;

import java.nio.file.Path;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.benhsoan.infrastructure.storage.JsonDatabaseBackupStorageAdapter;
import com.benhsoan.port.outbound.backup.DatabaseBackupStoragePort;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class BackupStorageConfiguration {

    /**
     * Operational tables included in a backup, ordered parents-first so that a
     * restore can re-insert them in this order and delete in reverse order.
     */
    private static final List<String> OPERATIONAL_TABLES = List.of(
            "patients",
            "appointments",
            "appointment_notification_logs",
            "visits",
            "medical_records",
            "medical_record_diagnoses",
            "medical_record_amendments",
            "medical_record_access_logs",
            "medical_attachments",
            "clinical_orders",
            "clinical_order_items",
            "clinical_results",
            "clinical_result_histories",
            "prescriptions",
            "prescription_items",
            "prescription_dispense_items",
            "prescription_amendments",
            "prescription_warning_logs",
            "inventory_receipts",
            "inventory_receipt_items",
            "medicine_batches",
            "stock_movements",
            "inventory_alert_logs",
            "payments",
            "invoices",
            "invoice_lines"
    );

    @Bean
    public DatabaseBackupStoragePort databaseBackupStoragePort(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        return new JsonDatabaseBackupStorageAdapter(
                jdbcTemplate,
                objectMapper,
                OPERATIONAL_TABLES,
                Path.of("backups")
        );
    }
}
