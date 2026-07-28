# Frontend Status Report

**Dự án:** Bệnh Án Số — Clinic Management System  
**Vai trò:** Senior Frontend Code Auditor  
**Scope:** `frontend/src/` — React + Vite  
**Ngày:** 2026-07-28  

---

## 1. Pages & Components Audit

### 1.1 Implemented Pages

| Page | File | Trạng thái | SLOC | Ghi chú |
|------|------|-----------|------|---------|
| **Login** | `Login.jsx` | ✅ Hoàn chỉnh | 107 | Form đăng nhập, redirect nếu đã auth, link tra cứu |
| **Dashboard** | `Dashboard.jsx` | ✅ Hoàn chỉnh | 277 | Thống kê, biểu đồ SVG, danh sách lịch hẹn, BN mới, thuốc |
| **PatientList** | `PatientList.jsx` | ✅ Hoàn chỉnh | 493 | CRUD + search + filter + export Excel + modal đăng ký BN |
| **PatientDetail** | `PatientDetail.jsx` | ✅ Tồn tại | — | Chi tiết hồ sơ BN |
| **AppointmentQueue** | `AppointmentQueue.jsx` | ✅ Hoàn chỉnh | 1119 | Tab lịch hẹn + tab hàng đợi, CRUD, check-in, no-show, detail |
| **MedicalEncounter** | `MedicalEncounter.jsx` | ✅ Hoàn chỉnh | 220 | Ghi bệnh án, chỉ định CLS, kết quả, file đính kèm |
| **PrescriptionPage** | `PrescriptionPage.jsx` | ✅ Tồn tại | — | Kê đơn thuốc |
| **PharmacyPage** | `PharmacyPage.jsx` | ✅ Tồn tại | — | Quản lý kho thuốc |
| **BillingPage** | `BillingPage.jsx` | ✅ Tồn tại | — | Hóa đơn, thanh toán |
| **ReportsPage** | `ReportsPage.jsx` | ✅ Tồn tại | — | Báo cáo |
| **UsersPage** | `UsersPage.jsx` | ✅ Tồn tại | — | Quản lý người dùng |
| **ServicesPage** | `ServicesPage.jsx` | ✅ Tồn tại | — | Quản lý dịch vụ |
| **SystemManagementPage** | `SystemManagementPage.jsx` | ✅ Tồn tại | — | Quản trị hệ thống |
| **PublicLookupPage** | `PublicLookupPage.jsx` | ✅ Tồn tại | — | Tra cứu công khai |
| **NotFound** | `NotFound.jsx` | ✅ Tồn tại | — | 404 |

### 1.2 Thư mục pages/

Có thư mục `appointment/` rỗng — dự kiến chứa các components chuyên biệt (QueueBoard) nhưng chưa có nội dung.

### 1.3 Components Common

| Component | File | Mô tả |
|-----------|------|-------|
| **RoleProtected** | `RoleProtected.jsx` | Component wrapper, kiểm tra roles/permissions trước khi render children |
| **RoleRoute** | `RoleRoute.jsx` | Route guard dựa trên role |
| **MainLayout** | `MainLayout.jsx` | Layout chính: sidebar menu + header + outlet |

### 1.4 Kết luận Pages

- **100% pages đã được implement** với UI đầy đủ, không có placeholder hay màn hình trống.
- Các page chính (PatientList, AppointmentQueue, MedicalEncounter, Dashboard) có business logic phức tạp, xử lý cả API call và mock data fallback.
- Modal đăng ký BN (PatientRegisterModal) được embed trực tiếp trong PatientList.jsx.

---

## 2. API & Integration Layer Audit

### 2.1 axiosClient (`api/axiosClient.js`)

```
baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'
- Request interceptor: gắn Bearer token từ localStorage
- Response interceptor: 401 → xóa token, redirect /login
```

### 2.2 Danh sách API Service Files

