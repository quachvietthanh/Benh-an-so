# Pharmacy Dispensing API Contract

> Module: **NCL-06 - Quản lý kho thuốc và cấp phát**
>
> User Story: **NCL-06-CN-003 - Cấp phát thuốc theo đơn**
>
> Base URL: `http://localhost:8080/api/v1`
>
> Auth: Bearer Token (JWT)
>
> Date: **2026-08-07**
>
> Status: **Phase 1 - Contract and Rule Baseline**

---

## 1. Scope

Tài liệu này chốt các quyết định Phase 1 cho tính năng cấp phát thuốc theo đơn:

1. Rule chọn lô cấp phát.
2. Điều kiện một lô được phép tham gia cấp phát.
3. Contract lỗi thiếu tồn kho theo từng thuốc.
4. Nguyên tắc lưu allocation từ đơn thuốc xuống từng lô.

Các quyết định trong tài liệu này là baseline để triển khai backend service, migration, API response, và frontend integration cho `NCL-06-CN-003`.

---

## 2. Dispensing Rule

### 2.1 Batch Selection Strategy

Hệ thống sử dụng rule **FEFO**: `First Expired, First Out`.

Ý nghĩa:

- Khi cấp phát một thuốc, hệ thống ưu tiên lấy từ lô có `expiry_date` gần nhất trước.
- Nếu số lượng của lô gần hết hạn nhất không đủ, hệ thống tiếp tục lấy từ lô kế tiếp theo thứ tự hạn dùng tăng dần.
- Một dòng thuốc trong đơn có thể được cấp từ **nhiều lô**.

### 2.2 Eligibility Rule For A Batch

Một lô chỉ được dùng để cấp phát nếu đồng thời thỏa cả 3 điều kiện:

- `status = ACTIVE`
- `quantity > 0`
- `expiry_date >= today`

Trong đó:

- `today` được xác định theo thời điểm xử lý nghiệp vụ trên backend.
- Lô `EXPIRED`, `DEPLETED`, hoặc có `quantity <= 0` bị loại khỏi tập lô khả dụng.

### 2.3 Dispensing Atomicity

Cấp phát đơn thuốc là thao tác **all-or-nothing**:

- Nếu bất kỳ thuốc nào trong đơn không đủ tồn khả dụng, toàn bộ thao tác cấp phát thất bại.
- Không được phép cấp phát một phần đơn rồi để phần còn lại thất bại.
- Chỉ khi tất cả các dòng thuốc đều được allocate đủ thì đơn mới được chuyển sang `DISPENSED`.

---

## 3. Inventory Check Rule

Đối với từng `prescription_item`, hệ thống tính:

- `requiredQuantity`: số lượng bác sĩ kê trong đơn.
- `availableQuantity`: tổng `quantity` của các lô hợp lệ theo rule ở mục 2.2.

Điều kiện đủ tồn:

```text
availableQuantity >= requiredQuantity
```

Nếu điều kiện trên sai với bất kỳ thuốc nào:

- Không thực hiện trừ tồn.
- Không đổi trạng thái đơn.
- Trả lỗi nghiệp vụ chứa danh sách thuốc thiếu tồn.

---

## 4. API Error Contract For Insufficient Stock

### 4.1 When Returned

Backend trả lỗi thiếu tồn khi:

- Đơn ở trạng thái hợp lệ để cấp phát, nhưng
- Ít nhất một thuốc trong đơn không đủ tổng tồn khả dụng từ các lô hợp lệ.

### 4.2 HTTP Status

Chốt dùng:

- `409 Conflict`

Lý do:

- Request hợp lệ về mặt cú pháp.
- Nhưng trạng thái tài nguyên hiện tại của kho không đáp ứng được thao tác cấp phát.

### 4.3 Response Shape

```json
{
  "code": "INSUFFICIENT_STOCK",
  "message": "Insufficient stock for one or more medicines.",
  "prescriptionId": "16200000-0000-0000-0000-000000000003",
  "details": [
    {
      "prescriptionItemId": "27200000-0000-0000-0000-000000000010",
      "medicineId": "15000000-0000-0000-0000-000000000006",
      "medicineCode": "MED-006",
      "medicineName": "Metformin 850 mg",
      "requiredQuantity": 60,
      "availableQuantity": 40,
      "shortageQuantity": 20
    }
  ],
  "timestamp": "2026-08-07T09:00:00Z"
}
```

### 4.4 Rules For Error Details

- `details` phải chứa từng thuốc không đủ tồn.
- `shortageQuantity = requiredQuantity - availableQuantity`
- `availableQuantity` chỉ tính trên các lô hợp lệ theo mục 2.2.
- Không trả lô cụ thể trong response lỗi Phase 1; chỉ trả mức thiếu theo thuốc.

---

## 5. Allocation Rule

### 5.1 Allocation Requirement

Hệ thống **bắt buộc** lưu allocation theo đơn:

- Một `prescription_item` cấp từ lô nào
- Mỗi lô cấp bao nhiêu
- Ai cấp
- Cấp lúc nào

### 5.2 Allocation Granularity

Allocation được lưu theo cấp:

- `prescription`
- `prescription_item`
- `medicine_batch`

Điều này cho phép:

- Truy ngược bệnh nhân đã nhận thuốc từ lô nào
- Audit chính xác việc trừ tồn
- Hỗ trợ thu hồi thuốc theo lô trong tương lai

### 5.3 Allocation Example

Ví dụ một dòng đơn cần `70` viên:

- Lô `BATCH-A`, hết hạn `2026-10-01`, còn `20`
- Lô `BATCH-B`, hết hạn `2026-12-01`, còn `100`

Theo FEFO, allocation sẽ là:

```json
[
  {
    "batchNumber": "BATCH-A",
    "allocatedQuantity": 20
  },
  {
    "batchNumber": "BATCH-B",
    "allocatedQuantity": 50
  }
]
```

### 5.4 Allocation Persistence Baseline

Phase 1 chốt rằng phase sau phải có bảng lưu allocation riêng, ví dụ:

- `prescription_dispense_items`

Mỗi dòng allocation tối thiểu cần gắn với:

- `prescription_id`
- `prescription_item_id`
- `medicine_id`
- `medicine_batch_id`
- `dispensed_quantity`
- `dispensed_by`
- `dispensed_at`

---

## 6. Successful Dispense Outcome

Khi cấp phát thành công:

1. Tất cả các `prescription_item` đều được allocate đủ.
2. Tồn ở từng lô được trừ theo allocation.
3. Tồn tổng `stock_quantity` của thuốc được cập nhật tương ứng.
4. Đơn được chuyển trạng thái từ `PENDING_DISPENSE` sang `DISPENSED`.
5. Allocation được lưu lại đầy đủ.

---

## 7. Phase 1 Decisions Summary

Các quyết định đã chốt:

- Rule cấp phát: **FEFO theo `expiry_date`**
- Chỉ dùng lô: **`ACTIVE`, `quantity > 0`, `expiry_date >= today`**
- Lỗi thiếu tồn: **HTTP `409 Conflict`**, trả danh sách thuốc thiếu tồn theo từng thuốc
- Bắt buộc lưu: **allocation theo đơn xuống từng lô**

Các phase sau phải tuân thủ đúng các quyết định trên khi triển khai migration, repository, application service, controller, và frontend.
