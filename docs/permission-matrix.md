# Permission Matrix - Hệ thống phân quyền Bệnh số án

## Ma trận quyền chi tiết

| API Endpoint | Method | ADMIN | DOCTOR | NURSE | RECEPTIONIST | PHARMACIST |
|---|---|---|---|---|---|---|
| **Authentication** | | | | | | |
| `/api/v1/auth/login` | POST | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/api/v1/auth/register` | POST | ✅ | ❌ | ❌ | ❌ | ❌ |
| `/api/v1/auth/refresh` | POST | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/api/v1/auth/logout` | POST | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/api/v1/auth/forgot-password` | POST | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/api/v1/auth/reset-password` | POST | ✅ | ✅ | ✅ | ✅ | ✅ |
| | | | | | | |
| **User Management** | | | | | | |
| `/api/v1/users` | GET | ✅ | ❌ | ❌ | ❌ | ❌ |
| `/api/v1/users` | POST | ✅ | ❌ | ❌ | ❌ | ❌ |
| `/api/v1/users/{id}` | GET | ✅ | ❌ | ❌ | ❌ | ❌ |
| `/api/v1/users/{id}` | PUT | ✅ | ❌ | ❌ | ❌ | ❌ |
| `/api/v1/users/{id}` | DELETE | ✅ | ❌ | ❌ | ❌ | ❌ |
| `/api/v1/users/{id}/roles` | PUT | ✅ | ❌ | ❌ | ❌ | ❌ |
| | | | | | | |
| **Patient Management** | | | | | | |
| `/api/v1/patients` | GET | ✅ | ✅ | ✅ | ✅ | ❌ |
| `/api/v1/patients` | POST | ✅ | ✅ | ❌ | ✅ | ❌ |
| `/api/v1/patients/{id}` | GET | ✅ | ✅ | ✅ | ✅ | ❌ |
| `/api/v1/patients/{id}` | PUT | ✅ | ✅ | ❌ | ✅ | ❌ |
| `/api/v1/patients/{id}` | DELETE | ✅ | ❌ | ❌ | ❌ | ❌ |
| `/api/v1/patients/me` | GET | ❌ | ✅ | ✅ | ✅ | ❌ |
| | | | | | | |
| **Medical Records** | | | | | | |
| `/api/v1/medical-records` | GET | ✅ | ✅ | ✅ | ❌ | ❌ |
| `/api/v1/medical-records` | POST | ✅ | ✅ | ❌ | ❌ | ❌ |
| `/api/v1/medical-records/{id}` | GET | ✅ | ✅ | ✅ | ❌ | ❌ |
| `/api/v1/medical-records/{id}` | PUT | ✅ | ✅ | ❌ | ❌ | ❌ |
| `/api/v1/medical-records/{id}` | DELETE | ✅ | ❌ | ❌ | ❌ | ❌ |
| `/api/v1/medical-records/{id}/diagnoses` | PUT | ❌ | ✅ | ❌ | ❌ | ❌ |
| `/api/v1/medical-records/{id}/diagnoses` | GET | ✅ | ✅ | ❌ | ❌ | ❌ |
| `/api/v1/medical-records/{id}/sign` | POST | ❌ | ✅ (chỉ bác sĩ phụ trách lượt khám) | ❌ | ❌ | ❌ |
| `/api/v1/medical-records/{id}/status` | PATCH | ✅ | ✅ | ✅ | ❌ | ❌ |
| | | | | | | |
| **Prescriptions** | | | | | | |
| `/api/v1/prescriptions` | GET | ✅ | ✅ | ❌ | ❌ | ✅ |
| `/api/v1/prescriptions` | POST | ✅ | ✅ | ❌ | ❌ | ❌ |
| `/api/v1/prescriptions/{id}` | GET | ✅ | ✅ | ❌ | ❌ | ✅ |
| `/api/v1/prescriptions/{id}` | PUT | ✅ | ✅ | ❌ | ❌ | ❌ |
| `/api/v1/prescriptions/{id}` | DELETE | ✅ | ✅ | ❌ | ❌ | ❌ |
| `/api/v1/prescriptions/{id}/status` | PUT | ✅ | ❌ | ❌ | ❌ | ✅ |
| `/api/v1/prescriptions/{id}/interconnection` | POST | ❌ | ✅ | ❌ | ❌ | ❌ |
| `/api/v1/prescription-interconnections` | GET | ✅ | ❌ | ❌ | ❌ | ❌ |
| `/api/v1/prescriptions/{id}/interconnection/retry` | POST | ✅ | ❌ | ❌ | ❌ | ❌ |
| | | | | | | |
| **Appointments** | | | | | | |
| `/api/v1/appointments` | GET | ✅ | ✅ | ❌ | ✅ | ❌ |
| `/api/v1/appointments` | POST | ✅ | ❌ | ❌ | ✅ | ❌ |
| `/api/v1/appointments/{id}` | GET | ✅ | ✅ | ❌ | ✅ | ❌ |
| `/api/v1/appointments/{id}` | PUT | ✅ | ✅ | ❌ | ✅ | ❌ |
| `/api/v1/appointments/{id}` | DELETE | ✅ | ❌ | ❌ | ✅ | ❌ |
| `/api/v1/appointments/me` | GET | ❌ | ✅ | ✅ | ✅ | ❌ |
| | | | | | | |
| **Vital Signs** | | | | | | |
| `/api/v1/vital-signs` | GET | ✅ | ✅ | ✅ | ❌ | ❌ |
| `/api/v1/vital-signs` | POST | ✅ | ✅ | ✅ | ❌ | ❌ |
| `/api/v1/vital-signs/{id}` | PUT | ✅ | ✅ | ✅ | ❌ | ❌ |
| | | | | | | |
| **Diagnoses** | | | | | | |
| `/api/v1/diagnosis-catalog` | GET | ✅ | ✅ | ❌ | ❌ | ❌ |
| `/api/v1/system/diagnosis-catalog/**` | ALL | ✅ | ❌ | ❌ | ❌ | ❌ |
| | | | | | | |
| **Pharmacy / Inventory** | | | | | | |
| `/api/v1/pharmacy/inventory` | GET | ✅ | ❌ | ❌ | ❌ | ✅ |
| `/api/v1/pharmacy/inventory` | POST | ✅ | ❌ | ❌ | ❌ | ✅ |
| `/api/v1/pharmacy/inventory/{id}` | PUT | ✅ | ❌ | ❌ | ❌ | ✅ |
| `/api/v1/pharmacy/inventory/{id}` | DELETE | ✅ | ❌ | ❌ | ❌ | ❌ |
| | | | | | | |
| **Invoices / Payments** | | | | | | |
| `/api/v1/invoices` | GET | ✅ | ❌ | ❌ | ✅ | ❌ |
| `/api/v1/invoices` | POST | ✅ | ❌ | ❌ | ✅ | ❌ |
| `/api/v1/invoices/{id}` | GET | ✅ | ❌ | ❌ | ✅ | ❌ |
| `/api/v1/invoices/{id}` | PUT | ✅ | ❌ | ❌ | ✅ | ❌ |
| | | | | | | |
| **Audit Logs** | | | | | | |
| `/api/v1/audit-logs` | GET | ✅ | ❌ | ❌ | ❌ | ❌ |
| `/api/v1/audit-logs/{id}` | GET | ✅ | ❌ | ❌ | ❌ | ❌ |
| | | | | | | |
| **Medical Queue** | | | | | | |
| `/api/v1/queue` | POST | ✅ | ❌ | ❌ | ✅ | ❌ |
| `/api/v1/queue/call-next` | POST | ✅ | ✅ | ❌ | ❌ | ❌ |
| `/api/v1/queue/{id}/status` | PUT | ✅ | ✅ | ✅ | ❌ | ❌ |
| `/api/v1/queue/room/{roomNumber}` | GET | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/api/v1/queue/doctor/{doctorId}` | GET | ✅ | ✅ | ❌ | ❌ | ❌ |
| `/api/v1/queue/count` | GET | ✅ | ✅ | ✅ | ✅ | ✅ |
| | | | | | | |
| **Medical History** | | | | | | |
| `/patients/{patientId}/medical-history` | GET | ✅ | ✅ (chỉ BN đã khám) | ❌ | ❌ | ❌ |

| | | | | | | |
| **Admin / System** | | | | | | |
| `/api/v1/admin/**` | ALL | ✅ | ❌ | ❌ | ❌ | ❌ |
| `/api/v1/roles` | GET | ✅ | ❌ | ❌ | ❌ | ❌ |
| `/api/v1/roles` | POST | ✅ | ❌ | ❌ | ❌ | ❌ |
| `/api/v1/permissions` | GET | ✅ | ❌ | ❌ | ❌ | ❌ |

## Role Hierarchy (Phân cấp vai trò)

```
ADMIN (Quản trị viên)
  ├── Full system access
  ├── User management
  ├── Role & Permission management
  ├── Audit log access
  └── Medical Queue full access

DOCTOR (Bác sĩ)
  ├── Patient CRUD
  ├── Medical Records CRUD
  ├── Prescriptions (create/read/update)
  ├── Diagnoses management
  ├── Appointments management
  ├── Medical Queue (call next, update status, view)
  └── Medical History view

NURSE (Y tá)
  ├── Patient read
  ├── Medical Records read
  ├── Vital Signs CRUD
  ├── Appointments read
  └── Medical Queue update status + view

RECEPTIONIST (Lễ tân)
  ├── Patient CRUD (read/write)
  ├── Appointments CRUD
  ├── Invoices CRUD
  ├── Patient registration
  └── Medical Queue (add patient, view, count)

PHARMACIST (Dược sĩ)
  ├── Prescriptions read
  ├── Prescription status update
  ├── Pharmacy inventory CRUD
  └── No patient/medical record access
```

## Permission Categories

| Category | Code Prefix | Description |
|---|---|---|
| User Management | `USER_` | Quản lý người dùng |
| Patient Management | `PATIENT_` | Quản lý bệnh nhân |
| Medical Record | `RECORD_` | Quản lý hồ sơ bệnh án |
| Prescription | `PRESCRIPTION_` | Quản lý đơn thuốc |
| Appointment | `APPOINTMENT_` | Quản lý lịch hẹn |
| Vital Signs | `VITAL_SIGN_` | Quản lý dấu hiệu sinh tồn |
| Diagnosis | `DIAGNOSIS_` | Quản lý chẩn đoán |
| Pharmacy | `PHARMACY_` | Quản lý nhà thuốc |
| Invoice | `INVOICE_` | Quản lý hóa đơn |
| Audit Log | `AUDIT_` | Quản lý nhật ký |
| Role | `ROLE_` | Quản lý vai trò |
| Permission | `PERMISSION_` | Quản lý quyền |
| Medical Queue | `QUEUE_` | Quản lý hàng đợi khám |

## Chi tiết Permission Codes

```java
// User
USER_CREATE, USER_READ, USER_UPDATE, USER_DELETE, USER_ASSIGN_ROLE

// Patient
PATIENT_CREATE, PATIENT_READ, PATIENT_UPDATE, PATIENT_DELETE

// Medical Record
RECORD_CREATE, RECORD_READ, RECORD_UPDATE, RECORD_DELETE, RECORD_UPDATE_STATUS

// Prescription
PRESCRIPTION_CREATE, PRESCRIPTION_READ, PRESCRIPTION_UPDATE, PRESCRIPTION_DELETE, PRESCRIPTION_UPDATE_STATUS,
PRESCRIPTION_INTERCONNECTION_SEND, PRESCRIPTION_INTERCONNECTION_READ, PRESCRIPTION_INTERCONNECTION_RETRY

// Appointment
APPOINTMENT_CREATE, APPOINTMENT_READ, APPOINTMENT_UPDATE, APPOINTMENT_DELETE

// Vital Sign
VITAL_SIGN_CREATE, VITAL_SIGN_READ, VITAL_SIGN_UPDATE

// Diagnosis
DIAGNOSIS_CREATE, DIAGNOSIS_READ, DIAGNOSIS_UPDATE

// Pharmacy
PHARMACY_CREATE, PHARMACY_READ, PHARMACY_UPDATE, PHARMACY_DELETE

// Invoice
INVOICE_CREATE, INVOICE_READ, INVOICE_UPDATE, INVOICE_DELETE

// Audit
AUDIT_READ

// Role & Permission
ROLE_READ, ROLE_CREATE, ROLE_UPDATE, ROLE_DELETE
PERMISSION_READ

// Medical Queue
QUEUE_CREATE, QUEUE_CALL_NEXT, QUEUE_UPDATE_STATUS, QUEUE_VIEW, QUEUE_COUNT
```