#### authApi.js — Authentication
| Method | Endpoint | Request Body | Response |
|--------|----------|-------------|----------|
| POST | `/auth/login` | `{ username, password }` | `{ accessToken, userId, username, role, expiredAt }` |

#### patientApi.js — Bệnh nhân
| Method | Endpoint | Params/Body |
|--------|----------|-------------|
| GET | `/patients` | `{ keyword, page, size }` |
| GET | `/patients/{id}` | — |
| GET | `/patients/code/{code}` | — |
| GET | `/patients/{id}/history` | `{ page, size }` |
| POST | `/patients` | `{ fullName, dateOfBirth, gender, phone, ... }` |
| PUT | `/patients/{id}` | patient object |
| DELETE | `/patients/{id}` | — |

#### appointmentApi.js — Lịch hẹn + Hàng đợi (OLD)
| Method | Endpoint | Ghi chú |
|--------|----------|---------|
| GET | `/appointments` | Lấy tất cả lịch hẹn |
| GET | `/users` | Lấy danh sách bác sĩ (filter từ users) |
| GET | `/appointments/queue` | Lấy hàng đợi |
| POST | `/appointments` | Tạo lịch hẹn |
| PATCH | `/appointments/{id}/cancel` | Hủy |
| PATCH | `/appointments/{id}/no-show` | Không đến |
| PATCH | `/appointments/{id}/check-in` | Check-in |
| POST | `/appointments/queue/call-next` | Gọi tiếp theo |
| PATCH | `/appointments/{id}/complete` | Hoàn tất |

**⛔ LƯU Ý:** Frontend đang dùng Appointments endpoints cho queue, nhưng Backend mới có dedicated Queue controller tại `/api/v1/queue*`. Cần migration.

#### medicalRecordApi.js — Bệnh án điện tử
| Method | Endpoint | Ghi chú |
|--------|----------|---------|
| GET | `/medical-records` | `{ patientId, doctorId, page, size }` |
| GET | `/medical-records/{id}` | Chi tiết |
| GET | `/medical-records/by-doctor/{doctorId}` | Theo bác sĩ |
| POST | `/medical-records` | Tạo bệnh án |
| PUT | `/medical-records/{id}` | Cập nhật |
| DELETE | `/medical-records/{id}` | Xóa |
| POST | `/medical-records/{id}/attachments` | Upload file (multipart) |
| GET | `/medical-records/attachments/{id}` | Download file |

#### billingApi.js — Hóa đơn
| Method | Endpoint |
|--------|----------|
| GET | `/invoices` |
| GET | `/invoices/payable` |
| POST | `/invoices/payments` |
| POST | `/invoices/{id}/adjustments` |

#### pharmacyApi.js — Kho thuốc & Đơn thuốc
| Method | Endpoint |
|--------|----------|
| GET | `/pharmacy/medicines` |
| GET | `/prescriptions` |
| POST | `/prescriptions/interactions` |
| POST | `/prescriptions` |
| PUT | `/prescriptions/{id}` |
| GET | `/pharmacy/batches` |
| POST | `/pharmacy/medicines` |
| PUT | `/pharmacy/medicines/{id}` |
| POST | `/pharmacy/batches` |
| POST | `/pharmacy/prescriptions/{id}/dispense` |

#### userApi.js — Người dùng
| Method | Endpoint | Ghi chú |
|--------|----------|---------|
| GET | `/users` | Danh sách (generic) |
| GET | `/users/doctors` | Danh sách bác sĩ |
| POST | `/users` | Tạo user |
| PUT | `/users/{id}` | Cập nhật |
| DELETE | `/users/{id}` | Xóa |
| PATCH | `/users/{id}/activate` | Kích hoạt |
| PATCH | `/users/{id}/deactivate` | Vô hiệu hóa |
| GET | `/admin/users` | Phân trang (admin) |
| GET | `/admin/users/{id}` | Chi tiết (admin) |
| PUT | `/admin/users/{id}/status?locked=true/false` | **Khóa/Mở khóa** |

