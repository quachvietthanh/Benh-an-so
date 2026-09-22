# Patient Portal Invoice API Contract

> Epic: **NCL-14 - Cổng bệnh nhân và đặt lịch trực tuyến**
>
> User Story: **NCL-14-CN-007 - Bệnh nhân xem và tải hóa đơn của mình**
>
> Base URL: `/patient-portal/invoices`
>
> Auth: Bearer Token (JWT) - Vai trò bắt buộc: `ROLE_PATIENT`
>
> Quy tắc liên quan: `QTN-23`, `QTN-09`, `QTN-01`, `QTN-02`
>
> Trạng thái: **Triển khai hoàn tất Backend**

---

## 1. Phạm vi & Yêu cầu Nghiệp vụ

Tài liệu này đặc tả API contract cho tính năng bệnh nhân tra cứu danh sách, xem chi tiết và tải hóa đơn điện tử bản đọc được (PDF) trên cổng bệnh nhân (`NCL-14-CN-007`).

### 1.1 Mục tiêu nghiệp vụ
- Người bệnh sau khi khám bệnh và thanh toán có thể tự tra cứu các chứng từ hóa đơn viện phí trên cổng trực tuyến mà không cần trực tiếp quay lại quầy thu ngân phòng khám.
- Dữ liệu hóa đơn được phân tách theo từng lượt khám, thể hiện rõ danh mục khoản mục chi phí (khám, dịch vụ cận lâm sàng, thuốc...).
- Hỗ trợ xuất tệp PDF bản đọc được chuẩn khổ A4, thể hiện rõ thông tin phòng khám, bệnh nhân, lượt khám và bảng kê chi phí (`TC-02`).
- Minh bạch tài chính: Hiển thị liên kết giữa hóa đơn gốc và hóa đơn điều chỉnh kèm lý do theo quy tắc `QTN-09`.
- Bảo vệ dữ liệu y tế cá nhân: Nghiêm ngặt tuân thủ `QTN-23`, từ chối và ghi nhật ký kiểm toán `ACCESS_DENIED` khi người dùng cố tình can thiệp mã hóa đơn của người khác (`TC-03`).
- Ghi nhật ký kiểm toán cho mọi hành vi truy cập xem chi tiết (`READ`) và tải tệp (`EXPORT`).

---

## 2. Phân quyền & Kiểm soát Truy cập (Roles & Data Scoping)

| Role | Quyền hạn | Ghi chú & Phạm vi dữ liệu (Data Scope) |
| :--- | :--- | :--- |
| **PATIENT** (`VT-06`) | `ROLE_PATIENT` | Chỉ được xem và tải các hóa đơn gắn với chính hồ sơ bệnh nhân của mình (`visit.patientId == currentPatientId`). Nếu truy cập hóa đơn của bệnh nhân khác sẽ bị từ chối với HTTP 403 `ACCESS_DENIED`. |
| **DOCTOR**, **RECEPTIONIST**, **MANAGER**, **ADMIN** | *Không truy cập cổng bệnh nhân* | Cổng `/patient-portal/**` chỉ dành riêng cho vai trò `PATIENT`. Nhân viên sử dụng các API nghiệp vụ nội bộ tại `/invoices/**`. |

---

## 3. Danh sách Endpoints

### 3.1. Danh sách hóa đơn của bệnh nhân
- **URL**: `GET /patient-portal/invoices`
- **Mô tả**: Trả về danh sách tóm tắt các hóa đơn của bệnh nhân đang đăng nhập, sắp xếp theo ngày tạo mới nhất trước (TC-01, TC-04).
- **Tham số truy vấn (Query Params)**:
  - `visitId` (UUID, tùy chọn): Lọc hóa đơn theo một lượt khám cụ thể.
  - `limit` (Integer, tùy chọn): Số lượng hóa đơn tối đa trả về (mặc định: 50, trần tối đa: 100).
