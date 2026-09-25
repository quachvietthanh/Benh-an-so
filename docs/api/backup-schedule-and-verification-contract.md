# NCL-09-CN-009 — Backup Schedule & Integrity Verification Contract

## 1. Overview & Scope

This contract defines the API endpoints for **Automated Scheduled Backup and Backup Integrity Verification** (`NCL-09-CN-009`) under Epic **NCL-09: Quản trị hệ thống và danh mục dịch vụ**.

It extends the base manual backup/restore capabilities (`NCL-09-CN-005`) with:
1. Configuring the automated daily backup schedule (enabled status, daily run time).
2. Querying schedule state, execution history, and active failure alerts.
3. Dismissing active failure alerts once reviewed by an administrator.
4. On-demand verification of backup snapshot integrity (JSON syntax, Flyway schema alignment, table list completeness, and column/row data integrity).

---

## 2. Security & RBAC Constraints

- **Allowed Roles**: Administrator (`ROLE_ADMIN` / `VT-05`).
- **Required Permissions**:
  - `BACKUP_READ`: for `GET /backups/schedule`, `POST /backups/verify-latest`, `POST /backups/{id}/verify`.
  - `BACKUP_CREATE`: for `PUT /backups/schedule`, `POST /backups/schedule/dismiss-alert`.
- **Non-Admin Roles**: Calls from any other role (`DOCTOR`, `RECEPTIONIST`, `PHARMACIST`, `MANAGER`) are strictly rejected with `403 Forbidden` and audited as `ACCESS_DENIED`.

---

## 3. Endpoints

### 3.1. `GET /backups/schedule`

Retrieves the current automated backup schedule configuration and failure alert state.

- **Method**: `GET`
- **Path**: `/backups/schedule`
- **Permission**: `BACKUP_READ`
- **Response `200 OK`**:
```json
{
  "id": "99999999-9999-9999-9999-999999999999",
  "enabled": true,
  "dailyTime": "02:00",
  "cronExpression": "0 0 2 * * *",
  "lastRunAt": "2026-09-24T02:00:00Z",
  "lastStatus": "SUCCESS",
  "lastFailureReason": null,
  "alertActive": false,
  "lastVerifiedAt": "2026-09-24T08:00:00Z",
  "lastVerificationStatus": "VALID",
  "updatedBy": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1",
  "updatedAt": "2026-09-24T01:00:00Z"
}
```

---

### 3.2. `PUT /backups/schedule`

Updates the automated backup schedule configuration.

- **Method**: `PUT`
- **Path**: `/backups/schedule`
- **Permission**: `BACKUP_CREATE`
- **Request Body**:
```json
{
  "enabled": true,
  "dailyTime": "03:30"
}
```
- **Validation Rules**:
  - `enabled`: Required (`@NotNull`).
  - `dailyTime`: Required, must match `^([01]?[0-9]|2[0-3]):[0-5][0-9]$` (24-hour format `HH:mm`).
- **Response `200 OK`**: Returns updated `BackupScheduleResponse`.
- **Response `400 Bad Request`**: When `dailyTime` is invalid or missing.

---

### 3.3. `POST /backups/schedule/dismiss-alert`

Dismisses the active failure alert after an administrator has acknowledged the issue.

- **Method**: `POST`
- **Path**: `/backups/schedule/dismiss-alert`
- **Permission**: `BACKUP_CREATE`
- **Response `200 OK`**: Returns `BackupScheduleResponse` with `alertActive: false`.

---

### 3.4. `POST /backups/verify-latest`

Verifies the integrity of the most recent successful backup snapshot without restoring data or altering database tables.

- **Method**: `POST`
- **Path**: `/backups/verify-latest`
- **Permission**: `BACKUP_READ`
- **Response `200 OK`** (Valid snapshot):
```json
{
  "backupId": "12345678-1234-1234-1234-123456789abc",
  "backupCode": "BKP-20260924-0001",
  "fileName": "BKP-20260924-0001.json",
  "valid": true,
  "readable": true,
  "dataIntact": true,
  "tableCount": 28,
  "rowCount": 1520,
  "schemaVersion": "87",
  "verifiedAt": "2026-09-24T08:30:00Z",
  "message": "Bản sao lưu đọc được và đủ dữ liệu.",
  "issues": []
}
```
- **Response `200 OK`** (Corrupted / Mismatched snapshot):
```json
{
  "backupId": "12345678-1234-1234-1234-123456789abc",
  "backupCode": "BKP-20260924-0001",
  "fileName": "BKP-20260924-0001.json",
  "valid": false,
  "readable": true,
  "dataIntact": false,
  "tableCount": 28,
  "rowCount": 1500,
  "schemaVersion": "80",
  "verifiedAt": "2026-09-24T08:30:00Z",
  "message": "Phát hiện 1 lỗi toàn vẹn trong bản sao lưu.",
  "issues": [
    "Phiên bản schema của bản sao lưu (80) không khớp với database hiện tại (87)."
  ]
}
```
- **Response `404 Not Found`**: When no successful backup records exist.

---

### 3.5. `POST /backups/{id}/verify`

Verifies the integrity of a specific backup record by its UUID.

- **Method**: `POST`
- **Path**: `/backups/{id}/verify`
- **Permission**: `BACKUP_READ`
- **Response `200 OK`**: Returns `BackupVerificationResponse`.
- **Response `404 Not Found`**: When the backup record with the given ID does not exist.

---

## 4. Acceptance Criteria Mapping

| Acceptance Criteria | Mechanism / Endpoint | Expected Behavior |
| :--- | :--- | :--- |
| **NCL-09-CN-009-TC-01** (Scheduled Execution) | `ScheduledBackupJob` $\rightarrow$ `ExecuteScheduledBackupUseCase` | At scheduled time, executes snapshot export under system actor, marks `SUCCESS`, logs to `backup_records`, updates schedule `lastRunAt` and `lastStatus`. |
| **NCL-09-CN-009-TC-02** (Failure Alert) | `ScheduledBackupExecutionService` $\rightarrow$ `BackupScheduleConfiguration` | On failure, records `FAILED` with `failureReason`, sets `alertActive: true` in schedule config, writes error audit log. Admin sees alert via `GET /backups/schedule`. |
| **NCL-09-CN-009-TC-03** (Integrity Verification) | `POST /backups/verify-latest` $\rightarrow$ `VerifyBackupIntegrityUseCase` | Validates file readability, JSON format, schema version matching, table completeness (28 tables), and column/row structure. Returns structured verdict. |
