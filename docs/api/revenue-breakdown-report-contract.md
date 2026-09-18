# Revenue Breakdown Reporting API Contract

> Module: **NCL-08 - Báo cáo vận hành và nhật ký truy cập**
>
> User Story: **NCL-08-CN-009 - Báo cáo doanh thu theo dịch vụ và theo bác sĩ**
>
> Base URL: `http://localhost:8080/api/v1`
>
> Auth: Bearer Token (JWT)
>
> Status: **Official Baseline**

---

## 1. Scope

Tài liệu này quy định API contract cho chức năng **Báo cáo doanh thu theo dịch vụ và theo bác sĩ** (`NCL-08-CN-009`):
1. Bóc tách doanh thu theo nhóm dịch vụ (`EXAMINATION`, `LAB_TEST`, `IMAGING`, `OTHER`, `MEDICATION`).
2. Bóc tách doanh thu theo bác sĩ chỉ định / phụ trách (kèm nhóm dự phòng `UNASSIGNED` cho các khoản thu chưa phân bổ).
3. Bóc tách doanh thu tiền thuốc (`MEDICATION`).
4. Khấu trừ đầy đủ hóa đơn điều chỉnh và hoàn tiền.
5. Bảo đảm tổng số liệu bóc tách khớp 100% với tổng doanh thu thuần của `NCL-08-CN-002` trong cùng kỳ.

---

## 2. Roles And Permissions

- **Allowed Roles:** `MANAGER` (hoặc các tài khoản được cấp quyền `REPORT_VIEW`).
  > *Lưu ý:* Vai trò `ADMIN` trong hệ thống không được cấp quyền `REPORT_VIEW` theo mặc định (nguyên tắc Tách biệt trách nhiệm - Separation of Duties).
- **Required Permission:** `@RequirePermission("REPORT_VIEW")`.
- **Unauthorized Roles:** `ADMIN` (mặc định), `RECEPTIONIST`, `DOCTOR`, `PHARMACIST`, `PATIENT` (trả về `403 Forbidden` và tự động ghi `AuditLog` với `actionType = ACCESS_DENIED`, `resourceType = PERMISSION`).

---

## 3. Request Parameters

```text
GET /reports/revenue-breakdown?from=YYYY-MM-DD&to=YYYY-MM-DD
```

- `from`: Ngày bắt đầu (`YYYY-MM-DD`), bắt buộc.
- `to`: Ngày kết thúc (`YYYY-MM-DD`), bắt buộc.
- **Validation:**
  - `from` và `to` phải đúng định dạng `yyyy-MM-dd`.
  - `from <= to`.
  - Khoảng thời gian không vượt quá 366 ngày (`MAX_REPORT_RANGE_DAYS`).
  - Vi phạm trả về `400 Bad Request` với mã lỗi `VALIDATION_ERROR`.

---

## 4. Business Calculation & Integrity Rules

### 4.1 Nguồn Dữ Liệu Chuẩn (Source of Truth)
- Dữ liệu doanh thu dựa trên các hóa đơn (`invoices`) có `createdAt` nằm trong khoảng thời gian `[fromInclusive, toExclusive)` (múi giờ `Asia/Ho_Chi_Minh`).
- Doanh thu từng mục tính theo từng dòng hóa đơn (`invoice_lines`):
  - `EXAM_FEE`: Tiền khám bệnh -> Nhóm dịch vụ `EXAMINATION`, gán cho Bác sĩ phụ trách lượt khám (`visit.doctorId`).
  - `SERVICE_FEE`: Dịch vụ cận lâm sàng -> Tra cứu nhóm dịch vụ từ `clinical_service_catalog.service_type` (`LAB_TEST`, `IMAGING`, `OTHER`), gán cho Bác sĩ chỉ định (`clinical_orders.orderedBy`).
  - `MEDICINE_FEE`: Tiền thuốc -> Nhóm `MEDICATION`, gán cho Bác sĩ kê đơn (`prescriptions.prescribedBy`, hoặc fallback `visit.doctorId`). Đối với lượt khám có nhiều đơn thuốc từ nhiều bác sĩ khác nhau, doanh thu thuốc tạm thời được gán cho bác sĩ có đơn thuốc kê mới nhất trong lượt khám.
  - `ADJUSTMENT`: Hóa đơn điều chỉnh và hoàn tiền -> Khấu trừ vào nhóm dịch vụ/thuốc và bác sĩ tương ứng theo dòng gốc (`originalInvoiceId` / `referenceId`). Trong trường hợp khoản điều chỉnh/hoàn tiền không thể liên kết được dòng gốc (`targetLineType == null`), khoản tiền sẽ được phân loại an toàn vào nhóm dịch vụ `OTHER` ("Dịch vụ khác") và gán cho nhóm bác sĩ `UNASSIGNED` ("Chưa phân bổ bác sĩ"), bảo đảm không làm sai lệch số liệu khám bệnh của bác sĩ phụ trách.

