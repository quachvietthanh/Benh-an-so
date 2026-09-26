# Backend Contract — Invoice, Pharmacy & Clinical Workflows

> Branch: `feature/invoice-pharmacy-clinical-workflows`
> Covers: NCL-07-CN-005, NCL-06-CN-011, NCL-06-CN-009, NCL-05-CN-006

This document is the backend contract for the frontend team. No backend change is
required to build the UI against these endpoints.

---

## 1. NCL-07-CN-005 — Tra cứu và in lại hóa đơn

Base: `/invoices` (permission `INVOICE_READ`).

| Method | Path | Description |
| --- | --- | --- |
| GET | `/invoices` | Search invoices (existing). Filters: `invoiceCode`, `invoiceType`, `visitId`, `createdFrom`, `createdTo`, `page`, `size`. |
| GET | `/invoices/{invoiceId}` | Invoice detail (now includes `reprintCount`, `lastReprintedAt`). |
| GET | `/invoices/{invoiceId}/adjustments` | Related adjustment invoices + `originalAmount` + `finalAmount`. |
| POST | `/invoices/{invoiceId}/reprint` | Records one reprint and returns the invoice with incremented `reprintCount`. |

`InvoiceResponse` now carries:
- `reprintCount` (`int`) — number of reprints already recorded.
- `lastReprintedAt` (`Instant`, nullable) — last reprint timestamp.

`InvoiceAdjustmentsResponse`:
```json
{
  "originalInvoiceId": "…",
  "originalAmount": 250000,
  "finalAmount": 230000,
  "adjustments": [ /* InvoiceResponse[] */ ]
}
```

Reprint is idempotent-by-counter: each `POST …/reprint` increments `reprintCount`.
The frontend should mark the printed document as "bản in lại" when `reprintCount > 0`.

---

## 2. NCL-06-CN-011 — Cấp phát theo hạn dùng gần trước (FEFO)

Base: `/prescriptions`.

| Method | Path | Description |
| --- | --- | --- |
| GET | `/prescriptions/{id}/dispense-suggestion` | FEFO batch suggestion for the dispensing screen (permission `PRESCRIPTION_UPDATE_STATUS`). |
| POST | `/prescriptions/{id}/partial-dispense` | Existing. Deducts stock in FEFO order (earliest expiry first). |

`GET …/dispense-suggestion` returns, per item with remaining quantity, the eligible
batches ordered by expiry ascending (expired/zero/blocked batches are already excluded):

```json
{
  "prescriptionId": "…",
  "items": [
    {
      "prescriptionItemId": "…",
      "medicineId": "…",
      "medicineName": "Paracetamol 500 mg",
      "prescribedQuantity": 20,
      "remainingQuantity": 20,
      "batches": [
        { "batchId": "…", "batchNumber": "BATCH-A", "expiryDate": "2026-12-01", "availableQuantity": 8, "suggestedQuantity": 8 },
        { "batchId": "…", "batchNumber": "BATCH-B", "expiryDate": "2027-06-01", "availableQuantity": 100, "suggestedQuantity": 12 }
      ]
    }
  ]
}
```


---

## 3. NCL-06-CN-009 — Trả lại thuốc và hủy phiếu cấp phát

Base: `/prescriptions`.

| Method | Path | Description |
| --- | --- | --- |
| POST | `/prescriptions/{id}/return` | Return dispensed medication to the original batch (permission `PRESCRIPTION_UPDATE_STATUS`). |

Request:
```json
{
  "reason": "Bệnh nhân không dùng thuốc",
  "items": [ { "dispenseItemId": "…", "quantity": 5 } ]
}
```

Response:
```json
{
  "prescriptionId": "…",
  "status": "PARTIALLY_DISPENSED",
  "returnedBy": "…",
  "returnedAt": "…",
  "returns": [
    {
      "returnId": "…",
      "dispenseItemId": "…",
      "prescriptionItemId": "…",
      "medicineId": "…",
      "medicineName": "…",
      "medicineBatchId": "…",
      "batchNumber": "BATCH-A",
      "returnedQuantity": 5,
      "remainingReturnableQuantity": 7
    }
  ]
}
```

