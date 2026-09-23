# Hợp đồng API: Dự trù mua thuốc và phiếu đặt hàng (NCL-06-CN-012)

> **Mã User Story:** `NCL-06-CN-012`  
> **Phân hệ:** `NCL-06` — Quản lý kho thuốc và cấp phát  
> **Quy tắc liên quan:** `QTN-06` (Không cấp phát vượt tồn kho), `QTN-01` (Phân quyền theo vai trò), `QTN-31` (Ghi nhật ký kiểm toán), Quy tắc phân tách nhiệm vụ (Separation of Duties - SoD), Quy tắc bất biến chứng từ (Immutability)  
> **Tiêu chí chấp nhận:** `NCL-06-CN-012-TC-01`, `NCL-06-CN-012-TC-02`, `NCL-06-CN-012-TC-03`  
> **Tiền tố đường dẫn (Base URL):** `/inventory/procurements`

---

## 1. Tổng quan & Ma trận phân quyền

Chức năng cung cấp các điểm cuối (endpoints) cho phép Dược sĩ xem gợi ý số lượng thuốc cần mua dựa trên tồn khả dụng, ngưỡng tồn tối thiểu và lượng tiêu thụ kỳ trước; lập, điều chỉnh và gửi duyệt phiếu dự trù; Quản lý phòng khám xem xét, phê duyệt hoặc từ chối phiếu dự trù.

### 1.1. Ma trận phân quyền RBAC

| Endpoint | Phương thức | Mô tả chức năng | Quyền yêu cầu (`@RequirePermission`) | Vai trò được phép |
| :--- | :---: | :--- | :--- | :--- |
| `/inventory/procurements/suggestions` | `GET` | Xem gợi ý số lượng cần mua | `MEDICATION_PROCUREMENT_READ` | `PHARMACIST`, `MANAGER`, `ADMIN` |
| `/inventory/procurements` | `POST` | Tạo phiếu dự trù mới (nháp hoặc gửi duyệt) | `MEDICATION_PROCUREMENT_CREATE` | `PHARMACIST`, `ADMIN` |
| `/inventory/procurements` | `GET` | Tra cứu danh sách phiếu dự trù | `MEDICATION_PROCUREMENT_READ` | `PHARMACIST`, `MANAGER`, `ADMIN` |
| `/inventory/procurements/{id}` | `GET` | Xem chi tiết phiếu dự trù | `MEDICATION_PROCUREMENT_READ` | `PHARMACIST`, `MANAGER`, `ADMIN` |
| `/inventory/procurements/{id}` | `PUT` | Cập nhật phiếu dự trù (`DRAFT` / `PENDING_APPROVAL`) | `MEDICATION_PROCUREMENT_CREATE` | `PHARMACIST`, `ADMIN` |
| `/inventory/procurements/{id}/submit` | `POST` | Gửi duyệt phiếu dự trù đang nháp | `MEDICATION_PROCUREMENT_CREATE` | `PHARMACIST`, `ADMIN` |
| `/inventory/procurements/{id}/cancel` | `POST` | Hủy phiếu dự trù (`DRAFT` / `PENDING_APPROVAL`) | `MEDICATION_PROCUREMENT_CREATE` | `PHARMACIST`, `ADMIN` |
| `/inventory/procurements/{id}/approve` | `POST` | Phê duyệt phiếu dự trù (`PENDING_APPROVAL`) | `MEDICATION_PROCUREMENT_APPROVE` | `MANAGER`, `ADMIN` |
| `/inventory/procurements/{id}/reject` | `POST` | Từ chối phiếu dự trù (`PENDING_APPROVAL`) | `MEDICATION_PROCUREMENT_APPROVE` | `MANAGER`, `ADMIN` |

> [!NOTE]
> Người dùng mang vai trò `RECEPTIONIST`, `DOCTOR`, hoặc `PATIENT` không có bất kỳ quyền nào trong bảng trên. Khi truy cập sẽ nhận mã lỗi `403 Forbidden` và hệ thống tự động ghi nhận nhật ký `ACCESS_DENIED` vào `audit_logs` (`TC-03`).

