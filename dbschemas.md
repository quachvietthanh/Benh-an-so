# Database schema hiện tại

Tài liệu này mô tả schema đang thực sự tồn tại trong repo hiện nay, dựa trên các file migration và cấu trúc backend.

## Công nghệ

- Database: MySQL 8.x
- ORM: Hibernate / Spring Data JPA
- Migration: Flyway
- Mã hóa khóa chính: BINARY(16) trong migration hiện tại

---

## Schema đã có trong repo

### 1. Auth & authorization

#### roles
| Column | Type | Notes |
|---|---|---|
| id | BINARY(16) | PK |
| name | VARCHAR(50) | UNIQUE |
| description | VARCHAR(255) | |
| is_system | BOOLEAN | |
| created_at | TIMESTAMP | |

#### users
| Column | Type | Notes |
|---|---|---|
| id | BINARY(16) | PK |
| username | VARCHAR(50) | UNIQUE |
| password_hash | VARCHAR(255) | NOT NULL |
| full_name | VARCHAR(100) | |
| email | VARCHAR(100) | UNIQUE |
| phone | VARCHAR(20) | |
| role_id | BINARY(16) | FK -> roles.id |
| active | BOOLEAN | |
| last_login_at | TIMESTAMP | |
| created_at | TIMESTAMP | |

#### user_sessions
| Column | Type | Notes |
|---|---|---|
| id | BINARY(16) | PK |
| user_id | BINARY(16) | FK -> users.id |
| token_hash | VARCHAR(255) | |
| expires_at | TIMESTAMP | |
| created_at | TIMESTAMP | |
| last_used_at | TIMESTAMP | |
| revoked_at | TIMESTAMP | |

#### login_logs
| Column | Type | Notes |
|---|---|---|
| id | BINARY(16) | PK |
| user_id | BINARY(16) | FK -> users.id |
| action_type | VARCHAR(30) | |
| resource_type | VARCHAR(30) | |
| resource_id | BINARY(16) | |
| detail | JSON | |
| ip_address | VARCHAR(45) | |
| created_at | TIMESTAMP | |


#### role_permissions

### PATIENT

### patients

| Column | Type |
|----------|------|
| id | UUID |
| patient_code | VARCHAR |
| full_name | VARCHAR |
| date_of_birth | DATE |
| gender | ENUM |
| phone | VARCHAR |
| email | VARCHAR |
| address | VARCHAR |
| identity_number | VARCHAR |
| insurance_number | VARCHAR |
| blood_type | ENUM |
| emergency_contact | VARCHAR |
| emergency_phone | VARCHAR |
| active | BOOLEAN |
| created_by | UUID FK |
| created_at | TIMESTAMP |
| updated_at | TIMESTAMP |

### visits

| Column | Type |
|----------|------|
| id | UUID |
| patient_id | UUID FK |
| doctor_id | UUID FK |
| department_id | UUID FK |
| visit_code | VARCHAR |
| visit_type | ENUM |
| visit_status | ENUM |
| visit_at | TIMESTAMP |
| reason | VARCHAR |
| note | TEXT |
| created_at | TIMESTAMP |
| updated_at | TIMESTAMP |

### patient_change_logs

| Column | Type |
|----------|------|
| id | UUID |
| patient_id | UUID FK |
| changed_by | UUID FK |
| action | ENUM |
| change_detail | JSON |
| created_at | TIMESTAMP |

### medical_record_access_logs

| Column | Type |
|----------|------|
| id | UUID |
| patient_id | UUID FK |
| visit_id | UUID FK |
| accessed_by | UUID FK |
| action | ENUM |
| ip_address | VARCHAR |
| accessed_at | TIMESTAMP |

---


## APPOINTMENT

#### appointments

| Column | Type | Notes |
|---|---|---|
| id | BINARY(16) | PK |
| appointment_code | VARCHAR(30) | UNIQUE |
| patient_id | BINARY(16) | FK -> patients.id |
| doctor_id | BINARY(16) | FK -> users.id |
| start_time | TIMESTAMP | NOT NULL |
| end_time | TIMESTAMP | NOT NULL |
| status | VARCHAR(30) | AppointmentStatus |
| reason | VARCHAR(500) | NOT NULL |
| cancel_reason | VARCHAR(500) | NULL |
| checked_in_at | TIMESTAMP | NULL |
| completed_at | TIMESTAMP | NULL |
| created_by | BINARY(16) | FK -> users.id |
| created_at | TIMESTAMP | NOT NULL |

