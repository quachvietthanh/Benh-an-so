# Invoice Discount And Free Approval API Contract

> Phân hệ: **NCL-07 - Thu ngân và Xuất hóa đơn**
>
> User Story: **NCL-07-CN-008 - Giảm giá và miễn phí có phê duyệt**
>
> Quy tắc nghiệp vụ liên quan: **QTN-37 - Kiểm soát giảm giá/miễn phí**
>
> Acceptance Criteria: **AC TC-01, TC-02, TC-03, TC-04**
>
> Base URL: `http://localhost:8080/api/v1`
>
> Auth: Bearer Token (JWT)
>
> Trạng thái: **Official Baseline**

---

## 1. Phạm Vi Nghiệp Vụ (Scope)

Tài liệu này chuẩn hóa API contract cho chức năng **Đề xuất, Phê duyệt, Từ chối và Áp dụng Giảm giá / Miễn phí viện phí** (`NCL-07-CN-008`):
1. **Lập đề xuất giảm giá/miễn phí**: Thu ngân lập yêu cầu theo tỷ lệ (`PERCENTAGE`), số tiền cố định (`FIXED_AMOUNT`), hoặc miễn phí 100% (`FULL_FREE`).
2. **Khóa thanh toán/xuất hóa đơn khi đang chờ duyệt (QTN-37 / AC TC-01)**: Chặn thanh toán và chặn phát hành hóa đơn khi có yêu cầu giảm giá ở trạng thái `PENDING`.
3. **Phê duyệt giảm giá (QTN-37 / AC TC-02)**: Quản lý/Trưởng phòng tài chính phê duyệt; tự động điều chỉnh số tiền phải thanh toán và xuất hóa đơn phản ánh dòng giảm trừ (`DISCOUNT`).
4. **Từ chối giảm giá (QTN-37 / AC TC-04)**: Quản lý từ chối với lý do bắt buộc; hệ thống giữ nguyên số tiền gốc ban đầu.
5. **Chặn tự phê duyệt và Ghi nhận Audit Log (QTN-37 / AC TC-03)**: Cấm người đề xuất tự duyệt hoặc tự từ chối yêu cầu của chính mình; vi phạm kích hoạt ghi `AuditLog` loại `ACCESS_DENIED` độc lập qua `REQUIRES_NEW`.

---

## 2. Vai Trò & Phân Quyền (Roles & Permissions)

| Hành động | Quyền yêu cầu (`Permission`) | Vai trò mặc định | Mã lỗi vi phạm |
| :--- | :--- | :--- | :--- |
| Tạo đề xuất giảm giá | `INVOICE_CREATE` | `RECEPTIONIST`, `ADMIN`, `MANAGER` (không cho phép `DOCTOR`) | `403 Forbidden` / `401 Unauthorized` |
| Tra cứu danh sách/chi tiết đề xuất | `INVOICE_READ` | `RECEPTIONIST`, `MANAGER`, `ADMIN` | `403 Forbidden` / `401 Unauthorized` |
| Phê duyệt yêu cầu giảm giá | `INVOICE_UPDATE` | `MANAGER`, `ADMIN` | `403 Forbidden` / `401 Unauthorized` |
| Từ chối yêu cầu giảm giá | `INVOICE_UPDATE` | `MANAGER`, `ADMIN` | `403 Forbidden` / `401 Unauthorized` |

> **Nguyên tắc Tách biệt Trách nhiệm (SoD - QTN-37):**
> Khi `approverId == requesterId`, dù tài khoản có quyền `INVOICE_UPDATE` (hoặc `ADMIN`), hệ thống lập tức từ chối với mã lỗi `403 Forbidden` (`SELF_APPROVAL_NOT_ALLOWED`) và ghi nhận `AuditLog` cảnh báo bảo mật (`ACCESS_DENIED`).

---

## 3. Danh Sách Endpoint

### 3.1 Tạo đề xuất giảm giá / miễn phí

- **Endpoint:** `POST /api/v1/invoices/discount-requests`
- **Quyền:** `@RequirePermission("INVOICE_CREATE")`
- **Request Headers:**
  - `Authorization: Bearer <access_token>`
  - `Content-Type: application/json`

#### Request Body
```json
{
  "visitId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "discountType": "PERCENTAGE",
  "discountPercent": 15.0,
  "discountAmount": null,
  "reason": "Bệnh nhân có hoàn cảnh khó khăn, gia đình chính sách"
}
```