Business rules:
- Only `PHARMACIST`/`ADMIN`.
- Only `DISPENSED` / `PARTIALLY_DISPENSED` prescriptions.
- `quantity > 0` and `<= remaining returnable` (dispensed − already returned).
- **Same-day rule:** the dispensing slip must have been created today (clinical timezone `Asia/Ho_Chi_Minh`); returns from a previous day are rejected.
- Stock restored to the **original batch**; every return is audited (`medication_returns` + `stock_movements` + audit log).
- Multiple partial returns are supported.
- **Payment guard:** if the visit is paid (`RECORDED`/`SUCCESS`) and not refunded, the return is rejected with `409 MEDICATION_RETURN_PAYMENT_NOT_REFUNDED`. The refund flow (NCL-07-CN-004) must run first.
- Resulting status: all items returned → `CANCELLED` (the dispensing slip is cancelled and must not re-enter the dispensing queue); some remaining → `PARTIALLY_DISPENSED`.

---

## 4. NCL-05-CN-006 — Cảnh báo chống chỉ định

Base: `/prescriptions` and `/patients`.

| Method | Path | Description |
| --- | --- | --- |
| POST | `/prescriptions/check-contraindications` | Evaluate age/pregnancy/disease contraindications (permission `PRESCRIPTION_CREATE`/`PRESCRIPTION_UPDATE`). |
| PATCH | `/patients/{patientId}/pregnancy-status` | Set/clear a patient's pregnancy status (permission `PATIENT_UPDATE`). |
| POST | `/prescriptions` | Existing. Now accepts `contraindicationOverrides` and blocks completion until all warnings are overridden. |

Check request:
```json
{ "medicalRecordId": "…", "medicineIds": ["…"] }
```

Check response:
```json
{
  "warnings": [
    {
      "ruleId": "…",
      "medicineId": "…",
      "medicineName": "Aspirin 81 mg",
      "type": "AGE",
      "severity": "CONTRAINDICATED",
      "message": "…",
      "recommendation": "Use Paracetamol…"
    }
  ],
  "missingData": [
    { "medicineId": "…", "medicineName": "…", "type": "PREGNANCY", "message": "Patient pregnancy status is missing…" }
  ]
}
```

- `type` ∈ `AGE`, `PREGNANCY`, `DISEASE`.
- `severity` ∈ `LOW`, `MODERATE`, `SEVERE`, `CONTRAINDICATED`.
- `warnings` = matched contraindications; `missingData` = required patient info missing
  (no date of birth for an age rule, or no pregnancy status for a female patient).
  Pregnancy evaluation is not applicable to non-female patients, and an empty chronic
  disease list is treated as "no matching disease" (not missing data). The frontend
  must distinguish these from a genuine "no warning" result (both lists empty).

Pregnancy status request:
```json
{ "pregnancyStatus": "PREGNANT" }   // or "NOT_PREGNANT", or null to clear
```

Prescription creation now requires `contraindicationOverrides` for every detected warning:
```json
{
  "medicalRecordId": "…",
  "items": [ … ],
  "interactionOverrides": [ … ],
  "allergyOverrides": [ … ],
  "contraindicationOverrides": [
    { "ruleId": "…", "medicineId": "…", "overrideReason": "…" }
  ]
}
```
If a warning is not overridden, creation fails with `409 CONTRAINDICATION_CONFIRMATION_REQUIRED`.
If required patient data is missing, creation fails with `422 CONTRAINDICATION_DATA_MISSING`
(even when an override reason is supplied — an override never bypasses missing data).
Overrides are recorded in `prescription_contraindication_warning_logs` (rule, type, severity,
medicine, reason, actor, timestamp).

---

---

## 5. NCL-07-CN-007 — Thu phí nhiều phương thức và ghi nhận phương thức thanh toán

Base: `/invoices` (permission `INVOICE_CREATE` cho ghi nhận thanh toán, `INVOICE_READ` cho tra cứu hóa đơn).

### 5.1. Ghi nhận thanh toán đa phương thức (`POST /invoices/payments`)

Hỗ trợ đồng thời 2 định dạng:
1. **Định dạng mới (Nhiều phương thức)**:
```json
{
  "visitId": "d0000000-0000-0000-0000-000000000001",
  "examFee": 100000,
  "medicineFee": 150000,
  "amountPaid": 250000,
  "paymentMethods": [
    {
      "paymentMethod": "CASH",
      "amount": 100000
    },
    {
      "paymentMethod": "BANK_TRANSFER",
      "amount": 150000,
      "referenceNumber": "TXN-20260921-001"
    }
  ]
}
```

2. **Định dạng cũ (Tương thích ngược 1 phương thức)**:
```json
{
  "visitId": "d0000000-0000-0000-0000-000000000001",
  "examFee": 100000,
  "medicineFee": 150000,
  "amountPaid": 250000,
  "paymentMethod": "BANK_TRANSFER",
  "referenceNumber": "TXN-20260921-001"
}
```