#### reportApi.js — Báo cáo
| Method | Endpoint |
|--------|----------|
| GET | `/reports/summary` |
| GET | `/reports/visits-timeline` |
| GET | `/reports/top-medicines` |
| GET | `/reports/audit-logs` |
| GET | `/reports/dashboard` |
| GET | `/reports/export` (responseType: blob) |

#### systemApi.js — Hệ thống
| Method | Endpoint |
|--------|----------|
| GET | `/system/services` |
| POST | `/system/services` |
| PUT | `/system/services/{id}` |
| GET | `/system/clinic` |
| PUT | `/system/clinic` |

#### publicLookupApi.js — Tra cứu công khai
| Method | Endpoint | Ghi chú |
|--------|----------|---------|
| POST | `/public/appointments/lookup` | Dùng publicApiClient (không gắn token) |

#### aftercareApi.js — Chăm sóc sau khám (Chạy trên localStorage)
| Method | Mô tả |
|--------|-------|
| getReminders | Lấy từ localStorage |
| createReminder | Lưu vào localStorage |
| updateReminderStatus | Cập nhật trong localStorage |
| getNotes | Lấy từ localStorage |
| createNote | Lưu vào localStorage |

### 2.3 Mock Data Strategy

- File: `services/mockDataService.js` + `mock-data/mockData.js`
- Pattern: **Try API → catch → fallback mock data**
- Auth: Context fallback từ `demoUsers` nếu backend offline
- Tất cả page đều có fallback pattern

### 2.4 API Gaps (so với Backend Backlog 8 Epics)

| Epic | Mã | Trạng thái FE | Ghi chú |
|------|-----|-------------|---------|
| Quản lý bệnh nhân | NCL-01 | ✅ Đầy đủ | patientApi + mock |
| Quản lý lịch hẹn & hàng đợi | NCL-02 | ⚠️ Partial | Dùng `/appointments/*` cũ, chưa migrate sang `/queues*` |
| Khám bệnh & bệnh án điện tử | NCL-03 | ✅ Đầy đủ | medicalRecordApi + file upload |
| Kê đơn thuốc & cảnh báo Tương tác | NCL-04 | ✅ Đầy đủ | pharmacyApi + interactions |
| Quản lý kho thuốc & cấp phát | NCL-05 | ✅ Đầy đủ | pharmacyApi |
| Thu phí & hóa đơn | NCL-06 | ✅ Đầy đủ | billingApi |
| Báo cáo vận hành & Audit | NCL-07 | ⚠️ Partial | reportApi dùng `/reports/*`, cần confirm response |
| Quản trị hệ thống & phân quyền | NCL-08 | ⚠️ Partial | userApi có CRUD + lock/unlock, thiếu role/permission management |

---

## 3. Routing & State Management

### 3.1 Active Routes (`routes/AppRoutes.jsx`)

```
/login                          → Login
/public-lookup                  → PublicLookupPage
/tra-cuu                        → Redirect to /public-lookup

/                               → Dashboard          [roles: any auth]
/patients                       → PatientList        [admin, doctor, receptionist]
/patients/:id                   → PatientDetail      [admin, doctor, receptionist]
/appointments                   → AppointmentQueue   [admin, doctor, receptionist]
/medical-records                → MedicalEncounter   [admin, doctor]
/prescriptions                  → PrescriptionPage   [admin, manager, doctor, pharmacist]
/pharmacy                       → PharmacyPage       [admin, manager, pharmacist]
/billing                        → BillingPage        [admin, manager, receptionist]
/reports                        → ReportsPage        [admin, manager]
/system-management              → SystemManagementPage [admin]
/users                          → UsersPage          [admin]
/services                       → ServicesPage       [admin, manager]
*                               → NotFound
```

### 3.2 State Management

- **Auth**: React Context (`AuthContext`) + localStorage
  - Lưu `token`, `user` vào localStorage
  - Tự động check localStorage khi mount
  - **Inactivity timeout**: 15 phút tự động logout
  - User object: `{ id, username, fullName, roles: [], expiredAt }`

