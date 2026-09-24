# NCL-09-CN-009: Automatic Scheduled Backup — Backend Contract

## Scope

Backend implementation for automatic daily backup, per-run history, failure
alerting and latest-backup integrity verification. The frontend consumes the
APIs below; no frontend changes are part of this scope.

## Reuse of NCL-09-CN-005

No second backup format exists. The scheduled run reuses the exact
`JsonDatabaseBackupStorageAdapter` snapshot and the `BackupRecord` /
`backup_records` history introduced for manual backup (NCL-09-CN-005). A
scheduled run differs only in trigger and `backupType = SCHEDULED`.

## API

Base path: `/api/v1` (servlet context). All endpoints require an authenticated
ADMIN with the listed permission.

| Method | Path | Permission | Description |
| --- | --- | --- | --- |
| `GET` | `/backups/schedule` | `BACKUP_SCHEDULE_READ` | Current schedule: `{ "enabled": bool, "backupTime": "HH:mm:ss", "updatedAt": ISO-8601 }`. Disabled default `02:00:00` when not yet configured. |
| `PUT` | `/backups/schedule` | `BACKUP_SCHEDULE_UPDATE` | Full-state update. Body `{ "enabled": bool, "backupTime": "HH:mm:ss" }`. Replaces both fields. Audited (`CONFIGURATION`). |
| `POST` | `/backups/latest/verify` | `BACKUP_VERIFY` | Verifies the latest `SUCCESS` backup. Response `{ "backupId", "backupCode", "fileName", "backupCreatedAt", "valid": bool, "reason", "tableCount", "rowCount", "verifiedAt" }`. `valid=false` + `reason` when no successful backup or when the snapshot is unreadable/incomplete. Audited (`BACKUP_VERIFY`). |
| `GET` | `/backups` | `BACKUP_READ` | History (existing). Each entry now includes `failureReason` (nullable) and `backupType` may be `SCHEDULED`. |

## Schedule semantics

- Daily, single `backupTime` (`HH:mm`), `enabled` flag. No cron/weekly/monthly.
- Timezone fixed to `app.backup.schedule.zone-id` (`Asia/Ho_Chi_Minh`).
- The poller (`BackupScheduleScheduler`, 60 s) delegates to
  `ExecuteScheduledBackupUseCase`; all decisions live in the service.

## Idempotency (daily, restart-safe)

At most one `SCHEDULED` record is created per local day, regardless of success
or failure:

- SUCCESS today → no second run today.
- FAILED today → **no automatic retry** today.
- Application restart or 60 s polls → no duplicate.

A same-JVM `AtomicBoolean` prevents two overlapping polls. Multi-instance
deduplication is not provided (no distributed lock in this project).

## Failure alerting

A failed scheduled run is persisted as `backup_records.status = FAILED` with a
sanitized `failureReason` (bounded, no stack trace or secrets). This history
entry is the administrator-observable alert surface (via `GET /backups`). No
email/SMS/push channel is introduced.

## Assumptions (not Excel requirements)

- Default daily time `02:00` is a technical seed default only.
- Timezone `Asia/Ho_Chi_Minh` is the project's existing convention.
- No backup retention/deletion policy is defined (QTN-19 applies to medical
  records, not backup files); nothing is deleted and no retention is invented.

## Migrations

- `V96__add_scheduled_backup_schema.sql` — `backup_schedules` singleton,
  `backup_records.failure_reason`, widened `backup_type` CHECK.
- `V97__add_scheduled_backup_permissions.sql` — seeds the three permissions to
  `ADMIN` (idempotent).
