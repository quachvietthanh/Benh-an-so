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

    private static final List<String> OPERATIONAL_TABLES = List.of(
            "rooms",
            "medicines",
            "patients",
            "appointments",
            "appointment_notification_logs",
            "medical_queues",
            "doctor_room_assignments",
            "queue_items",
            "visits",
            "medical_records",
            "medical_record_diagnoses",
            "medical_record_amendments",
            "medical_record_access_logs",
            "clinical_orders",
            "clinical_order_items",
            "clinical_results",
            "clinical_result_histories",
            "medical_attachments",
            "prescriptions",
            "prescription_items",
            "medicine_batches",
            "prescription_dispense_items",
            "prescription_amendments",
            "prescription_warning_logs",
            "inventory_receipts",
            "inventory_receipt_items",
            "stock_movements",
            "inventory_alert_logs",
            "invoices",
            "invoice_lines",
            "payments",
            "patient_change_logs"
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