#### Quy tắc Validation
- `visitId`: Bắt buộc, UUID hợp lệ, lượt khám phải tồn tại.
- `discountType`: Bắt buộc, thuộc `[PERCENTAGE, FIXED_AMOUNT, FULL_FREE]`.
- Nếu `PERCENTAGE`: `discountPercent` bắt buộc, `0 < discountPercent <= 100`.
- Nếu `FIXED_AMOUNT`: `discountAmount` bắt buộc, `discountAmount > 0` và không được vượt quá tổng viện phí chưa giảm.
- Nếu `FULL_FREE`: `discountPercent` tự động chuẩn hóa về `100.0`.
- `reason`: Bắt buộc, không được trống hoặc chỉ chứa khoảng trắng, tối đa 500 ký tự.
- **Tính duy nhất**: Mỗi lượt khám chỉ được có tối đa 1 yêu cầu giảm giá đang hoạt động (`PENDING` hoặc `APPROVED`). Nếu có, trả về `409 Conflict`.

#### Response (201 Created)
```json
{
  "success": true,
  "data": {
    "id": "11111111-2222-3333-4444-555555555555",
    "visitId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "discountType": "PERCENTAGE",
    "discountPercent": 15.0,
    "discountAmount": 150000.00,
    "originalAmount": 1000000.00,
    "finalAmount": 850000.00,
    "status": "PENDING",
    "reason": "Bệnh nhân có hoàn cảnh khó khăn, gia đình chính sách",
    "requestedBy": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "approvedBy": null,
    "rejectedBy": null,
    "rejectReason": null,
    "invoiceId": null,
    "createdAt": "2026-09-21T21:00:00Z",
    "updatedAt": "2026-09-21T21:00:00Z"
  },
  "message": "Tạo đề xuất giảm giá thành công",
  "timestamp": "2026-09-21T21:00:00Z"
}
```

---

### 3.2 Tra cứu danh sách đề xuất giảm giá

- **Endpoint:** `GET /api/v1/invoices/discount-requests`
- **Quyền:** `@RequirePermission("INVOICE_READ")`
- **Query Parameters:**
  - `requestedFrom` (Instant, tùy chọn): Thời gian bắt đầu tìm kiếm (ISO-8601).
  - `requestedTo` (Instant, tùy chọn): Thời gian kết thúc tìm kiếm (ISO-8601).
  - `visitId` (UUID, tùy chọn): Lọc theo lượt khám.
  - `status` (`PENDING` | `APPROVED` | `REJECTED`, tùy chọn): Lọc theo trạng thái.
  - `page` (int, mặc định `0`, `>= 0`): Chỉ số trang bắt đầu từ 0.
  - `size` (int, mặc định `20`, `1 <= size <= 100`): Kích thước trang.

