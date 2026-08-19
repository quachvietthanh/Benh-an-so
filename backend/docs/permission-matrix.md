# Permission matrix — Giai đoạn C

Nguồn: controller, `SecurityConfig` và service authorization hiện tại. `Target permission` là code cần đặt trên `@RequirePermission`; tiền tố `NEW:` đánh dấu code được bổ sung vào catalog V2 ở Giai đoạn C. Các endpoint trên cùng một dòng có cùng permission và cùng contextual rule.

`Contextual rule` luôn chạy sau permission tổng quát. Không chuyển các rule này thành role check trong controller.

| HTTP path → controller method | Target permission | Contextual rule cần giữ | Trạng thái hiện tại |
|---|---|---|---|
| `POST /auth/login` → `AuthController.login`; `POST /auth/refresh` → `refresh` | Public | Xác thực mật khẩu/refresh session | Public |
| `POST /auth/logout` → `logout` | Authenticated session | Token hợp lệ, revoke session của token | Public trong config; cần tách ở C |
| `GET /` → `HomeController.home` | Authenticated | Không có | `anyRequest().authenticated()` |
| `GET /roles` → `RoleController.getSystemRoles` | `ROLE_READ` | Không có | Dynamic hoàn tất |
| `GET /permissions` → `RoleController.getPermissionCatalog` | `PERMISSION_READ` | Chỉ catalog active | Dynamic hoàn tất |
| `PUT /roles/{roleId}/permissions` → `RoleController.updateRolePermissions` | `ROLE_UPDATE` | AC-02: admin active duy nhất không tự mất quyền quản trị | Dynamic hoàn tất |
| `GET /reports/summary` → `getSummary`; `/visits-timeline` → `getVisitsTimeline`; `/top-medicines` → `getTopMedicines`; `/doctor-visits` → `getDoctorVisits` | `REPORT_VIEW` | Khoảng ngày hợp lệ, tối đa 366 ngày | Dynamic hoàn tất |
| `GET /reports/export` → `ReportsController.export` | `REPORT_EXPORT` | Khoảng ngày hợp lệ | Dynamic hoàn tất |
| `POST /users` → `UserController.create` | `USER_CREATE` | Username/email/role hợp lệ | Chuyển từ role |
| `GET /users`, `GET /users/{id}` → `getAll`, `getById` | `USER_READ` | Không có | Chuyển từ role |
| `GET /users/doctors` → `getDoctors` | `USER_READ` | Chỉ trả user role doctor/active theo use case | Chuyển từ role |
| `PUT /users/{id}` → `update`; `PATCH /users/{id}/activate` → `activate`; `PATCH /users/{id}/deactivate` → `deactivate` | `USER_UPDATE` | Không vô hiệu hóa/xử lý tài khoản trái business rule | Chuyển từ role |
| `POST /patients` → `PatientController.create` | `PATIENT_CREATE` | Dữ liệu định danh duy nhất | Chuyển từ role |
| `GET /patients`, `/patients/code/{code}`, `/patients/{patientId}` → `search`, `getByCode`, `getById` | `PATIENT_READ` | Phạm vi dữ liệu bệnh nhân nếu use case áp dụng | Chuyển từ role |
| `PUT /patients/{patientId}` → `update` | `PATIENT_UPDATE` | Kiểm tra dữ liệu định danh/trạng thái | Chuyển từ role |
| `GET /medical-history/patients/{patientId}` → `MedicalHistoryController.getByPatient` | `MEDICAL_RECORD_READ` | Giữ rule truy cập hồ sơ bệnh nhân | Chuyển từ role |
| `GET /visits/{visitId}/encounter` → `VisitController.getEncounter` | `MEDICAL_RECORD_READ` | ADMIN/NURSE đọc; DOCTOR chỉ khi là bác sĩ phụ trách visit | Permission + service context |
| `POST /medical-records` → `create`; `PUT /medical-records/{id}` → `update`; `PUT /medical-records/{id}/diagnoses` → `replaceDiagnoses`; `POST /medical-records/{id}/lock` → `lock`; `POST /medical-records/{id}/amendments` → `amend` | `MEDICAL_RECORD_CREATE` / `MEDICAL_RECORD_UPDATE` / `MEDICAL_RECORD_UPDATE_STATUS` (lock) | Giữ `MedicalRecordAuthorizationService`, trạng thái lock/amend và actor | Chuyển role tổng quát, giữ service context |
| `GET /medical-records/{id}`, `/{id}/diagnoses`, `/visits/{visitId}`, `/patient/{patientId}` → read methods | `MEDICAL_RECORD_READ` | Giữ phạm vi hồ sơ/bệnh nhân | Chuyển từ role |
| `GET /medical-records/{id}/access-logs`, `/access-logs` → access-log methods | `AUDIT_READ` | `requireAuditReadAccess` | Legacy enum permission |
| `POST /prescriptions` → `create` | `PRESCRIPTION_CREATE` | Chỉ bác sĩ phụ trách medical record/visit; kiểm tra tương tác và trạng thái | Permission + service context |
| `PATCH /prescriptions/{id}` → `update`; `POST /prescriptions/{id}/cancel` → `cancel` | `PRESCRIPTION_UPDATE` | Chỉ bác sĩ phụ trách visit; không sửa khi trạng thái không cho phép | Permission + service context |
| `GET /prescriptions`, `/{id}`, `/medical-records/{medicalRecordId}` → read methods | `PRESCRIPTION_READ` | DOCTOR chỉ đọc prescription của visit mình phụ trách; admin/pharmacist theo service | Permission + service context |
| `POST /prescriptions/{id}/dispense` → `dispense` | `PRESCRIPTION_UPDATE_STATUS` | Chỉ trạng thái có thể dispense; tồn kho/lot hợp lệ | Chuyển role tổng quát, giữ service context |
| `POST /prescriptions/check-interactions` → `checkInteractions` | `PRESCRIPTION_CREATE` | Chỉ kiểm tra lâm sàng, không ghi đơn | Chuyển từ role |
| `GET /medicines`, `/medicines/{medicineId}` → `search`, `getById` | `PHARMACY_READ` | Chỉ catalog active nếu use case yêu cầu | Chuyển từ role |
| `POST /medicines` → `create`; `PUT /medicines/{id}` → `update`; `PATCH /medicines/{id}/status` → `updateStatus` | `PHARMACY_CREATE` / `PHARMACY_UPDATE` | Giữ kiểm tra SKU, tồn tại, trạng thái | Chuyển role tổng quát, giữ authorizer |
| `GET /inventory/stocks`, `/batches`, `/low-stock`, `/expiry-alerts` → inventory read methods | `PHARMACY_READ` | Giữ filter tồn kho/hạn dùng | Chuyển từ role |
| `POST /inventory/receipts` → `InventoryReceiptController.receiveStock` | `PHARMACY_CREATE` | Lô hàng, số lượng, người nhận | Chuyển role tổng quát, giữ authorizer |
| `POST /appointments` → `AppointmentController.create` | `APPOINTMENT_CREATE` | Bác sĩ tồn tại/active, không trùng lịch; ADMIN/RECEPTIONIST business authorization | Permission + service context |
| `GET /appointments`, `/{id}`, `/overdue` → read methods | `APPOINTMENT_READ` | Phạm vi lịch hẹn/bác sĩ nếu use case áp dụng | Chuyển từ role |
| `POST /appointments/{id}/reminder`; `PATCH /appointments/{id}/cancel`, `/{id}/no-show` | `APPOINTMENT_UPDATE` | Chỉ transition trạng thái hợp lệ; ADMIN/RECEPTIONIST business authorization | Permission + service context |
| `GET /queues` → `QueueController.getQueues`; `GET /queue-items/{itemId}` → `getById` | `QUEUE_VIEW` | Phạm vi ngày/bác sĩ/queue | Chuyển từ role |
| `GET /queues/me` → `getMyQueue` | `QUEUE_VIEW` | Bắt buộc DOCTOR; query doctorId = current user | Permission + service context |
| `POST /appointments/{appointmentId}/check-in`; `POST /queue-items/walk-in` | `QUEUE_CREATE` | Hẹn/queue ở trạng thái có thể check-in; ADMIN/RECEPTIONIST business authorization | Permission + service context |
| `POST /queues/{queueId}/call-next` → `callNext` | `QUEUE_CALL_NEXT` | Queue mở, bác sĩ/room/queue item phù hợp | Permission + service context |
| `PATCH /queue-items/{itemId}/status`; `POST /queue-items/{itemId}/complete`; `POST /queue-items/{itemId}/skip` | `QUEUE_UPDATE_STATUS` | Chỉ transition hợp lệ; bác sĩ phụ trách/role workflow theo `QueueOperationAuthorization` | Permission + service context |
| `POST /clinical-orders/visits/{visitId}` → `ClinicalOrderController.create` | `NEW: CLINICAL_ORDER_CREATE` | Giữ `ClinicalOrderAuthorizationService.requireWriteAccess`, visit/record state | New catalog + service context |
| `GET /clinical-orders/visits/{visitId}` → `getByVisitId` | `NEW: CLINICAL_ORDER_READ` | Giữ `requireReadAccess` | New catalog + service context |
| `POST /clinical-order-items/{itemId}/results`; `PUT /clinical-results/{id}`; `POST /clinical-results/{id}/finalize` | `NEW: CLINICAL_RESULT_CREATE` / `NEW: CLINICAL_RESULT_UPDATE` / `NEW: CLINICAL_RESULT_FINALIZE` | Giữ write access, actor, finalize-state rule | New catalog + service context |
| `GET /clinical-results/{id}`, `/visits/{visitId}`, `/{id}/history` | `NEW: CLINICAL_RESULT_READ` | Giữ clinical read access | New catalog + service context |
| `POST /clinical-results/{resultId}/attachments`; `GET /clinical-result-attachments/{attachmentId}/download` | `NEW: CLINICAL_RESULT_ATTACHMENT_CREATE` / `NEW: CLINICAL_RESULT_ATTACHMENT_READ` | Giữ clinical read/write access, file validation | New catalog + service context |
| `GET /clinical-services` → `ClinicalServiceCatalogController.search` | `NEW: CLINICAL_SERVICE_READ` | Giữ clinical read access | New catalog + service context |
| `GET /diagnosis-catalog` → `search` | `DIAGNOSIS_READ` | Không có | Chuyển từ role |
| `POST /invoices`, `/payments`, `/payment-quotes` → create/payment methods | `INVOICE_CREATE` | Giữ invoice/payment state, actor, ADMIN/RECEPTIONIST workflow | Permission + service context |
| `GET /invoices`, `/{invoiceId}`, `/payable` → read methods | `INVOICE_READ` | Phạm vi invoice nếu use case áp dụng | Legacy enum permission |
| `POST /invoices/payments/{paymentId}/refund`; `POST /invoices/{invoiceId}/adjustments` | `INVOICE_UPDATE` | Giữ monetary state and manager workflow until replaced by explicit policy | Permission + service context |
| `GET /system/services`, `/{id}`, `/{id}/prices` → service catalog read methods | `SERVICE_CATALOG_READ` | Không có | Legacy enum permission |
| `POST /system/services` → `create` | `SERVICE_CATALOG_CREATE` **AND** `SERVICE_PRICE_MANAGE` | Giá khởi tạo hợp lệ | Legacy enum permission |
| `PUT /system/services/{id}` → `update` | `SERVICE_CATALOG_UPDATE` **AND** `SERVICE_PRICE_MANAGE` | Giá/hiệu lực hợp lệ | Legacy enum permission |
| `PATCH /system/services/{id}/status` → `updateStatus` | `SERVICE_CATALOG_UPDATE` | Không vô hiệu hóa trái rule catalogue | Legacy enum permission |
| `GET /dashboard/operational` → `OperationalDashboardController.getOperational` | `NEW: DASHBOARD_OPERATIONAL_READ` | Khoảng ngày và aggregate scope | New catalog; service role rule phải tách |
| `POST /follow-up-reminders`; `PATCH /follow-up-reminders/{id}/status` | `NEW: FOLLOW_UP_REMINDER_CREATE` / `NEW: FOLLOW_UP_REMINDER_UPDATE` | Rule due date/status; receptionist workflow | New catalog + service context |
| `GET /follow-up-reminders`, `/due` | `NEW: FOLLOW_UP_REMINDER_READ` | Filter due/status | New catalog + service context |
| `POST /care-logs`; `GET /care-logs`, `/patient/{patientId}` | `NEW: CARE_LOG_CREATE` / `NEW: CARE_LOG_READ` | Giữ staff/clinical context và audit actor | New catalog + service context |
| `GET /rooms`, `/{roomId}` | `NEW: ROOM_READ` | Giữ room availability/active filters | New catalog + service context |
| `POST /rooms`; `PUT /rooms/{roomId}`; `PATCH /rooms/{roomId}/activate`, `/{roomId}/deactivate` | `NEW: ROOM_CREATE` / `NEW: ROOM_UPDATE` | Giữ room state, không xung đột workflow | New catalog + service context |
| `GET /doctor-room-assignments` | `NEW: ROOM_ASSIGNMENT_READ` | Không có | New catalog |
| `PUT /doctors/{doctorId}/room-assignment`; `DELETE /doctors/{doctorId}/room-assignment` | `NEW: ROOM_ASSIGNMENT_UPDATE` | Phòng/bác sĩ hợp lệ; không đổi khi queue open | New catalog + service context |
| `GET /system/clinic`; `PUT /system/clinic` | `NEW: CLINIC_CONFIGURATION_READ` / `NEW: CLINIC_CONFIGURATION_UPDATE` | Cấu hình hợp lệ, audit actor | New catalog |
| `POST /backups`; `GET /backups`, `/{id}`, `/{id}/download`; `POST /backups/{id}/restore` | `NEW: BACKUP_CREATE` / `NEW: BACKUP_READ` / `NEW: BACKUP_RESTORE` | Giữ backup integrity/restore lock và audit; `BackupAuthorizer` phải thay role policy riêng | New catalog + service context |

## Quy tắc thực hiện Giai đoạn C

1. Các code `NEW:` đã được seed trong V2; không reuse một code không cùng nghiệp vụ chỉ để tránh thêm permission.
2. Với mỗi dòng, thêm `@RequirePermission` tại controller và đổi matcher tương ứng ở `SecurityConfig` thành `authenticated()`.
3. Không xóa `VisitEncounterAuthorization`, `MedicalRecordAuthorizationService`, `Prescription*Validator`, `QueueOperationAuthorization` hoặc state validation. Chúng là authorization/validation theo dữ liệu, không phải RBAC menu-level.
4. Các authorizer hiện dùng `CurrentUserPort.hasRole(...)` cho workflow (backup, appointment, follow-up, inventory, room, billing...) cần được review riêng: thay bằng permission khi đó là quyền tổng quát; chỉ giữ lại điều kiện gắn với actor/resource/state.