### 1.2. Quy tắc Phân tách nhiệm vụ (Separation of Duties - SoD)
- Người tạo phiếu (`createdBy`) **tuyệt đối không được phép tự phê duyệt hoặc tự từ chối phiếu** của chính mình, kể cả khi tài khoản đó có quyền `MEDICATION_PROCUREMENT_APPROVE` (hoặc vai trò `ADMIN`).
- Nếu vi phạm, hệ thống ném ngoại lệ `SelfProcurementApprovalNotAllowedException` và trả về mã lỗi `403 Forbidden`.

### 1.3. Quy tắc Bất biến chứng từ (Data Immutability)
- Khi phiếu đã ở trạng thái `APPROVED` hoặc `REJECTED`, phiếu không thể bị sửa đổi, xóa, hủy hoặc duyệt lại. Mọi thao tác thay đổi trạng thái sẽ trả về lỗi `409 Conflict`.

---

## 2. Chi tiết các Endpoints

### 2.1. Lấy danh sách gợi ý số lượng cần mua
* **Đường dẫn:** `GET /inventory/procurements/suggestions`
* **Quyền yêu cầu:** `MEDICATION_PROCUREMENT_READ`
* **Tham số Query:**
  - `from` *(tùy chọn, LocalDate yyyy-MM-dd)*: Ngày bắt đầu kỳ tham chiếu tiêu thụ (mặc định: 30 ngày trước).
  - `to` *(tùy chọn, LocalDate yyyy-MM-dd)*: Ngày kết thúc kỳ tham chiếu tiêu thụ (mặc định: ngày hiện tại).
  - `onlyBelowThreshold` *(tùy chọn, boolean, mặc định `true`)*: Chỉ lấy các thuốc có tồn khả dụng thấp hơn ngưỡng tồn tối thiểu hoặc có số lượng gợi ý > 0.
* **Công thức gợi ý:**
  $$\text{Số lượng gợi ý} = \max\Big(0,\ (\text{Lượng cấp phát kỳ trước} + \text{Tồn tối thiểu}) - \text{Tồn khả dụng}\Big)$$
* **Phản hồi thành công (200 OK):**
```json
{
  "periodStartDate": "2026-08-24",
  "periodEndDate": "2026-09-23",
  "calculatedAt": "2026-09-23T08:00:00Z",
  "totalItems": 1,
  "items": [
    {
      "medicineId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "medicineCode": "TH001",
      "medicineName": "Paracetamol 500mg",
      "unit": "Viên",
      "currentStock": 20,
      "eligibleStock": 20,
      "minStockThreshold": 50,
      "previousPeriodConsumption": 30,
      "suggestedQuantity": 60
    }
  ]
}
```

---

### 2.2. Lập phiếu dự trù mua thuốc
* **Đường dẫn:** `POST /inventory/procurements`
* **Quyền yêu cầu:** `MEDICATION_PROCUREMENT_CREATE`
* **Request Body:**
```json
{
  "periodStartDate": "2026-08-24",
  "periodEndDate": "2026-09-23",
  "note": "Dự trù định kỳ bổ sung thuốc tháng 09/2026",
  "submitImmediately": true,
  "items": [
    {
      "medicineId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "currentStock": 20,
      "minStockThreshold": 50,
      "previousPeriodConsumption": 30,
      "suggestedQuantity": 60,
      "proposedQuantity": 70,
      "note": "Tăng thêm 10 viên dự phòng"
    }
  ]
}
```
* **Phản hồi thành công (201 Created):**
```json
{
  "id": "c1f7535b-1662-436f-b2b9-291775e54d39",
  "planCode": "DT000001",
  "status": "PENDING_APPROVAL",
  "createdBy": "8b941578-8380-4cf8-a901-2db6d1c9ad95",
  "periodStartDate": "2026-08-24",
  "periodEndDate": "2026-09-23",
  "totalItems": 1,
  "totalSuggestedQuantity": 60,
  "totalProposedQuantity": 70,
  "totalApprovedQuantity": 0,
  "note": "Dự trù định kỳ bổ sung thuốc tháng 09/2026",
  "submittedAt": "2026-09-23T08:05:00Z",
  "approvedBy": null,
  "approvedAt": null,
  "rejectionReason": null,
  "createdAt": "2026-09-23T08:05:00Z",
  "updatedAt": "2026-09-23T08:05:00Z",
  "items": [
    {
      "id": "e2a3c710-53b1-4f12-9c44-d301bc1829e1",
      "planId": "c1f7535b-1662-436f-b2b9-291775e54d39",
      "medicineId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "medicineCode": "TH001",
      "medicineName": "Paracetamol 500mg",
      "unit": "Viên",
      "currentStock": 20,
      "minStockThreshold": 50,
      "previousPeriodConsumption": 30,
      "suggestedQuantity": 60,
      "proposedQuantity": 70,
      "approvedQuantity": 0,
      "note": "Tăng thêm 10 viên dự phòng",
      "createdAt": "2026-09-23T08:05:00Z"
    }
  ]
}
```

