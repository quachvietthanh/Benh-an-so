# NCL-06-CN-010 — Hợp đồng backend chức năng Điều chỉnh tồn kho và Hủy lô thuốc hết hạn

## 1. Phạm vi

Tài liệu này xác định toàn bộ hợp đồng API, phân quyền, quy tắc nghiệp vụ và ràng buộc cơ sở dữ liệu cho chức năng Điều chỉnh tồn kho và Hủy lô thuốc hết hạn (User Story `NCL-06-CN-010`). Phạm vi thực hiện chỉ bao gồm backend, không thay đổi frontend.

## 2. Quy tắc nghiệp vụ & Quyền hạn

- **Phân quyền (QTN-01 / NCL-06-CN-010-TC-04)**:
  - Chỉ vai trò Dược sĩ (`PHARMACIST`) và Quản trị viên (`ADMIN`) có quyền thực hiện thao tác điều chỉnh kho và hủy lô thuốc (`PERMISSION_PHARMACY_UPDATE`).
  - Lễ tân (`RECEPTIONIST`) và Bác sĩ (`DOCTOR`) bị từ chối truy cập với mã lỗi `403 Forbidden` và hệ thống tự động ghi nhật ký `ACCESS_DENIED` vào bảng `audit_logs` (thỏa mãn `NCL-06-CN-010-TC-04`).
- **Quy tắc điều chỉnh tồn kho sau kiểm kê (QTN-32 / NCL-06-CN-010-TC-01)**:
  - Dược sĩ nhập số lượng thực tế sau kiểm kê (`actualQuantity`) và lý do (`reason`).
  - `actualQuantity >= 0`. Nếu `actualQuantity = 0`, trạng thái lô chuyển thành `DEPLETED`. Nếu `actualQuantity > 0`, trạng thái lô giữ nguyên hoặc chuyển thành `ACTIVE`.
  - Không cho phép điều chỉnh trên lô thuốc đã bị hủy (`status = 'EXPIRED'`).
  - `actualQuantity` không được bằng với số lượng hiện tại của lô (chênh lệch khác 0 để bảo đảm toàn vẹn constraint `chk_stock_movements_non_zero_change`).
  - Ghi nhận biến động kho `StockMovement` với:
    - `movement_type = 'ADJUSTMENT'`
    - `reference_type = 'MANUAL_ADJUSTMENT'`
    - `reference_id = batch_id`
    - `quantity_before = tồn hiện tại`
    - `quantity_after = actualQuantity`
    - `quantity_change = actualQuantity - quantity_before`
    - `performed_by = ID người thực hiện`
    - `performed_at = thời điểm thực hiện`
    - `note = reason`
  - Đồng bộ tổng tồn kho trên danh mục thuốc `medicines.stock_quantity`.
  - Kích hoạt cập nhật cảnh báo tồn thấp (`LowStockAlertTransitionService`).
- **Quy tắc hủy lô thuốc hết hạn (QTN-14, QTN-32 / NCL-06-CN-010-TC-02)**:
  - Chỉ cho phép hủy đối với các lô thuốc đã quá hạn sử dụng (`expiryDate < today`). Nếu lô chưa quá hạn, từ chối thao tác với mã lỗi `400 Bad Request`.
  - Lô thuốc phải chưa bị hủy trước đó (`status != 'EXPIRED'`) và còn số lượng tồn (`quantity > 0`).
  - Sau khi hủy:
    - Trạng thái lô chuyển thành `EXPIRED`.
    - Số lượng của lô về `0`.
    - Lô bị loại hoàn toàn khỏi tồn kho khả dụng và không thể xuất hiện trong danh sách cấp phát thuốc (`findAvailableByMedicineIdForUpdate`).
  - Ghi nhận biến động kho `StockMovement` với:
    - `movement_type = 'EXPIRE'`
    - `reference_type = 'EXPIRY_PROCESS'`
    - `reference_id = batch_id`
    - `quantity_before = tồn trước khi hủy`
    - `quantity_after = 0`
    - `quantity_change = -quantity_before`
    - `performed_by = ID người thực hiện`
    - `performed_at = thời điểm thực hiện`
    - `note = reason`
  - Trừ tổng tồn kho danh mục thuốc `medicines.stock_quantity` tương ứng với số lượng lô bị hủy.
  - Kích hoạt cập nhật cảnh báo tồn thấp (`LowStockAlertTransitionService`).