**Quy tắc xác thực (Business Rules & Validations)**:
- `paymentMethods`: Bắt buộc tổng `amount` của các phần tử phải khớp chính xác với `totalAmount` của hóa đơn. Nếu nhỏ hơn hoặc lớn hơn, trả về lỗi `400 Bad Request` (`PAYMENT_AMOUNT_MISMATCH`) kèm số tiền còn thiếu.
- `BANK_TRANSFER`: Bắt buộc phải có `referenceNumber` không được để trống (vi phạm trả về `400 Bad Request` - `VALIDATION_FAILED`). Hỗ trợ truyền ở cấp con `paymentMethods[i].referenceNumber` hoặc cấp cao `referenceNumber` (cho luồng đơn phương thức).
- `referenceNumber`: Độ dài tối đa 100 ký tự. Nếu vượt quá sẽ bị chặn với mã lỗi `400 Bad Request` (`VALIDATION_FAILED`).
- `paymentMethod` trên response: Tự động là `MULTIPLE` nếu kết hợp từ 2 phương thức khác nhau, hoặc giữ nguyên mã phương thức đơn nếu chỉ dùng 1 loại.

Response (`PaymentResponse`):
```json
{
  "id": "…",
  "visitId": "…",
  "examFee": 100000,
  "medicineFee": 150000,
  "serviceFee": 0,
  "totalAmount": 250000,
  "amountPaid": 250000,
  "paymentMethod": "MULTIPLE",
  "status": "RECORDED",
  "collectedBy": "…",
  "paidAt": "2026-09-21T16:00:00Z",
  "createdAt": "2026-09-21T16:00:00Z",
  "paymentMethods": [
    {
      "id": "…",
      "paymentId": "…",
      "paymentMethod": "CASH",
      "amount": 100000,
      "referenceNumber": null,
      "createdAt": "2026-09-21T16:00:00Z"
    },
    {
      "id": "…",
      "paymentId": "…",
      "paymentMethod": "BANK_TRANSFER",
      "amount": 150000,
      "referenceNumber": "TXN-20260921-001",
      "createdAt": "2026-09-21T16:00:00Z"
    }
  ]
}
```

### 5.2. Tra cứu chi tiết hóa đơn kèm thanh toán (`GET /invoices/{invoiceId}`)

`InvoiceResponse` bổ sung trường `payment` (`PaymentDetailResponse`), giúp frontend loại bỏ hoàn toàn cơ chế lưu tạm vào `localStorage`:
```json
{
  "id": "…",
  "invoiceCode": "HD000010",
  "visitId": "…",
  "paymentId": "…",
  "type": "ORIGINAL",
  "totalAmount": 250000,
  "payment": {
    "id": "…",
    "visitId": "…",
    "status": "RECORDED",
    "totalAmount": 250000,
    "amountPaid": 250000,
    "paymentMethod": "MULTIPLE",
    "collectedBy": "…",
    "collectorName": "Nguyễn Văn Thu Ngân",
    "paidAt": "2026-09-21T16:00:00Z",
    "createdAt": "2026-09-21T16:00:00Z",
    "paymentMethods": [
      {
        "id": "…",
        "paymentId": "…",
        "paymentMethod": "CASH",
        "amount": 100000,
        "referenceNumber": null,
        "createdAt": "2026-09-21T16:00:00Z"
      },
      {
        "id": "…",
        "paymentId": "…",
        "paymentMethod": "BANK_TRANSFER",
        "amount": 150000,
        "referenceNumber": "TXN-20260921-001",
        "createdAt": "2026-09-21T16:00:00Z"
      }
    ]
  }
}
```

---

## Database migrations

- `V74__add_invoice_reprint_tracking.sql` — `invoices.reprint_count`, `invoices.last_reprinted_at`.
- `V75__create_medication_returns.sql` — `prescription_dispense_items.returned_quantity`, new `medication_returns` table.
- `V76__create_contraindication_schema.sql` — `patients.pregnancy_status`, new `contraindication_rules` and `prescription_contraindication_warning_logs` tables.
- `V77__add_inventory_report_view_permission.sql` — quyền xem báo cáo xuất nhập tồn kho.
- `V81__add_priority_to_queue_items.sql` — ưu tiên khám cho bệnh nhân cấp cứu (NCL-03-CN-013).
- `V82__create_payment_method_items_and_support_multiple_methods.sql` — tạo bảng `payment_method_items`, cập nhật constraint `chk_payments_method` mở rộng `'MULTIPLE'`, backfill toàn bộ dữ liệu lịch sử.