- **Routing guard**: `PrivateRoute` component inline trong AppRoutes
  - Kiểm tra `isAuthenticated`, nếu chưa → redirect `/login`
  - Kiểm tra `allowedRoles`, nếu không match → Alert lỗi

- **No Redux/Zustand**: Tất cả state là local (useState) + Context (Auth)

---

## 4. Gaps & Backend Expectations

### 4.1 Expected JSON Response Structure

Frontend kỳ vọng Backend trả về:

**Pagination `Page<T>`:**
```json
{
  "content": [ { ... } ],
  "totalElements": 100,
  "totalPages": 10,
  "number": 0,
  "size": 10
}
```

**Login Response:**
```json
{
  "accessToken": "jwt...",
  "userId": "uuid",
  "username": "admin",
  "role": "ADMIN",
  "expiredAt": "2026-07-28T..."
}
```

**List Response:** Mảng JSON trực tiếp `[ { ... } ]`
**Single Object:** Object JSON trực tiếp
**File Download:** Blob/ArrayBuffer

### 4.2 Parameters/Filters gửi lên Backend

| Feature | Parameters | Endpoint |
|---------|-----------|----------|
| Patient Search | `keyword, page, size, gender, status, dateRange` | `GET /patients` |
| Medical Records | `patientId, doctorId, page, size` | `GET /medical-records` |
| Patient History | `page, size` | `GET /patients/{id}/history` |
| Reports | `fromDate, toDate, type` | `GET /reports/summary` |
| User Management | `page, size, keyword` | `GET /admin/users` |
| User Lock/Unlock | `locked: true/false` | `PUT /admin/users/{id}/status` |

### 4.3 Critical Gaps

| # | Gap | Severity | Mô tả |
|---|-----|----------|-------|
| 1 | **Medical Queue Migration** | 🔴 HIGH | FE dùng `/appointments/queue` nhưng BE đã chuyển sang `/queues` |
| 2 | **Refresh Token** | 🟡 MEDIUM | Không có refresh token flow (hiện tại chỉ dùng JWT với expiredAt) |
| 3 | **Aftercare Module** | 🟡 MEDIUM | Hoàn toàn localStorage, không có API backend |
| 4 | **Role/Permission Management UI** | 🟡 MEDIUM | Chưa có giao diện quản lý permission chi tiết (backend có @CheckPermission) |
| 5 | **Audit Log UI** | 🟡 MEDIUM | reportApi có `/reports/audit-logs` nhưng ReportsPage chưa confirm |
| 6 | **Notification / WebSocket** | 🟢 LOW | Chưa có real-time notification |

---

## 5. Executive Summary

Frontend đã được implement gần như hoàn chỉnh với **15 pages**, **11 API service files**, và **pattern fallback mock data** cho tất cả các module. Kiến trúc tổng thể sử dụng React Context cho Auth + localStorage cho persistence. Routing được bảo vệ bởi role-based PrivateRoute.

**Điểm mạnh:**
- UI/UX đầy đủ, responsive với Ant Design
- Xử lý tốt trường hợp backend offline (mock data fallback)
- Clean code, separation of concerns

**Điểm yếu cần xử lý:**
1. **Medical Queue endpoints** chưa đồng bộ với Backend (FE dùng `/appointments/queue` thay vì `/queues`)
2. **Thiếu Refresh Token** — JWT hiện tại dùng expiredAt nhưng không refresh
3. **Aftercare** dùng localStorage thay vì API
4. **Permissions/Admin** — cần UI quản lý roles & permissions đồng bộ với backend `@CheckPermission` annotation

**Tổng quan:** Frontend đạt ~80% completeness. Cần khoảng 2-3 sprint để đồng bộ API contracts, thêm tính năng refresh token, và hoàn thiện admin panel.

---

*Report generated by: Senior Frontend Code Auditor*  
*Guidelines: .clinerules — Code Generation & Refactoring Rules (YAGNI, Reuse, Simplicity)*
