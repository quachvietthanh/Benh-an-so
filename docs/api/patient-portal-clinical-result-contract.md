# Patient Portal Clinical Result API Contract

> Epic: **NCL-14 - Cổng bệnh nhân và đặt lịch trực tuyến**  
> User Story: **NCL-14-CN-009 - Bệnh nhân xem và tải kết quả cận lâm sàng của bản thân**  
> Base URL: `/patient-portal/clinical-results`  
> Auth: Bearer Token (JWT) - Vai trò bắt buộc: `ROLE_PATIENT`  
> Quy tắc liên quan: `QTN-23`, `QTN-13`  
> Trạng thái: **Triển khai Backend**

---

## 1. Phạm vi & Yêu cầu Nghiệp vụ

Tài liệu này đặc tả API contract cho tính năng bệnh nhân tra cứu danh sách, xem chi tiết và tải kết quả cận lâm sàng bản đọc được (PDF khổ A4) trên cổng bệnh nhân (`NCL-14-CN-009`).

### 1.1 Mục tiêu nghiệp vụ
- Bệnh nhân sau khi thực hiện các chỉ định cận lâm sàng (xét nghiệm máu, sinh hóa, nước tiểu, X-quang, siêu âm...) có thể theo dõi kết quả trực tuyến mà không phải quay lại phòng khám.
- Dữ liệu kết quả cận lâm sàng được phân tách theo từng lượt khám (`QTN-13`).
- **Chỉ hiển thị các kết quả đã được bác sĩ/kỹ thuật viên xác nhận** (`status == FINAL`). Tuyệt đối không hiển thị kết quả nháp (`DRAFT`) theo tiêu chí chấp nhận `TC-02`.
- **Hỗ trợ kết quả trả sau** (`TC-04`): Ngay cả khi lượt khám đã kết thúc (`VisitStatus.COMPLETED`), các kết quả mới được phòng xét nghiệm ký duyệt sau đó vẫn hiển thị đầy đủ và chính xác cho lượt khám tương ứng.
- **Bản đọc được (PDF)** (`TC-01`): Cho phép tải phiếu kết quả cận lâm sàng chuẩn khổ A4, thể hiện rõ tiêu đề phòng khám, thông tin bệnh nhân, bác sĩ chỉ định, bác sĩ thực hiện, bảng chỉ số xét nghiệm, ngưỡng tham chiếu, cờ bất thường và kết luận.
- **Bảo vệ dữ liệu y tế cá nhân** (`QTN-23`, `TC-03`): Nghiêm ngặt từ chối (HTTP 403 Forbidden) và ghi nhật ký kiểm toán `ActionType.ACCESS_DENIED` khi người dùng cố tình sửa mã lượt khám (`visitId`) hoặc mã kết quả (`resultId`) trên đường dẫn URL để xem kết quả của người khác.
- Ghi nhật ký kiểm toán đầy đủ cho mọi hành vi truy cập xem (`ActionType.READ`) và tải tệp (`ActionType.EXPORT`) với kênh `ONLINE_PORTAL`.

---

## 2. Phân quyền & Kiểm soát Truy cập (Roles & Data Scoping)

| Role | Quyền hạn | Ghi chú & Phạm vi dữ liệu (Data Scope) |
| :--- | :--- | :--- |
| **PATIENT** (`VT-06`) | `ROLE_PATIENT` | Chỉ được xem và tải các kết quả cận lâm sàng gắn với chính hồ sơ bệnh nhân của mình (`visit.patientId == currentPatientId`). Nếu truy cập dữ liệu của bệnh nhân khác sẽ bị từ chối với HTTP 403 `ACCESS_DENIED`. |
| **DOCTOR**, **NURSE**, **LAB_TECH**, **MANAGER**, **ADMIN** | *Không truy cập cổng bệnh nhân* | Cổng `/patient-portal/**` chỉ dành riêng cho vai trò `PATIENT`. Nhân viên y tế sử dụng các API nghiệp vụ nội bộ tại `/clinical-results/**`. |

---

## 3. Danh sách Endpoints

### 3.1. Danh sách kết quả cận lâm sàng của lượt khám
- **URL**: `GET /patient-portal/clinical-results` (hỗ trợ query param `visitId`) hoặc `GET /patient-portal/visits/{visitId}/clinical-results`
- **Mô tả**: Trả về danh sách tóm tắt các kết quả cận lâm sàng đã xác nhận (`FINAL`) của lượt khám thuộc bệnh nhân đang đăng nhập (TC-01, TC-02, TC-04).
- **Tham số truy vấn (Query Params)**:
  - `visitId` (UUID, bắt buộc): Mã định danh lượt khám cần tra cứu.