#### Quy tắc Validation:
- `requestedFrom` và `requestedTo`: Nếu cả hai được cung cấp, `requestedFrom` phải trước hoặc bằng `requestedTo`. Nếu vi phạm, trả về `400 Bad Request` (`VALIDATION_FAILED`) kèm thông báo: `"Thời gian bắt đầu tìm kiếm phải trước hoặc bằng thời gian kết thúc."`.
- `page` và `size`: `page >= 0` và `1 <= size <= 100`. Nếu vi phạm, trả về `400 Bad Request` (`VALIDATION_FAILED`) kèm thông báo: `"Chỉ số trang không được âm và kích thước trang phải từ 1 đến 100."`.

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "11111111-2222-3333-4444-555555555555",
        "visitId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
        "discountType": "PERCENTAGE",
        "discountPercent": 15.0,
        "discountAmount": 150000.00,
        "originalAmount": 1000000.00,
        "finalAmount": 850000.00,
        "status": "PENDING",
        "reason": "Bệnh nhân có hoàn cảnh khó khăn",
        "requestedBy": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "approvedBy": null,
        "rejectedBy": null,
        "rejectReason": null,
        "invoiceId": null,
        "createdAt": "2026-09-21T21:00:00Z",
        "updatedAt": "2026-09-21T21:00:00Z"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 20,
    "number": 0
  },
  "message": "Lấy danh sách đề xuất giảm giá thành công",
  "timestamp": "2026-09-21T21:00:00Z"
}
```

---

### 3.3 Lấy chi tiết đề xuất giảm giá

- **Endpoint:** `GET /api/v1/invoices/discount-requests/{id}`
- **Quyền:** `@RequirePermission("INVOICE_READ")`

#### Response (200 OK)
Trả về thông tin chi tiết của đề xuất với cấu trúc tương tự `data` ở mục 3.1.
- Trường hợp không tìm thấy: Trả về `404 Not Found` (`DISCOUNT_REQUEST_NOT_FOUND`).

---

### 3.4 Phê duyệt đề xuất giảm giá (QTN-37 / AC TC-02)

- **Endpoint:** `POST /api/v1/invoices/discount-requests/{id}/approve`
- **Quyền:** `@RequirePermission("INVOICE_UPDATE")`
- **Request Body:** Không có.
- **Ràng buộc:**
  - Trạng thái yêu cầu phải là `PENDING`.
  - Người thực hiện (`approverId`) **không được trùng** với người đề xuất (`requestedBy`). Nếu trùng:
    - Trả về `400 Bad Request` với mã `SELF_APPROVAL_NOT_ALLOWED`.
    - Ghi nhận `AuditLog` cảnh báo (`actionType = ACCESS_DENIED`, `resourceType = DISCOUNT_REQUEST`).

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "id": "11111111-2222-3333-4444-555555555555",
    "visitId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "discountType": "PERCENTAGE",
    "discountPercent": 15.0,
    "discountAmount": 150000.00,
    "originalAmount": 1000000.00,
    "finalAmount": 850000.00,
    "status": "APPROVED",
    "reason": "Bệnh nhân có hoàn cảnh khó khăn",
    "requestedBy": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "approvedBy": "b2c3d4e5-f6a7-8901-bcde-fa2345678901",
    "approvedAt": "2026-09-21T21:05:00Z",
    "rejectedBy": null,
    "rejectReason": null,
    "invoiceId": null,
    "createdAt": "2026-09-21T21:00:00Z",
    "updatedAt": "2026-09-21T21:05:00Z"
  },
  "message": "Phê duyệt giảm giá thành công",
  "timestamp": "2026-09-21T21:05:00Z"
}
```

---

### 3.5 Từ chối đề xuất giảm giá (QTN-37 / AC TC-04)

- **Endpoint:** `POST /api/v1/invoices/discount-requests/{id}/reject`
- **Quyền:** `@RequirePermission("INVOICE_UPDATE")`
- **Request Headers:** `Content-Type: application/json`

#### Request Body
```json
{
  "rejectReason": "Không đáp ứng tiêu chuẩn đối tượng miễn giảm theo quy chế số 12/QC-BV"
}
```