- **Ràng buộc lý do bắt buộc (QTN-32 / NCL-06-CN-010-TC-03)**:
  - Cả 2 thao tác điều chỉnh và hủy lô đều bắt buộc phải cung cấp lý do (`reason`).
  - Trường `reason` không được null, không được rỗng hoặc chỉ chứa khoảng trắng (`@NotBlank`). Độ dài tối đa 500 ký tự.
  - Nếu thiếu lý do, hệ thống chặn ngay từ tầng validation và trả về `400 Bad Request`.
- **An toàn đồng thời (Concurrency Control)**:
  - Sử dụng khóa bi quan (`SELECT ... FOR UPDATE`) khi đọc lô thuốc trong transaction để tránh race condition giữa kiểm kê, hủy lô và cấp phát thuốc.

---

## 3. API Contract

### 3.1 Điều chỉnh tồn kho sau kiểm kê
- **Endpoint**: `POST /inventory/batches/{id}/adjust`
- **Quyền yêu cầu**: `@RequirePermission("PHARMACY_UPDATE")`
- **URL Path**: `{id}` là UUID của lô thuốc (`medicine_batches.id`).
- **Request Body**:
```json
{
  "actualQuantity": 85,
  "reason": "Điều chỉnh sau kiểm kê kho định kỳ cuối tháng 9/2026, phát hiện vỡ 15 viên"
}
```
- **Response `200 OK`**:
```json
{
  "batchId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "medicineId": "f1e2d3c4-b5a6-7890-1234-56789abcdef0",
  "medicineCode": "TH000001",
  "medicineName": "Paracetamol 500mg",
  "batchNumber": "BATCH-2026-001",
  "expiryDate": "2027-12-31",
  "quantityBefore": 100,
  "quantityAfter": 85,
  "quantityChange": -15,
  "status": "ACTIVE",
  "reason": "Điều chỉnh sau kiểm kê kho định kỳ cuối tháng 9/2026, phát hiện vỡ 15 viên",
  "performedBy": "3b2cb612-4cfc-40ad-a035-71cb14b09b55",
  "performedAt": "2026-09-21T08:30:00Z"
}
```

### 3.2 Hủy lô thuốc hết hạn
- **Endpoint**: `POST /inventory/batches/{id}/discard`
- **Quyền yêu cầu**: `@RequirePermission("PHARMACY_UPDATE")`
- **URL Path**: `{id}` là UUID của lô thuốc (`medicine_batches.id`).
- **Request Body**:
```json
{
  "reason": "Hủy lô thuốc hết hạn sử dụng ngày 15/09/2026 theo biên bản tiêu hủy số 12/BB-TH"
}
```
- **Response `200 OK`**:
```json
{
  "batchId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "medicineId": "f1e2d3c4-b5a6-7890-1234-56789abcdef0",
  "medicineCode": "TH000001",
  "medicineName": "Paracetamol 500mg",
  "batchNumber": "BATCH-2026-001",
  "expiryDate": "2026-09-15",
  "discardedQuantity": 50,
  "status": "EXPIRED",
  "reason": "Hủy lô thuốc hết hạn sử dụng ngày 15/09/2026 theo biên bản tiêu hủy số 12/BB-TH",
  "performedBy": "3b2cb612-4cfc-40ad-a035-71cb14b09b55",
  "performedAt": "2026-09-21T08:35:00Z"
}
```

---

## 4. Bảng mã lỗi phản hồi (Error Codes)

| HTTP Status | Error Code | Mô tả |
|---|---|---|
| `400 Bad Request` | `VALIDATION_FAILED` | Thiếu lý do điều chỉnh/hủy lô (`reason` null/blank), hoặc `actualQuantity < 0`, hoặc `actualQuantity == currentQuantity`. |
| `400 Bad Request` | `BATCH_NOT_EXPIRED` | Cố gắng hủy lô thuốc vẫn còn hạn sử dụng (`expiryDate >= today`). |
| `400 Bad Request` | `BATCH_ALREADY_DISCARDED` | Lô thuốc đã ở trạng thái `EXPIRED` hoặc số lượng tồn kho bằng 0. |
| `403 Forbidden` | `ACCESS_DENIED` | Người dùng không có quyền `PHARMACY_UPDATE` (ví dụ: Lễ tân, Bác sĩ). Hệ thống tự động ghi nhật ký `audit_logs`. |
| `404 Not Found` | `BATCH_NOT_FOUND` | Không tìm thấy lô thuốc với `id` cung cấp. |
| `409 Conflict` | `BATCH_STATE_CONFLICT` | Trạng thái lô không hợp lệ để điều chỉnh (lô đã bị hủy trước đó). |
