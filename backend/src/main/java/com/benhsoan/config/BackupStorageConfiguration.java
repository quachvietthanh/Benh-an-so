package com.benhsoan.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.benhsoan.infrastructure.storage.JsonDatabaseBackupStorageAdapter;
import com.benhsoan.infrastructure.storage.BackupRestorePlan;
import com.benhsoan.infrastructure.storage.BackupRestorePlan.DeferredForeignKey;
import com.benhsoan.infrastructure.storage.BackupRestorePlan.ForeignKeyDependency;
import com.benhsoan.port.outbound.backup.DatabaseBackupStoragePort;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class BackupStorageConfiguration {

    @Bean
    public BackupRestorePlan fullBackupRestorePlan() {
        return new BackupRestorePlan(
                List.of(
                        "clinic_configuration", "rooms", "medicines", "diagnosis_catalog", "service_catalog", "service_price",
                        "clinical_service_catalog",
                        "drug_interaction_rules", "prescription_code_sequences", "invoice_code_sequences",
                        "patients", "patient_change_logs", "appointments", "appointment_notification_logs",
                        "doctor_room_assignments", "medical_queues", "queue_items", "visits",
                        "follow_up_reminders",
                        "medical_records", "medical_record_diagnoses", "medical_record_amendments",
                        "medical_record_access_logs", "clinical_orders", "clinical_order_items",
                        "clinical_results", "clinical_result_histories", "medical_attachments",
                        "prescriptions", "prescription_items", "prescription_dispense_items",
                        "prescription_amendments", "prescription_warning_logs", "medicine_batches",
                        "inventory_receipts", "inventory_receipt_items", "stock_movements",
                        "inventory_alert_logs", "payments", "payment_service_fees", "invoices", "invoice_lines"
                ),
                Set.of("users"),
                List.of(
                        dependency("patient_change_logs", "patient_id", "patients"),
                        dependency("appointments", "patient_id", "patients"),
                        dependency("appointment_notification_logs", "appointment_id", "appointments"),
                        dependency("appointment_notification_logs", "patient_id", "patients"),
                        dependency("doctor_room_assignments", "room_id", "rooms"),
                        dependency("medical_queues", "room_id", "rooms"),
                        dependency("queue_items", "medical_queue_id", "medical_queues"),
                        dependency("queue_items", "patient_id", "patients"),
                        dependency("queue_items", "appointment_id", "appointments"),
                        dependency("queue_items", "visit_id", "visits"),
                        dependency("visits", "patient_id", "patients"),
                        dependency("visits", "appointment_id", "appointments"),
                        dependency("visits", "queue_item_id", "queue_items"),
                        dependency("follow_up_reminders", "patient_id", "patients"),
                        dependency("follow_up_reminders", "visit_id", "visits"),
                        dependency("follow_up_reminders", "appointment_id", "appointments"),
                        dependency("medical_records", "visit_id", "visits"),
                        dependency("medical_record_diagnoses", "medical_record_id", "medical_records"),
                        dependency("medical_record_diagnoses", "diagnosis_catalog_id", "diagnosis_catalog"),
                        dependency("medical_record_amendments", "medical_record_id", "medical_records"),
                        dependency("medical_record_access_logs", "patient_id", "patients"),
                        dependency("medical_record_access_logs", "visit_id", "visits"),
                        dependency("medical_record_access_logs", "medical_record_id", "medical_records"),
                        dependency("clinical_orders", "visit_id", "visits"),
                        dependency("clinical_orders", "medical_record_id", "medical_records"),
                        dependency("clinical_orders", "patient_id", "patients"),
                        dependency("service_price", "service_catalog_id", "service_catalog"),
                        dependency("clinical_service_catalog", "service_catalog_id", "service_catalog"),
                        dependency("clinical_order_items", "clinical_order_id", "clinical_orders"),
                        dependency("clinical_order_items", "clinical_service_id", "clinical_service_catalog"),
                        dependency("clinical_results", "clinical_order_item_id", "clinical_order_items"),
                        dependency("clinical_results", "visit_id", "visits"),
                        dependency("clinical_result_histories", "clinical_result_id", "clinical_results"),
                        dependency("medical_attachments", "visit_id", "visits"),
                        dependency("medical_attachments", "medical_record_id", "medical_records"),
                        dependency("medical_attachments", "clinical_result_id", "clinical_results"),
                        dependency("prescriptions", "medical_record_id", "medical_records"),
                        dependency("prescription_items", "prescription_id", "prescriptions"),
                        dependency("prescription_items", "medicine_id", "medicines"),
                        dependency("prescription_dispense_items", "prescription_id", "prescriptions"),
                        dependency("prescription_dispense_items", "prescription_item_id", "prescription_items"),
                        dependency("prescription_dispense_items", "medicine_id", "medicines"),
                        dependency("prescription_dispense_items", "medicine_batch_id", "medicine_batches"),
                        dependency("prescription_amendments", "prescription_id", "prescriptions"),
                        dependency("prescription_warning_logs", "prescription_id", "prescriptions"),
                        dependency("prescription_warning_logs", "rule_id", "drug_interaction_rules"),
                        dependency("prescription_warning_logs", "first_medicine_id", "medicines"),
                        dependency("prescription_warning_logs", "second_medicine_id", "medicines"),
                        dependency("medicine_batches", "medicine_id", "medicines"),
                        dependency("inventory_receipt_items", "inventory_receipt_id", "inventory_receipts"),
                        dependency("inventory_receipt_items", "medicine_id", "medicines"),
                        dependency("inventory_receipt_items", "medicine_batch_id", "medicine_batches"),
                        dependency("stock_movements", "medicine_id", "medicines"),
                        dependency("stock_movements", "medicine_batch_id", "medicine_batches"),
                        dependency("inventory_alert_logs", "medicine_id", "medicines"),
                        dependency("payments", "visit_id", "visits"),
                        dependency("payment_service_fees", "payment_id", "payments"),
                        dependency("payment_service_fees", "clinical_order_item_id", "clinical_order_items"),
                        dependency("invoices", "visit_id", "visits"),
                        dependency("invoices", "payment_id", "payments"),
                        dependency("invoices", "original_invoice_id", "invoices"),
                        dependency("invoice_lines", "invoice_id", "invoices")
                ),
                Set.of(
                        new DeferredForeignKey("visits", "queue_item_id"),
                        new DeferredForeignKey("invoices", "original_invoice_id")
                )
        );
    }

    private static ForeignKeyDependency dependency(String childTable, String childColumn, String parentTable) {
        return new ForeignKeyDependency(childTable, childColumn, parentTable);
    }

    @Bean
    public DatabaseBackupStoragePort databaseBackupStoragePort(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            BackupRestorePlan fullBackupRestorePlan
    ) {
        return new JsonDatabaseBackupStorageAdapter(
                jdbcTemplate,
                objectMapper,
                fullBackupRestorePlan,
                Path.of("backups")
        );
    }
}