---

## 6. Khuyến nghị tích hợp và đối soát ca thu ngân (QTN-38)

Khi triển khai tính năng **Chốt ca thu ngân cuối ngày (`NCL-07-CN-009` / `QTN-38`)** hoặc các báo cáo doanh thu theo phương thức thanh toán:
- **Không thực hiện** `GROUP BY payments.payment_method` hoặc `if (payment.getPaymentMethod() == PaymentMethod.CASH)` đơn thuần, vì các khoản thu đa phương thức sẽ có `payment_method = 'MULTIPLE'`, dẫn đến nguy cơ gom nhầm hoặc bỏ sót tiền mặt.
- **Bắt buộc đọc từ `payment_method_items`**:
  - Tại tầng Domain Java: Sử dụng các helper methods sẵn có trên entity `Payment`:
    - `payment.getCashAmount()`
    - `payment.getBankTransferAmount()`
    - `payment.getAmountByMethod(PaymentMethod method)`
    - `payment.getPaymentMethodItems()`
  - Tại tầng Frontend / Client: Duyệt mảng `paymentMethods` trong `PaymentResponse` / `InvoiceResponse.payment` để phân rã chính xác số tiền từng phương thức.

---

## 7. NCL-06-CN-012 — Dự trù mua thuốc và phiếu đặt hàng

Base: `/inventory/procurements` (Quyền: `MEDICATION_PROCUREMENT_READ`, `MEDICATION_PROCUREMENT_CREATE`, `MEDICATION_PROCUREMENT_APPROVE`).

| Phương thức | Đường dẫn | Mô tả chức năng | Quyền yêu cầu |
| --- | --- | --- | --- |
| GET | `/inventory/procurements/suggestions` | Gợi ý số lượng cần mua (kết hợp tồn khả dụng lô còn hạn, tồn tối thiểu, lượng cấp phát kỳ trước) | `MEDICATION_PROCUREMENT_READ` |
| POST | `/inventory/procurements` | Lập phiếu dự trù mới (lưu nháp `DRAFT` hoặc gửi duyệt `PENDING_APPROVAL`) | `MEDICATION_PROCUREMENT_CREATE` |
| GET | `/inventory/procurements` | Danh sách tóm tắt phiếu dự trù (phân trang, lọc theo status, ngày tạo, người tạo) | `MEDICATION_PROCUREMENT_READ` |
| GET | `/inventory/procurements/{id}` | Chi tiết phiếu dự trù và các dòng thuốc kèm số lượng duyệt | `MEDICATION_PROCUREMENT_READ` |
| PUT | `/inventory/procurements/{id}` | Điều chỉnh danh sách thuốc / số lượng đề nghị (khi phiếu nháp hoặc chờ duyệt) | `MEDICATION_PROCUREMENT_CREATE` |
| POST | `/inventory/procurements/{id}/submit` | Gửi duyệt phiếu dự trù đang ở trạng thái `DRAFT` | `MEDICATION_PROCUREMENT_CREATE` |
| POST | `/inventory/procurements/{id}/cancel` | Hủy phiếu dự trù đang ở trạng thái `DRAFT` hoặc `PENDING_APPROVAL` | `MEDICATION_PROCUREMENT_CREATE` |
| POST | `/inventory/procurements/{id}/approve` | Quản lý phê duyệt phiếu dự trù (SoD: Người lập không được tự duyệt) | `MEDICATION_PROCUREMENT_APPROVE` |
| POST | `/inventory/procurements/{id}/reject` | Quản lý từ chối phiếu dự trù kèm lý do bắt buộc $\ge 5$ ký tự (SoD) | `MEDICATION_PROCUREMENT_APPROVE` |

**Quy tắc kiểm soát Separation of Duties (SoD):**
- Dược sĩ lập phiếu dự trù. Quản lý phòng khám duyệt hoặc từ chối phiếu.
- Người lập phiếu tuyệt đối không thể tự phê duyệt hoặc tự từ chối phiếu của mình (ném lỗi `403 Forbidden` - `SELF_APPROVAL_NOT_ALLOWED`).
- Lễ tân và Bác sĩ không có quyền truy cập, hệ thống tự động ghi nhật ký `ACCESS_DENIED` (`TC-03`).