- **Định dạng phản hồi**: Mảng JSON thuần `List<PatientPortalClinicalResultSummaryResponse>`
- **Thành công**: `200 OK`
```json
[
  {
    "clinicalResultId": "e1f2a3b4-c5d6-7890-abcd-ef1234567890",
    "clinicalOrderItemId": "f2a3b4c5-d6e7-8901-bcde-f12345678901",
    "visitId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "serviceCode": "XN-MAU-01",
    "serviceName": "Tổng phân tích tế bào máu ngoại vi",
    "resultType": "NUMBER",
    "numericValue": 14.2000,
    "lowerBound": 12.0000,
    "upperBound": 16.5000,
    "textValue": null,
    "unit": "g/dL",
    "referenceRange": "12.0 - 16.5",
    "abnormalFlag": "NORMAL",
    "conclusion": "Bình thường",
    "status": "FINAL",
    "doctorName": "BS. Lê Văn C",
    "enteredAt": "2026-09-22T08:30:00Z",
    "hasAttachment": false
  }
]
```
- **Dữ liệu rỗng** (TC-02 / Chưa có kết quả xác nhận): Trả về `[]` với mã `200 OK` nếu lượt khám chưa có kết quả nào được xác nhận.
- **Lỗi không có quyền** (TC-03, QTN-23): `403 Forbidden` kèm ghi nhận log kiểm toán `ACCESS_DENIED`.

---

### 3.2. Xem chi tiết kết quả cận lâm sàng
- **URL**: `GET /patient-portal/clinical-results/{resultId}`
- **Mô tả**: Xem thông tin chi tiết một kết quả cận lâm sàng cụ thể (TC-01).
- **Tham số đường dẫn (Path Variables)**:
  - `resultId` (UUID, bắt buộc): Mã định danh duy nhất của kết quả.
- **Thành công**: `200 OK`
```json
{
  "clinicalResultId": "e1f2a3b4-c5d6-7890-abcd-ef1234567890",
  "clinicalOrderItemId": "f2a3b4c5-d6e7-8901-bcde-f12345678901",
  "visitId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "visitCode": "KB-20260922-0015",
  "visitAt": "2026-09-22T08:00:00Z",
  "serviceCode": "XN-MAU-01",
  "serviceName": "Tổng phân tích tế bào máu ngoại vi",
  "resultType": "NUMBER",
  "numericValue": 14.2000,
  "lowerBound": 12.0000,
  "upperBound": 16.5000,
  "textValue": null,
  "unit": "g/dL",
  "referenceRange": "12.0 - 16.5",
  "abnormalFlag": "NORMAL",
  "conclusion": "Chỉ số bình thường",
  "status": "FINAL",
  "orderingDoctorName": "BS. Nguyễn Văn A",
  "performingDoctorName": "BS. Lê Văn C",
  "specialtyName": "Khoa Xét nghiệm",
  "enteredAt": "2026-09-22T08:30:00Z",
  "attachments": []
}
```
- **Lỗi không có quyền** (TC-03, QTN-23): `403 Forbidden`
- **Lỗi không tìm thấy hoặc chưa duyệt** (TC-02): `404 Not Found` nếu kết quả ở trạng thái `DRAFT`.

---

### 3.3. Tải phiếu kết quả cận lâm sàng bản đọc được (PDF)
- **URL**: `GET /patient-portal/clinical-results/{resultId}/download` (tải kết quả lẻ) hoặc `GET /patient-portal/visits/{visitId}/clinical-results/download` (tải phiếu kết quả tổng hợp của cả lượt khám)
- **Mô tả**: Tải về tệp PDF bản đọc được chứa đầy đủ thông tin phòng khám, bệnh nhân, lượt khám và bảng kết quả cận lâm sàng (TC-01).
- **Headers phản hồi thành công**:
  - `Content-Type`: `application/pdf`
  - `Content-Disposition`: `attachment; filename="ket-qua-can-lam-sang-KB-20260922-0015.pdf"`
  - `Content-Length`: Kích thước tệp (bytes)
- **Body**: Chuỗi byte nhị phân của tệp PDF.
- **Audit**: Ghi nhận `ActionType.EXPORT`, `ResourceType.CLINICAL_RESULT` với kênh `ONLINE_PORTAL` vào nhật ký kiểm toán.
- **Lỗi không có quyền** (TC-03, QTN-23): `403 Forbidden` kèm ghi nhận `ACCESS_DENIED` audit.
