# Payable Encounters API Contract

> Module: **NCL-07 - Quản lý thu ngân và viện phí**
>
> User Story: **NCL-07-CN-006 - Danh sách lượt khám chưa thanh toán**
>
> Base URL: `http://localhost:8080/api/v1` (hoặc `/invoices/payable`)
>
> Auth: Bearer Token (JWT)
>
> Status: **Official Baseline**

---

## 1. Scope & Objective

Tài liệu này định nghĩa API contract cho chức năng **Danh sách lượt khám chưa thanh toán** (`NCL-07-CN-006`):
1. Cung cấp danh sách các lượt tiếp đón/khám bệnh đã hoàn thành quá trình khám (`COMPLETED`) nhưng chưa được thu tiền / chưa lập hóa đơn thu ngân.
2. Hỗ trợ lọc theo ngày hoàn thành khám bệnh (`date`), tìm kiếm theo từ khóa (`search`: họ tên bệnh nhân, mã bệnh nhân, mã tiếp đón), và phân trang (`page`, `size`).
3. Cung cấp thông tin chi phí dự kiến bóc tách gồm tiền khám (`examFee`), tiền thuốc (`medicineFee`), tiền dịch vụ cận lâm sàng (`serviceFee`), và tổng tiền dự kiến (`totalEstimatedAmount`).
4. Kiểm soát phân quyền chặt chẽ (`INVOICE_READ`) và tích hợp nhật ký kiểm toán (`AuditLog`) khi vi phạm truy cập.

---

## 2. Roles & Permissions

- **Allowed Roles:** `RECEPTIONIST`, `ADMIN`, `MANAGER` (các vai trò sở hữu quyền `INVOICE_READ`).
- **Required Permission:** `@RequirePermission("INVOICE_READ")`.
- **Unauthorized Roles:** `DOCTOR`, `PHARMACIST`, `PATIENT`, các tài khoản không có quyền `INVOICE_READ`.
  - Kết quả khi vi phạm: Trả về HTTP `403 Forbidden`.
  - Tự động ghi nhận `AuditLog` với `actionType = ACCESS_DENIED`, `resourceType = PERMISSION`.

---

## 3. Endpoint Specification

### 3.1 Request

```http
GET /invoices/payable?date=YYYY-MM-DD&search={keyword}&page=0&size=20
```

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `date` | `LocalDate` (ISO-8601: `YYYY-MM-DD`) | Không | `null` | Lọc theo ngày hoàn thành khám (`completedAt`) theo múi giờ `Asia/Ho_Chi_Minh`. Nếu không truyền, hệ thống hiển thị tất cả các ngày chưa thanh toán. |
| `search` | `String` | Không | `null` | Từ khóa tìm kiếm (không phân biệt chữ hoa thường) áp dụng trên: Họ và tên bệnh nhân (`patient.fullName`), Mã bệnh nhân (`patient.patientCode`), Mã tiếp đón (`visit.visitCode`). |
| `page` | `int` | Không | `0` | Chỉ số trang bắt đầu từ 0. Ràng buộc: `page >= 0`. |
| `size` | `int` | Không | `20` | Số lượng bản ghi trên một trang. Ràng buộc: `1 <= size <= 100`. |

#### Sắp Xếp (Sorting)
- Mặc định: `completedAt DESC` (lượt khám hoàn thành gần nhất hiển thị lên đầu).

---

### 3.2 Response

#### Success Response (HTTP 200 OK)

```json
{
  "content": [
    {
      "visitId": "23000000-0000-0000-0000-000000000001",
      "visitCode": "VIS000010",
      "patientId": "12000000-0000-0000-0000-000000000001",
      "patientCode": "BN000010",
      "patientName": "Nguyễn Văn A",
      "reason": "Khám sức khỏe tổng quát",
      "completedAt": "2026-09-21T02:30:00Z",
      "examFee": 100000,
      "medicineFee": 0,
      "serviceFee": 150000,
      "totalEstimatedAmount": 250000,
      "hasPrescription": true,
      "hasPendingDispense": false
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "size": 20,
  "number": 0,
  "numberOfElements": 1,
  "first": true,
  "empty": false
}
```

#### Field Descriptions

