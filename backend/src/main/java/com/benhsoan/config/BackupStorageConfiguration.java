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
     * Operational tables included in a full backup, in PARENT-FIRST order.
     *
     * The restore engine empties every table in the reverse (CHILD-FIRST) order
     * and then re-inserts rows in this PARENT-FIRST order, so foreign keys are
     * never violated.
     *
     * Reference/auth tables (users, roles, role_permissions, user_sessions,
     * audit_logs) and read-only catalogs / code sequences
     * (clinical_service_catalog, diagnosis_catalog, drug_interaction_rules,
     * invoice_code_sequences, prescription_code_sequences) are intentionally
     * NOT included, so seeded system data is never deleted or restored.
     *
     * Note: visits <-> queue_items form a circular foreign key
     * (visits.queue_item_id is nullable while queue_items.visit_id is not). This
     * engine handles the general acyclic case; the circular pair is documented
     * as a known limitation for a future null-out-first enhancement.
     */
    private static final List<String> OPERATIONAL_TABLES = List.of(
            // Master / catalog
            "rooms",
            "medicines",
            // Patients & appointments
            "patients",
            "appointments",
            "appointment_notification_logs",
            // Medical queue
            "medical_queues",
            "doctor_room_assignments",
            "queue_items",
            "visits",
            // Medical records
            "medical_records",
            "medical_record_diagnoses",
            "medical_record_amendments",
            "medical_record_access_logs",
            // Clinical orders & results
            "clinical_orders",
            "clinical_order_items",
            "clinical_results",
            "clinical_result_histories",
            "medical_attachments",
            // Prescriptions & dispensing
            "prescriptions",
            "prescription_items",
            "medicine_batches",
            "prescription_dispense_items",
            "prescription_amendments",
            "prescription_warning_logs",
            // Inventory
            "inventory_receipts",
            "inventory_receipt_items",
            "stock_movements",
            "inventory_alert_logs",
            // Billing
            "invoices",
            "invoice_lines",
            "payments",
            // Patient audit trail
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
