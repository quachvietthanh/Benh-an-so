# Báo cáo xuất nhập tồn theo kỳ (NCL-06-CN-013)

Backend API contract for the periodic stock in/out inventory report.

## Endpoint

```
GET /inventory/report/stock-in-out?from={yyyy-MM-dd}&to={yyyy-MM-dd}
```

- Method: `GET`
- Authorization: `@RequirePermission({"PHARMACY_READ", "REPORT_VIEW"})` (any one)
  - `PHARMACIST` (PHARMACY_READ), `MANAGER` (REPORT_VIEW), `ADMIN` (PHARMACY_READ) are allowed.
- Period semantics: half-open interval `[from 00:00, to+1d 00:00)` in clinic timezone `Asia/Ho_Chi_Minh`.
  - A transaction exactly at `from 00:00` is included; a transaction exactly at `to+1d 00:00` is excluded (it becomes the next period's opening).
- Validation: `from`/`to` required (`yyyy-MM-dd`), `from <= to`, range ≤ 366 days.

## Response (200 OK)

```json
{
  "from": "2026-08-01",
  "to": "2026-08-31",
  "generatedAt": "2026-08-31T08:00:00Z",
  "hasTransactions": true,
  "items": [
    {
      "medicineId": "00000000-0000-0000-0000-000000000001",
      "medicineCode": "MED-001",
      "medicineName": "Paracetamol",
      "unit": "vien",
      "openingQuantity": 100,
      "receivedQuantity": 20,
      "dispensedQuantity": 30,
      "returnedQuantity": 5,
      "adjustedQuantity": -2,
      "closingQuantity": 93
    }
  ]
}
```

- `hasTransactions`: `false` when there are no in-period movements (see empty-period behavior).
- Rows are sorted by `medicineCode` ascending.
- A medicine appears in `items` when it has a non-zero opening stock or any in-period movement.

## Fields

| Field | Meaning | Report category |
|---|---|---|
| `openingQuantity` | Net stock immediately before `from` | Tồn đầu kỳ |
| `receivedQuantity` | Receipts inside the period (always ≥ 0) | Số nhập |
| `dispensedQuantity` | Dispensing inside the period (≥ 0) | Số cấp phát |
| `returnedQuantity` | Returns inside the period (≥ 0) | Số trả lại |
| `adjustedQuantity` | Net of `ADJUSTMENT` + `EXPIRE` movements (signed) | Số điều chỉnh |
| `closingQuantity` | `opening + received - dispensed + returned + adjusted` | Tồn cuối kỳ |

## Reconciliation formula

```
closingQuantity = openingQuantity + receivedQuantity - dispensedQuantity
                + returnedQuantity + adjustedQuantity
```

The backend computes `closingQuantity` from the same aggregated movement sources, so the
report is internally reconcilable by construction (NCL-06-CN-013-TC-01).

## Movement type mapping (authoritative sources)

| Source | Movement type | Report category | Sign |
|---|---|---|---|
| `inventory_receipt_items` (joined `inventory_receipts`) | receipt | `receivedQuantity` | `+` |
| `stock_movements` | `DISPENSE` | `dispensedQuantity` | `-` (reported positive) |
| `stock_movements` | `RETURN` | `returnedQuantity` | `+` |
| `stock_movements` | `ADJUSTMENT` | `adjustedQuantity` | signed |
| `stock_movements` | `EXPIRE` | `adjustedQuantity` (folded) | signed |

Notes:
- The project's receiving workflow persists receipts to `inventory_receipt_items`
  (it does **not** write a `RECEIPT` `StockMovement`), so receipts are read from
  `inventory_receipts`/`inventory_receipt_items`.
- The Excel report defines only the six columns above; there is no separate disposal
  column. `EXPIRE` is therefore folded into `adjustedQuantity` (its `quantity_change`
  is negative, which keeps the formula balanced). This is documented, not implicit.
- No active workflow currently writes `ADJUSTMENT` or `EXPIRE` movements, so in practice
  `adjustedQuantity` is `0` until such workflows are added. The enum values and columns
  already exist in the schema, so the report is future-proof without schema changes.

## Empty period (NCL-06-CN-013-TC-03)

When there are no in-period movements:
- `hasTransactions` is `false`.
- Each medicine with existing opening stock still appears with `openingQuantity == closingQuantity`
  and zero movement categories.
- A completely empty inventory returns `items: []` and `hasTransactions: false`.

## Query design

The report uses two set-based aggregate JPQL queries (one over receipt items, one over
stock movements) merged in-memory, followed by a single batched `MedicineRepository.findAllById`
catalog lookup. It does **not** loop medicines and query movements per medicine (no N+1).