| Field | Type | Description |
|---|---|---|
| `visitId` | `UUID` | Khóa chính của lượt tiếp đón/khám bệnh. |
| `visitCode` | `String` | Mã lượt tiếp đón/khám bệnh (VD: `VIS000010`). |
| `patientId` | `UUID` | Khóa chính của bệnh nhân. |
| `patientCode` | `String` | Mã bệnh nhân (VD: `BN000010`). |
| `patientName` | `String` | Họ và tên bệnh nhân (tự động mask nếu bật Anonymization Mode). |
| `reason` | `String` | Lý do đến khám bệnh. |
| `completedAt` | `Instant` | Thời điểm bác sĩ hoàn thành lượt khám. |
| `examFee` | `BigDecimal` | Tiền công khám bệnh tiêu chuẩn (100,000 VND). |
| `medicineFee` | `BigDecimal` | Tiền thuốc tạm tính (hiện tại mặc định 0 VND do bảng thuốc chưa có đơn giá bán lẻ). |
| `serviceFee` | `BigDecimal` | Tiền dịch vụ cận lâm sàng tính động theo danh mục và bảng giá hiệu lực. |
| `totalEstimatedAmount` | `BigDecimal` | Tổng tiền viện phí dự kiến (`examFee + medicineFee + serviceFee`). |
| `hasPrescription` | `boolean` | `true` nếu lượt khám có đơn thuốc. Giúp lễ tân nhận biết và đối soát tiền thuốc quầy dược. |
| `hasPendingDispense` | `boolean` | `true` nếu đơn thuốc đang ở trạng thái chờ cấp phát thuốc (`PENDING_DISPENSE`). Frontend dựa vào cờ này để hiển thị huy hiệu cảnh báo, hướng dẫn người bệnh hoàn tất nhận thuốc trước khi xác nhận lập phiếu thu. |

---

## 4. Business Rules & Technical Validation

1. **BR-1 (Trạng thái lượt khám & Đơn thuốc - Finding P1):**
   - Chỉ lấy các lượt khám có `visit.status = 'COMPLETED'` và chưa tồn tại bất kỳ hóa đơn nào trong bảng `invoices` (`NOT EXISTS (SELECT 1 FROM invoices i WHERE i.visit_id = v.id)`).
   - Lượt khám có đơn thuốc ở trạng thái chờ phát thuốc (`PENDING_DISPENSE`) **vẫn được hiển thị đầy đủ** kèm cờ `hasPendingDispense = true` để Lễ tân nắm bắt toàn bộ công nợ khám chữa bệnh trong ngày, phòng ngừa thất thoát doanh thu khi bệnh nhân ra về không lấy thuốc.
   - Tuyệt đối không hiển thị các lượt tiếp đón đang chờ khám (`WAITING`), đang khám (`IN_PROGRESS`), đã hủy (`CANCELLED`), hoặc kết thúc sớm (`EARLY_ENDED`).

2. **BR-2 (Lọc theo ngày):**
   - Khi có tham số `date`, lọc theo khoảng thời gian cả ngày: `visit.completedAt >= date.atStartOfDay(Asia/Ho_Chi_Minh)` và `visit.completedAt < date.plusDays(1).atStartOfDay(Asia/Ho_Chi_Minh)`.

3. **BR-3 (Tìm kiếm từ khóa):**
   - Khi có tham số `search`, áp dụng tìm kiếm chứa ký tự (case-insensitive `LOWER(...) LIKE %keyword%`) đồng thời trên Họ tên bệnh nhân, Mã bệnh nhân, và Mã tiếp đón.

4. **BR-4 (Tính toán chi phí và cơ chế an toàn):**
   - Tiền dịch vụ cận lâm sàng (`serviceFee`) được tổng hợp từ các chỉ định cận lâm sàng trong lượt khám (`clinical_order_items`) dựa theo bảng giá có hiệu lực (`service_price.price`).
   - Nếu xảy ra tình trạng dịch vụ chưa được cấu hình bảng giá hiệu lực, hệ thống kích hoạt cơ chế fallback trả về `serviceFee = 0` thay vì báo lỗi HTTP 500, bảo đảm danh sách thu ngân luôn hiển thị liên tục, không bị gián đoạn hoạt động.

5. **BR-5 (Phân trang và kiểm tra hợp lệ):**
   - `page < 0` hoặc `size < 1` hoặc `size > 100` sẽ lập tức trả về HTTP `400 Bad Request` với thông điệp `"Page must be non-negative and size must be between 1 and 100."`.

---

## 5. Error Responses

### 400 Bad Request (Tham số phân trang không hợp lệ)
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Page must be non-negative and size must be between 1 and 100.",
  "timestamp": "2026-09-21T04:30:00Z"
}
```

### 403 Forbidden (Không đủ quyền hạn)
```json
{
  "code": "ACCESS_DENIED",
  "message": "Access is denied",
  "timestamp": "2026-09-21T04:30:00Z"
}
```
*(Đồng thời tạo 1 bản ghi trong bảng `audit_logs` với `actionType = ACCESS_DENIED`, `resourceType = PERMISSION`)*