- **Định dạng phản hồi**: Mảng JSON thuần `List<PatientPortalInvoiceSummaryResponse>` (bảo toàn 100% tính tương thích ngược cho Frontend).
- **Thành công**: `200 OK`
```json
[
  {
    "invoiceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "invoiceCode": "HD-20260921-0001",
    "invoiceType": "ORIGINAL",
    "totalAmount": 350000.00,
    "createdAt": "2026-09-21T08:30:00Z",
    "visitId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "visitCode": "KB-20260921-0012",
    "visitDate": "2026-09-21T08:00:00Z",
    "doctorName": "BS. Nguyễn Văn A",
    "specialtyName": "Khoa Nội tổng quát",
    "itemCount": 2
  }
]
```
- **Dữ liệu rỗng** (TC-04): Trả về `[]` với mã `200 OK` khi bệnh nhân chưa có hóa đơn nào.

---

### 3.2. Xem chi tiết hóa đơn
- **URL**: `GET /patient-portal/invoices/{invoiceId}`
- **Mô tả**: Xem chi tiết các khoản mục của một hóa đơn cụ thể (TC-01).
- **Tham số đường dẫn (Path Variables)**:
  - `invoiceId` (UUID, bắt buộc): Mã định danh duy nhất của hóa đơn.
- **Thành công**: `200 OK`
```json
{
  "invoiceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "invoiceCode": "HD-20260921-0001",
  "invoiceType": "ORIGINAL",
  "originalInvoiceId": null,
  "originalInvoiceCode": null,
  "adjustmentReason": null,
  "totalAmount": 350000.00,
  "createdAt": "2026-09-21T08:30:00Z",
  "creatorName": "Lễ tân Trần Thị B",
  "visitId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "visitCode": "KB-20260921-0012",
  "visitDate": "2026-09-21T08:00:00Z",
  "doctorName": "BS. Nguyễn Văn A",
  "specialtyName": "Khoa Nội tổng quát",
  "items": [
    {
      "lineId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
      "lineType": "SERVICE",
      "itemName": "Khám chuyên khoa Nội",
      "quantity": 1,
      "unitPrice": 150000.00,
      "amount": 150000.00
    },
    {
      "lineId": "d4e5f6a7-b8c9-0123-def1-234567890123",
      "lineType": "MEDICINE",
      "itemName": "Paracetamol 500mg",
      "quantity": 20,
      "unitPrice": 10000.00,
      "amount": 200000.00
    }
  ]
}
```
- **Lỗi không có quyền** (TC-03, QTN-23): `403 Forbidden`
```json
{
  "timestamp": "2026-09-21T08:35:00Z",
  "status": 403,
  "code": "ACCESS_DENIED",
  "message": "Patient may only access their own data.",
  "path": "/patient-portal/invoices/a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```
*(Đồng thời ghi nhận sự kiện `ActionType.ACCESS_DENIED` vào bảng nhật ký kiểm toán)*.

---

### 3.3. Tải tệp hóa đơn bản đọc được (PDF)
- **URL**: `GET /patient-portal/invoices/{invoiceId}/download`
- **Mô tả**: Tải về tệp PDF bản đọc được chứa đầy đủ thông tin phòng khám, bệnh nhân, lượt khám và bảng kê chi tiết khoản mục hóa đơn (TC-02).
- **Tham số đường dẫn**:
  - `invoiceId` (UUID, bắt buộc): Mã định danh hóa đơn.
- **Headers phản hồi thành công**:
  - `Content-Type`: `application/pdf`
  - `Content-Disposition`: `attachment; filename="hoa-don-HD-20260921-0001.pdf"`
  - `Content-Length`: Kích thước tệp (bytes)
- **Body**: Chuỗi byte nhị phân của tệp PDF.
- **Audit**: Ghi nhận `ActionType.EXPORT` với kênh `ONLINE_PORTAL` vào nhật ký kiểm toán.
- **Lỗi không có quyền** (TC-03, QTN-23): `403 Forbidden` kèm ghi nhận `ACCESS_DENIED` audit.
