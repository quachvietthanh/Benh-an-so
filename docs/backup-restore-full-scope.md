# NCL-09-CN-005: Full Backup Scope Decision

## Status

Proposed for BA/PO approval. This document is the scope contract for the
backup/restore implementation. `BackupRestorePlan` is the technical source of
truth for this scope and the adapter validates it against database metadata at
restore time.

## Business Intent

The Excel backlog for `NCL-09-CN-005` requires an administrator to back up and
restore system data to the selected point in time. The acceptance criteria also
require that only successful backups can be restored, only administrators can
perform the operation, and backup/restore actions are auditable.

For this application, `FULL` means a full **operational logical backup**. It
restores all clinical, patient, catalog, pharmacy, inventory, billing, and
their business history needed to resume clinic operations at the snapshot time.

It is intentionally not a disaster-recovery image for an empty database. The
running environment retains its authentication and backup control-plane data.
This keeps the administrator identity, active access controls, and the audit
trail for the restore operation available while data is restored.

## Included Tables

All tables below are part of every `FULL` snapshot. Code-sequence tables are
included even though they have no foreign keys: omitting them can cause a new
prescription or invoice code to collide with restored data.

| Group | Tables |
| --- | --- |
| Operational reference data | `clinic_configuration`, `rooms`, `medicines`, `diagnosis_catalog`, `specialties`, `medical_record_templates`, `medical_record_template_versions`, `medical_record_template_sections`, `clinical_service_catalog`, `drug_interaction_rules`, `prescription_code_sequences`, `invoice_code_sequences` |
| Patient and appointment | `patients`, `patient_change_logs`, `appointments`, `appointment_notification_logs` |
| Queue and visit | `doctor_room_assignments`, `medical_queues`, `queue_items`, `visits` (including `specialty_id`) |
| Medical record | `medical_records` (including `applied_template_version_id`), `medical_record_diagnoses`, `medical_record_amendments`, `medical_record_access_logs` |
| Clinical workflow | `clinical_orders`, `clinical_order_items`, `clinical_results`, `clinical_result_histories`, `medical_attachments` |
| Prescription and dispensing | `prescriptions`, `prescription_items`, `prescription_dispense_items`, `prescription_amendments`, `prescription_warning_logs` |
| Inventory | `medicine_batches`, `inventory_receipts`, `inventory_receipt_items`, `stock_movements`, `inventory_alert_logs` |
| Billing | `payments`, `invoices`, `invoice_lines` |

## Explicit Exclusions

| Table | Decision and reason |
| --- | --- |
| `roles`, `role_permissions`, `users` | Preserve the current environment's identities and permissions. Operational records keep their existing user foreign-key references, so restore is supported only into the same initialized system, not a blank database. |
| `user_sessions` | Do not restore credentials or refresh-token state from the past. Restore implementation must invalidate sessions according to the security policy in a later phase. |
| `audit_logs` | Keep the live, append-only audit trail. `BackupAuditLogWriter` records the backup/restore action outside the restored operational data, satisfying the audit acceptance criterion. |
| `backup_records` | This is the control-plane record used to authorize, locate, and log the restore. It must survive the restore that it initiates. |

## Foreign-Key Boundary Rules

1. Every foreign key between included operational tables must have both tables
   in the snapshot. The restore planner must reject a `FULL` plan that violates
   this rule.
2. Foreign keys from included operational tables to `users` are approved
   external references. They are valid only because `users` is preserved in the
   same initialized environment.
3. No other parent table may be omitted from `FULL` without a documented BA/PO
   decision and an automated validation test.
4. The restore implementation must treat the following in-scope cycles as
   explicit restore phases, not as arbitrary list ordering:
   - `queue_items.visit_id -> visits.id` and `visits.queue_item_id -> queue_items.id`
   - `invoices.original_invoice_id -> invoices.id`
5. Template data is restored in parent-first order: `specialties` ->
   `medical_record_templates` -> `medical_record_template_versions` ->
   `medical_record_template_sections`; `medical_records.applied_template_version_id`
   is restored only after its template version exists.
6. Insert order, delete order, and snapshot scope are separate concerns. They
   must not be represented by one shared ordered list.

## Snapshot Format and Compatibility

Each backup JSON file contains a manifest with `formatVersion`, the complete
table list, an ISO-8601 creation time, and the latest successful Flyway schema
version. Before it deletes any data, restore validates all of the following:

1. The format version is supported and the manifest matches the table payload.
2. The table list exactly matches the configured `FULL` allow-list.
3. The current Flyway schema version exactly matches the snapshot version.
4. Each snapshot table has the same column names and JDBC types as the current
   database, and every data row has the expected number of values.

Pre-manifest JSON snapshots are not compatible with this format and are
rejected. A deliberately rejected restore is safer than deleting data from an
unknown or incompatible snapshot.

## Backup Lifecycle Transactions

Backup metadata and data export use separate transactions:

1. Create `IN_PROGRESS` in a `REQUIRES_NEW` transaction.
2. Export the snapshot in an independent read-only `REPEATABLE_READ`
   transaction, providing a consistent database view to the JDBC adapter.
3. Mark `SUCCESS` or `FAILED` in a separate `REQUIRES_NEW` transaction.

As a result, a failed export leaves a durable `FAILED` record. It cannot be
restored and remains available for the administrator to inspect, satisfying the
status-control acceptance criterion.

## MySQL Integration Coverage

The backup integration suite uses MySQL 8 Testcontainers, not H2 compatibility
mode. CI must provide a Docker daemon and execute these cases:

1. Restore the `payments -> invoices -> invoice_lines` chain, including an
   adjustment invoice referencing its original invoice.
2. Restore the `queue_items <-> visits` cycle using its configured deferred
   foreign key.
3. Restore catalog data (`rooms`, `diagnosis_catalog`, and
   `clinical_service_catalog`) together with the operational records.
4. Restore specialty, template, immutable template version, section, and the
   medical record's applied-template-version reference without data loss.
5. Persist `FAILED` when snapshot export throws.
6. Roll back every delete and insert when a restore fails after it has started.

## Consequences

- `FULL` can restore the clinic's operational state to the backup point while
  retaining an administrator capable of completing and auditing the operation.
- `FULL` cannot rebuild a new empty database. Disaster recovery requires a
  separate physical database backup/runbook, including schema, auth, secrets,
  and storage configuration.
- New `FULL` snapshots use this scope. Restore derives parent-first insert and
  child-first delete order from database foreign-key metadata, then applies the
  two deferred cyclic references after every table has been inserted.
- Legacy snapshots that omit an in-scope parent table are rejected before data
  is deleted. This is safer than a partial restore with broken references.

## BA/PO Approval Checklist

- [ ] Confirm that `FULL` means operational logical backup, not empty-database
      disaster recovery.
- [ ] Confirm that restoring users, roles, sessions, audit logs, and backup
      records is excluded.
- [ ] Confirm that restoring to a different environment is unsupported for
      `FULL` backups.
- [ ] Confirm the listed clinical, pharmacy, inventory, and billing data groups
      are in scope.

## Traceability

- User story: `NCL-09-CN-005` - backup and restore data.
- Acceptance criteria: successful timestamped backup; block failed/in-progress
  restore; admin-only access; audit backup and restore actor/time.
- Schema sources: `backend/src/main/resources/db/migration/V1`, `V4`, `V6`,
  `V8` through `V11`, `V16`, `V18`, `V19`, `V21`, `V23`, and `V27`.
