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
- Stock restored to the **original batch**; every return is audited (`medication_returns` + `stock_movements` + audit log).
- Multiple partial returns are supported.
- **Payment guard:** if the visit is paid (`RECORDED`/`SUCCESS`) and not refunded, the return is rejected with `409 MEDICATION_RETURN_PAYMENT_NOT_REFUNDED`. The refund flow (NCL-07-CN-004) must run first.
- Resulting status: all items returned → `PENDING_DISPENSE`; some remaining → `PARTIALLY_DISPENSED`.

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
  (no date of birth, no pregnancy status, or no recorded chronic diseases). The frontend
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
Overrides are recorded in `prescription_contraindication_warning_logs` (rule, type, severity,
medicine, reason, actor, timestamp).

---

## Database migrations

- `V74__add_invoice_reprint_tracking.sql` — `invoices.reprint_count`, `invoices.last_reprinted_at`.
- `V75__create_medication_returns.sql` — `prescription_dispense_items.returned_quantity`, new `medication_returns` table.
- `V76__create_contraindication_schema.sql` — `patients.pregnancy_status`, new `contraindication_rules` and `prescription_contraindication_warning_logs` tables.