---

### 2.3. Tra cứu danh sách phiếu dự trù mua thuốc
* **Đường dẫn:** `GET /inventory/procurements`
* **Quyền yêu cầu:** `MEDICATION_PROCUREMENT_READ`
* **Tham số Query:**
  - `status` *(tùy chọn)*: `DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `REJECTED`, `CANCELLED`
  - `from` *(tùy chọn)*: Ngày tạo bắt đầu (yyyy-MM-dd)
  - `to` *(tùy chọn)*: Ngày tạo kết thúc (yyyy-MM-dd)
  - `createdBy` *(tùy chọn, UUID)*: Lọc theo người tạo
  - `page` *(tùy chọn, int, mặc định `0`)*
  - `size` *(tùy chọn, int, mặc định `20`)*
* **Phản hồi thành công (200 OK):**
```json
{
  "content": [
    {
      "id": "c1f7535b-1662-436f-b2b9-291775e54d39",
      "planCode": "DT000001",
      "status": "PENDING_APPROVAL",
      "createdBy": "8b941578-8380-4cf8-a901-2db6d1c9ad95",
      "periodStartDate": "2026-08-24",
      "periodEndDate": "2026-09-23",
      "totalItems": 1,
      "totalProposedQuantity": 70,
      "totalApprovedQuantity": 0,
      "submittedAt": "2026-09-23T08:05:00Z",
      "approvedBy": null,
      "approvedAt": null,
      "createdAt": "2026-09-23T08:05:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 2.4. Xem chi tiết phiếu dự trù
* **Đường dẫn:** `GET /inventory/procurements/{id}`
* **Quyền yêu cầu:** `MEDICATION_PROCUREMENT_READ`
* **Phản hồi thành công (200 OK):** Trả về đối tượng `ProcurementPlanResponse` đầy đủ thông tin cha và danh sách các dòng thuốc `items`.

---

### 2.5. Cập nhật phiếu dự trù
* **Đường dẫn:** `PUT /inventory/procurements/{id}`
* **Quyền yêu cầu:** `MEDICATION_PROCUREMENT_CREATE`
* **Ràng buộc:** Chỉ cho phép cập nhật khi phiếu ở trạng thái `DRAFT` hoặc `PENDING_APPROVAL`.
* **Request Body:**
```json
{
  "note": "Điều chỉnh bổ sung cơ số thuốc dự phòng",
  "items": [
    {
      "medicineId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "currentStock": 20,
      "minStockThreshold": 50,
      "previousPeriodConsumption": 30,
      "suggestedQuantity": 60,
      "proposedQuantity": 80,
      "note": "Điều chỉnh lên 80"
    }
  ]
}
```

---

### 2.6. Gửi duyệt phiếu dự trù nháp
* **Đường dẫn:** `POST /inventory/procurements/{id}/submit`
* **Quyền yêu cầu:** `MEDICATION_PROCUREMENT_CREATE`
* **Ràng buộc:** Phiếu phải ở trạng thái `DRAFT`. Chuyển sang `PENDING_APPROVAL`.

---

### 2.7. Hủy phiếu dự trù
* **Đường dẫn:** `POST /inventory/procurements/{id}/cancel`
* **Quyền yêu cầu:** `MEDICATION_PROCUREMENT_CREATE`
* **Ràng buộc:** Phiếu phải ở trạng thái `DRAFT` hoặc `PENDING_APPROVAL`. Chuyển sang `CANCELLED`.
* **Phản hồi:** `204 No Content`.

---

### 2.8. Phê duyệt phiếu dự trù
* **Đường dẫn:** `POST /inventory/procurements/{id}/approve`
* **Quyền yêu cầu:** `MEDICATION_PROCUREMENT_APPROVE`
* **Ràng buộc:**
  - Phiếu phải ở trạng thái `PENDING_APPROVAL`.
  - Người thực hiện không được trùng với người tạo phiếu (`actorId != plan.createdBy` - SoD).
* **Request Body (tùy chọn):**
```json
{
  "note": "Đồng ý phê duyệt dự trù thuốc",
  "itemAdjustments": {
    "3fa85f64-5717-4562-b3fc-2c963f66afa6": 65
  }
}
```
* **Phản hồi thành công (200 OK):** Trạng thái phiếu chuyển thành `APPROVED`. Số lượng duyệt được ghi nhận.

---

### 2.9. Từ chối phiếu dự trù
* **Đường dẫn:** `POST /inventory/procurements/{id}/reject`
* **Quyền yêu cầu:** `MEDICATION_PROCUREMENT_APPROVE`
* **Ràng buộc:**
  - Phiếu phải ở trạng thái `PENDING_APPROVAL`.
  - Người thực hiện không được trùng với người tạo phiếu (SoD).
  - Lý do từ chối `reason` là bắt buộc và tối thiểu 5 ký tự.
* **Request Body:**
```json
{
  "reason": "Vượt hạn mức ngân sách tháng 09/2026, đề nghị giảm số lượng thuốc không khẩn cấp."
}
```
* **Phản hồi thành công (200 OK):** Trạng thái phiếu chuyển thành `REJECTED`. Toàn bộ `approvedQuantity` đặt về 0.

---

## 3. Bảng mã lỗi nghiệp vụ chuẩn hóa (Error Codes)

| Mã lỗi HTTP | Mã lỗi Domain (`code`) | Thông báo mẫu (100% Tiếng Việt) |
| :---: | :--- | :--- |
| `400 Bad Request` | `VALIDATION_FAILED` | Ngày bắt đầu kỳ tham chiếu không được để trống / Danh sách thuốc không được để trống. |
| `400 Bad Request` | `PROCUREMENT_PLAN_EMPTY_ITEMS` | Phiếu dự trù mua thuốc phải có ít nhất một loại thuốc. |
| `400 Bad Request` | `PROCUREMENT_PLAN_DUPLICATE_MEDICINE` | Thuốc có mã ID {id} đã xuất hiện nhiều lần trong phiếu dự trù. |
| `401 Unauthorized` | `TOKEN_INVALID` | Phiên đăng nhập không hợp lệ hoặc đã hết hạn. |
| `403 Forbidden` | `ACCESS_DENIED` | Người dùng không có quyền truy cập chức năng này (Lễ tân/Bác sĩ bị chặn - TC-03). |
| `403 Forbidden` | `SELF_APPROVAL_NOT_ALLOWED` | Người lập phiếu dự trù mua thuốc không được tự phê duyệt hoặc từ chối phiếu của chính mình. |
| `404 Not Found` | `PROCUREMENT_PLAN_NOT_FOUND` | Không tìm thấy phiếu dự trù mua thuốc: {id}. |
| `404 Not Found` | `MEDICINE_NOT_FOUND` | Không tìm thấy thuốc có mã ID: {id}. |
| `409 Conflict` | `PROCUREMENT_PLAN_INVALID_STATUS` | Không thể thực hiện hành động này khi phiếu dự trù đang ở trạng thái: APPROVED. |