## STOCK_RECEIPT

Phiếu nhập kho.

| Column | Type |
|----------|------|
| id | UUID |
| reference_number | VARCHAR |
| received_by | UUID FK |
| received_at | TIMESTAMP |
| notes | TEXT |

---

## STOCK_RECEIPT_ITEM

Chi tiết nhập kho.

| Column | Type |
|----------|------|
| id | UUID |
| receipt_id | UUID FK |
| medicine_id | UUID FK |
| batch_id | UUID FK |
| quantity | INT |
| unit_price | DECIMAL |

---

## DISPENSE_RECORD

Phiếu cấp phát thuốc.

| Column | Type |
|----------|------|
| id | UUID |
| prescription_id | UUID FK |
| dispensed_by | UUID FK |
| dispensed_at | TIMESTAMP |

---

## DISPENSE_ITEM

Chi tiết cấp phát.

| Column | Type |
|----------|------|
| id | UUID |
| dispense_id | UUID FK |
| prescription_item_id | UUID FK |
| batch_id | UUID FK |
| quantity | INT |

---

# 9. Billing

## PAYMENT

Thanh toán.

| Column | Type |
|----------|------|
| id | UUID |
| visit_id | UUID FK |
| exam_fee | DECIMAL |
| medicine_fee | DECIMAL |
| total_amount | DECIMAL |
| amount_paid | DECIMAL |
| payment_method | VARCHAR |
| status | VARCHAR |
| collected_by | UUID FK |
| paid_at | TIMESTAMP |

---

## INVOICE

Hóa đơn.

| Column | Type |
|----------|------|
| id | UUID |
| visit_id | UUID FK |
| payment_id | UUID FK |
| invoice_number | VARCHAR |
| type | VARCHAR |
| original_invoice_id | UUID FK |
| adjustment_reason | TEXT |
| total_amount | DECIMAL |
| created_by | UUID FK |
| created_at | TIMESTAMP |

---

## INVOICE_LINE

Chi tiết hóa đơn.

| Column | Type |
|----------|------|
| id | UUID |
| invoice_id | UUID FK |
| item_type | VARCHAR |
| item_name | VARCHAR |
| service_id | UUID FK |
| medicine_id | UUID FK |
| quantity | INT |
| unit_price | DECIMAL |
| amount | DECIMAL |

---

# 10. After Care

## FOLLOWUP_REMINDER

Nhắc tái khám.

| Column | Type |
|----------|------|
| id | UUID |
| patient_id | UUID FK |
| visit_id | UUID FK |
| remind_date | DATE |
| note | TEXT |
| status | VARCHAR |
| created_by | UUID FK |
| created_at | TIMESTAMP |

---

## AFTERCARE_NOTE

Theo dõi sau khám.

| Column | Type |
|----------|------|
| id | UUID |
| patient_id | UUID FK |
| visit_id | UUID FK |
| content | TEXT |
| contact_result | TEXT |
| recorded_by | UUID FK |
| recorded_at | TIMESTAMP |

---

# 11. System Configuration

## CLINIC_CONFIG

Thông tin phòng khám.

| Column | Type |
|----------|------|
| id | UUID |
| name | VARCHAR |
| address | TEXT |
| phone | VARCHAR |
| working_hours | JSON |
| created_at | TIMESTAMP |

---

## BACKUP_LOG

Lịch sử sao lưu.

| Column | Type |
|----------|------|
| id | UUID |
| file_path | VARCHAR |
| status | VARCHAR |
| created_by | UUID FK |
| created_at | TIMESTAMP |
| restored_at | TIMESTAMP |

---
# 13. Quy ước đặt tên

- Tên bảng: UPPER_SNAKE_CASE
- Primary Key: `id`
- Foreign Key: `<entity>_id`
- Thời gian tạo: `created_at`
- Người tạo: `created_by`
- Trạng thái: `status`
- Boolean: tiền tố `is_`
- UUID dùng làm Primary Key cho toàn bộ bảng.