#### Quy tắc Validation
- `rejectReason`: Bắt buộc, không để trống, tối đa 500 ký tự.
- Trạng thái yêu cầu phải là `PENDING`.
- Người thực hiện (`rejecterId`) **không được trùng** với người đề xuất (`requestedBy`).

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "id": "11111111-2222-3333-4444-555555555555",
    "visitId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "discountType": "PERCENTAGE",
    "discountPercent": 15.0,
    "discountAmount": 150000.00,
    "originalAmount": 1000000.00,
    "finalAmount": 1000000.00,
    "status": "REJECTED",
    "reason": "Bệnh nhân có hoàn cảnh khó khăn",
    "requestedBy": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "approvedBy": null,
    "rejectedBy": "b2c3d4e5-f6a7-8901-bcde-fa2345678901",
    "rejectReason": "Không đáp ứng tiêu chuẩn đối tượng miễn giảm theo quy chế số 12/QC-BV",
    "rejectedAt": "2026-09-21T21:06:00Z",
    "invoiceId": null,
    "createdAt": "2026-09-21T21:00:00Z",
    "updatedAt": "2026-09-21T21:06:00Z"
  },
  "message": "Từ chối giảm giá thành công",
  "timestamp": "2026-09-21T21:06:00Z"
}
```

---

## 4. Tác Động Lên Thanh Toán Và Hóa Đơn

### 4.1 Quy trình ghi nhận thanh toán (`POST /api/v1/billing/payments`)
1. **Kiểm tra trạng thái giảm giá**:
   - Nếu lượt khám tồn tại đề xuất giảm giá ở trạng thái `PENDING`: Hệ thống trả về `409 Conflict` kèm thông điệp:
     `"Lượt khám đang có yêu cầu giảm giá chờ phê duyệt, không thể thực hiện thanh toán."` (`PENDING_DISCOUNT_APPROVAL`).
   - Nếu có đề xuất giảm giá `APPROVED`: Số tiền phải thu được khấu trừ chính xác `discountAmount`. Với trường hợp miễn phí 100% (`FULL_FREE`), thanh toán được ghi nhận hợp lệ với `amountPaid = 0.00`.
   - Lưu trữ `discountAmount` và `discountRequestId` trên bản ghi thanh toán `payments`.

### 4.2 Quy trình phát hành hóa đơn (`POST /api/v1/invoices`)
1. **Kiểm tra trạng thái giảm giá**:
   - Tương tự thanh toán, hóa đơn bị chặn phát hành nếu có yêu cầu `PENDING`.
2. **Thêm dòng giảm trừ chi phí**:
   - Khi có đề xuất `APPROVED`, hóa đơn tự động sinh thêm 1 dòng `InvoiceLine` với:
     - `lineType`: `DISCOUNT`
     - `description`: `"Giảm giá / Miễn phí viện phí (Mã yêu cầu: ...)"`
     - `unitPrice`: `-discountAmount` (số âm)
     - `quantity`: `1`
     - `totalAmount`: `-discountAmount` (số âm)
   - `invoices.discount_amount` được cập nhật và liên kết 2 chiều với `invoices.discount_request_id`.
   - Cột `discount_requests.invoice_id` được cập nhật tham chiếu ngược tới hóa đơn phát hành.

---

## 5. Bảng Mã Lỗi (Error Codes)

| HTTP Status | Mã lỗi (`code`) | Mô tả lỗi |
| :--- | :--- | :--- |
| `400 Bad Request` | `VALIDATION_FAILED` | Dữ liệu đầu vào không hợp lệ (sai định dạng, thiếu trường bắt buộc, vượt độ dài). |
| `403 Forbidden` | `SELF_APPROVAL_NOT_ALLOWED` | Người đề xuất không được phép tự phê duyệt hoặc tự từ chối yêu cầu của chính mình (QTN-37 / AC TC-03). |
| `400 Bad Request` | `DISCOUNT_EXCEEDS_TOTAL` | Số tiền giảm giá vượt quá tổng viện phí chưa giảm của lượt khám. |
| `401 Unauthorized` | `UNAUTHORIZED` | Người dùng chưa đăng nhập hoặc token hết hạn. |
| `403 Forbidden` | `ACCESS_DENIED` | Người dùng không có quyền thực hiện nghiệp vụ (`INVOICE_CREATE`, `INVOICE_READ`, `INVOICE_UPDATE`). |
| `404 Not Found` | `DISCOUNT_REQUEST_NOT_FOUND` | Không tìm thấy đề xuất giảm giá theo `id` cung cấp. |
| `404 Not Found` | `VISIT_NOT_FOUND` | Lượt khám `visitId` không tồn tại trong hệ thống. |
| `409 Conflict` | `PENDING_DISCOUNT_APPROVAL` | Lượt khám đang có yêu cầu giảm giá chờ duyệt; chặn thanh toán và chặn phát hành hóa đơn (QTN-37 / AC TC-01). |
| `409 Conflict` | `DISCOUNT_ALREADY_EXISTS` | Lượt khám đã có đề xuất giảm giá đang chờ duyệt hoặc đã được phê duyệt. |
| `409 Conflict` | `INVALID_DISCOUNT_STATE` | Không thể phê duyệt/từ chối do đề xuất không ở trạng thái `PENDING`. |

---

## 6. Audit Logging (Kiểm toán bảo mật)

Mọi nỗ lực tự phê duyệt vi phạm quy tắc QTN-37 đều được ghi log kiểm toán bắt buộc:
- **`actionType`**: `ACCESS_DENIED`
- **`resourceType`**: `DISCOUNT_REQUEST`
- **`resourceId`**: ID của yêu cầu giảm giá
- **`description`**: Mô tả nỗ lực tự phê duyệt trái quy tắc
- **Cơ chế ghi**: Sử dụng transaction riêng biệt `Propagation.REQUIRES_NEW`, bảo đảm log vẫn được lưu vào cơ sở dữ liệu ngay cả khi giao dịch nghiệp vụ bị rollback do ngoại lệ.