### 4.2 Tính Bảo Toàn Khớp Số Liệu (Postcondition)
- Tổng doanh thu thuần:
  $$\text{totalNetRevenue} = \sum (\text{invoices.totalAmount trong kỳ})$$
- Khớp hoàn toàn với `OperationalSummaryResult.revenue` của `NCL-08-CN-002`.
- Khớp 100% giữa tổng nhóm dịch vụ và tổng bác sĩ:
  $$\text{totalNetRevenue} = \sum (\text{serviceGroup.revenue}) = \sum (\text{doctor.totalRevenue})$$
- **Quy tắc tính tỷ lệ phần trăm (%):**
  - Khi $\text{totalNetRevenue} \le 0$: Mọi tỷ lệ phần trăm trả về `0.00`.
  - Khi $\text{totalNetRevenue} > 0$: $\text{percentage} = \text{revenue} \times 100 / \text{totalNetRevenue}$, làm tròn 2 chữ số thập phân (`HALF_UP`). Cho phép tỷ lệ phần trăm mang giá trị âm khi một mục có doanh thu âm do khấu trừ hoàn tiền/điều chỉnh vượt quá phát sinh, bảo đảm tổng tỷ trọng đóng góp của các bác sĩ luôn bảo toàn đạt đúng 100%.

---

## 5. Response Contract

### 5.1 Success Response (200 OK)

```json
{
  "from": "2026-08-01",
  "to": "2026-08-31",
  "totalNetRevenue": 45000000.00,
  "totalExamRevenue": 10000000.00,
  "totalClinicalServiceRevenue": 20000000.00,
  "totalMedicationRevenue": 15000000.00,
  "totalAdjustmentRevenue": -500000.00,
  "currency": "VND",
  "serviceGroups": [
    {
      "groupCode": "EXAMINATION",
      "groupName": "Khám bệnh",
      "revenue": 10000000.00,
      "percentage": 22.22
    },
    {
      "groupCode": "LAB_TEST",
      "groupName": "Xét nghiệm",
      "revenue": 12000000.00,
      "percentage": 26.67
    },
    {
      "groupCode": "IMAGING",
      "groupName": "Chẩn đoán hình ảnh",
      "revenue": 8000000.00,
      "percentage": 17.78
    },
    {
      "groupCode": "OTHER",
      "groupName": "Dịch vụ khác",
      "revenue": 0.00,
      "percentage": 0.00
    },
    {
      "groupCode": "MEDICATION",
      "groupName": "Thuốc / Dược phẩm",
      "revenue": 15000000.00,
      "percentage": 33.33
    }
  ],
  "doctors": [
    {
      "doctorId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2",
      "doctorCode": "doctor1",
      "doctorName": "Dr. Nguyen Minh Anh",
      "examRevenue": 6000000.00,
      "clinicalServiceRevenue": 12000000.00,
      "medicationRevenue": 9000000.00,
      "adjustmentRevenue": -300000.00,
      "totalRevenue": 26700000.00,
      "percentage": 59.33
    },
    {
      "doctorId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3",
      "doctorCode": "doctor2",
      "doctorName": "Dr. Tran Quang Huy",
      "examRevenue": 4000000.00,
      "clinicalServiceRevenue": 8000000.00,
      "medicationRevenue": 6000000.00,
      "adjustmentRevenue": -200000.00,
      "totalRevenue": 17800000.00,
      "percentage": 39.56
    },
    {
      "doctorId": null,
      "doctorCode": "UNASSIGNED",
      "doctorName": "Chưa phân bổ bác sĩ",
      "examRevenue": 0.00,
      "clinicalServiceRevenue": 500000.00,
      "medicationRevenue": 0.00,
      "adjustmentRevenue": 0.00,
      "totalRevenue": 500000.00,
      "percentage": 1.11
    }
  ]
}
```

### 5.2 Empty Period Response (200 OK)

Khi không có hóa đơn trong kỳ, API trả về `200 OK`:

```json
{
  "from": "2026-08-01",
  "to": "2026-08-31",
  "totalNetRevenue": 0.00,
  "totalExamRevenue": 0.00,
  "totalClinicalServiceRevenue": 0.00,
  "totalMedicationRevenue": 0.00,
  "totalAdjustmentRevenue": 0.00,
  "currency": "VND",
  "serviceGroups": [],
  "doctors": []
}
```
