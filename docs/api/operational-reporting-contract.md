# Operational Reporting API Contract

> Module: **NCL-08 - Báo cáo vận hành và nhật ký truy cập**
>
> User Stories:
> - **NCL-08-CN-001 - Báo cáo lượt khám**
> - **NCL-08-CN-002 - Báo cáo doanh thu**
>
> Base URL: `http://localhost:8080/api/v1`
>
> Auth: Bearer Token (JWT)
>
> Date: **2026-08-13**
>
> Status: **Phase 1 - Business Rule and Response Contract Baseline**

---

## 1. Scope

Tài liệu này chốt các quyết định Phase 1 cho 2 chức năng:

1. Báo cáo lượt khám đã xong theo khoảng thời gian.
2. Báo cáo doanh thu theo khoảng thời gian.
3. Quy tắc loại trừ hóa đơn điều chỉnh khỏi doanh thu thuần.
4. Format response chuẩn cho `summary`, `timeline`, và `export`.

Các quyết định trong tài liệu này là baseline để triển khai backend API, service tổng hợp dữ liệu, frontend integration, export file, và test case.

---

## 2. Roles And Access

### 2.1 Allowed Roles

Chốt cho Phase 1:

- `ADMIN`
- `MANAGER`

Người dùng không thuộc 2 vai trò trên:

- Không được xem báo cáo lượt khám.
- Không được xem báo cáo doanh thu.
- Không được export báo cáo.

### 2.2 Permission Direction

Phase 1 chốt theo hướng:

- Dùng route protection theo role `ADMIN`, `MANAGER`.
- Khi triển khai permission chi tiết ở backend, nên bổ sung permission riêng cho reporting thay vì tái dùng `INVOICE_READ`.

Đề xuất tên permission ở các giai đoạn sau:

- `REPORT_VIEW`
- `REPORT_EXPORT`

---

## 3. Time Range Rule

### 3.1 Input Range

Tất cả báo cáo Phase 1 dùng:

- `from`: ngày bắt đầu, định dạng `YYYY-MM-DD`
- `to`: ngày kết thúc, định dạng `YYYY-MM-DD`

### 3.2 Range Semantics

Khoảng thời gian được hiểu là:

- Bao gồm toàn bộ ngày `from` từ `00:00:00`
- Bao gồm toàn bộ ngày `to` đến `23:59:59.999999999`

Ở tầng backend nên chuẩn hóa thành:

- `fromInclusive = from at start of day`
- `toExclusive = to + 1 day at start of day`

Điều kiện lọc:

```text
timestamp >= fromInclusive AND timestamp < toExclusive
```

### 3.3 Validation

Request bị từ chối nếu:

- Thiếu `from`
- Thiếu `to`
- `from > to`

Khuyến nghị Phase 1:

- Cho phép kỳ tối đa 366 ngày để tránh query quá rộng.

---

## 4. Visit Report Rule

### 4.1 Business Goal

`NCL-08-CN-001` dùng để thống kê **lượt khám đã xong** trong một kỳ báo cáo.

### 4.2 Definition Of A Counted Visit

Một lượt khám được tính vào báo cáo nếu đồng thời thỏa cả 2 điều kiện:

- Thuộc khoảng thời gian báo cáo theo thời điểm `completedAt`.
- Có trạng thái hoàn tất nghiệp vụ khám, tức là `COMPLETED`.

### 4.3 Source Of Truth

Nguồn dữ liệu chuẩn là bảng/domain `visit`.

Không dùng:

- `medical_record.createdAt` để thay thế cho thời điểm hoàn tất.
- `queue_item.updatedAt` để thay thế cho hoàn tất lượt khám.
- Dữ liệu tạm ở localStorage frontend.

### 4.4 Counting Formula

Chốt công thức:

```text
visitCount = số lượng visit có status = COMPLETED và completedAt nằm trong kỳ
```

### 4.5 Timeline Rule

Biểu đồ lượt khám theo ngày được tính như sau:

- Mỗi dòng timeline tương ứng 1 ngày lịch trong khoảng `from..to`.
- Một lượt khám được cộng vào ngày chứa `completedAt`.
- Nếu một ngày không có dữ liệu thì vẫn trả về ngày đó với giá trị `0`.

### 4.6 Empty Data Rule

Nếu không có lượt khám nào trong kỳ:

- API vẫn trả về thành công.
- `visitCount = 0`
- Timeline vẫn trả đủ các ngày trong kỳ với `visitCount = 0`

---

## 5. Revenue Report Rule

### 5.1 Business Goal

`NCL-08-CN-002` dùng để thống kê **doanh thu thuần** trong một kỳ báo cáo.

### 5.2 Source Of Truth

Nguồn dữ liệu chuẩn là bảng/domain `invoice`.

Không dùng:

- Tổng tiền hiển thị trên frontend.
- localStorage.
- Dữ liệu payment rời nếu chưa được phản ánh thành invoice.

### 5.3 Revenue Components

Phase 1 chốt:

- `invoice gốc` là invoice tạo doanh thu dương.
- `adjustment invoice` là invoice điều chỉnh làm giảm doanh thu thuần.

### 5.4 Revenue Formula

Chốt công thức doanh thu:

```text
netRevenue = tổng totalAmount của invoice gốc trong kỳ
             - tổng totalAmount của adjustment invoice trong kỳ
```

Hay viết rõ hơn:

```text
netRevenue = SUM(originalInvoice.totalAmount)
             - SUM(adjustmentInvoice.totalAmount)
```

Trong đó:

- Invoice gốc là các invoice không phải loại điều chỉnh.
- Adjustment invoice là các invoice có `type = ADJUSTMENT`.

### 5.5 Date Field For Revenue Aggregation

Doanh thu được ghi nhận theo `invoice.createdAt`.

Nghĩa là:

- Invoice gốc được tính vào ngày tạo invoice gốc.
- Invoice điều chỉnh được trừ vào ngày tạo adjustment invoice.

Không hồi tố adjustment về ngày của invoice gốc trong Phase 1.

Lý do:

- Đơn giản, nhất quán với dữ liệu kế toán đang có trong hệ thống.
- Phản ánh đúng phát sinh điều chỉnh trong kỳ đang xem.

### 5.6 Timeline Rule

Biểu đồ doanh thu theo ngày được tính như sau:

- Mỗi ngày bắt đầu từ `0`.
- Cộng doanh thu từ invoice gốc tạo trong ngày.
- Trừ tổng tiền của adjustment invoice tạo trong ngày.
- Nếu không có phát sinh thì vẫn trả về ngày đó với `revenue = 0`.

### 5.7 Empty Data Rule

Nếu không có hóa đơn nào trong kỳ:

- API vẫn trả về thành công.
- `revenue = 0`
- Timeline vẫn trả đủ các ngày với `revenue = 0`

### 5.8 Notes For Future Phase

Các trường hợp sau chưa xử lý khác đi trong Phase 1:

- Phân loại doanh thu theo dịch vụ/thuốc.
- Phân biệt doanh thu đã thu tiền và doanh thu đã lập hóa đơn.
- Điều chỉnh tăng doanh thu.
- Hủy hóa đơn hoặc void invoice nếu sau này domain bổ sung.

Nếu phát sinh các loại nghiệp vụ trên, contract Phase 1 sẽ cần mở rộng.

---

## 6. Shared Reporting Model

### 6.1 Daily Timeline Row

Mọi báo cáo timeline Phase 1 dùng cấu trúc ngày cố định:

```json
{
  "date": "2026-08-13",
  "visitCount": 0,
  "revenue": 0
}
```

Quy ước:

- `date`: luôn là `YYYY-MM-DD`
- `visitCount`: số nguyên không âm
- `revenue`: số tiền thuần trong ngày, đơn vị VND, kiểu number

Với báo cáo chỉ quan tâm 1 chỉ số:

- Các field còn lại vẫn có thể giữ để frontend dùng chung component timeline.

---

## 7. Summary Endpoint Contract

### 7.1 Endpoint

```text
GET /reports/summary?from=YYYY-MM-DD&to=YYYY-MM-DD
```

### 7.2 Purpose

Trả về số liệu tổng hợp cho kỳ báo cáo:

- Tổng lượt khám đã xong
- Tổng doanh thu thuần

### 7.3 Response Shape

```json
{
  "from": "2026-08-01",
  "to": "2026-08-13",
  "visitCount": 128,
  "revenue": 45200000,
  "currency": "VND"
}
```

### 7.4 Field Meaning

- `from`: ngày bắt đầu đã áp dụng
- `to`: ngày kết thúc đã áp dụng
- `visitCount`: tổng số lượt khám `COMPLETED` theo `completedAt`
- `revenue`: doanh thu thuần theo công thức ở mục 5.4
- `currency`: cố định là `VND`

### 7.5 Empty Response Example

```json
{
  "from": "2026-08-01",
  "to": "2026-08-13",
  "visitCount": 0,
  "revenue": 0,
  "currency": "VND"
}
```

---

## 8. Timeline Endpoint Contract

### 8.1 Endpoint

```text
GET /reports/visits-timeline?from=YYYY-MM-DD&to=YYYY-MM-DD
```

### 8.2 Purpose

Trả về timeline theo ngày cho cả 2 chỉ số chính để frontend có thể dùng:

- Tab báo cáo lượt khám
- Tab báo cáo doanh thu

### 8.3 Response Shape

```json
{
  "from": "2026-08-01",
  "to": "2026-08-03",
  "items": [
    {
      "date": "2026-08-01",
      "visitCount": 12,
      "revenue": 5400000
    },
    {
      "date": "2026-08-02",
      "visitCount": 9,
      "revenue": 3100000
    },
    {
      "date": "2026-08-03",
      "visitCount": 0,
      "revenue": -200000
    }
  ]
}
```

### 8.4 Timeline Semantics

- `visitCount` phản ánh lượt khám hoàn tất trong ngày.
- `revenue` phản ánh doanh thu thuần trong ngày.
- `revenue` có thể âm nếu trong ngày chỉ phát sinh adjustment invoice.

### 8.5 Empty Response Example

```json
{
  "from": "2026-08-01",
  "to": "2026-08-03",
  "items": [
    {
      "date": "2026-08-01",
      "visitCount": 0,
      "revenue": 0
    },
    {
      "date": "2026-08-02",
      "visitCount": 0,
      "revenue": 0
    },
    {
      "date": "2026-08-03",
      "visitCount": 0,
      "revenue": 0
    }
  ]
}
```

---

## 9. Export Endpoint Contract

### 9.1 Endpoint

```text
GET /reports/export?from=YYYY-MM-DD&to=YYYY-MM-DD
```

### 9.2 Output Format

Phase 1 chốt export dạng `CSV`.

Response headers nên là:

```text
Content-Type: text/csv; charset=UTF-8
Content-Disposition: attachment; filename="operational-report-2026-08-01-to-2026-08-13.csv"
```

### 9.3 Export Content Scope

File export Phase 1 gồm 2 phần dữ liệu chính:

1. Summary:
   - Khoảng thời gian
   - Tổng lượt khám đã xong
   - Tổng doanh thu thuần
2. Timeline theo ngày:
   - Ngày
   - Số lượt khám
   - Doanh thu thuần

### 9.4 CSV Layout

```csv
OPERATIONAL REPORT
From,2026-08-01
To,2026-08-13
Visit Count,128
Revenue (VND),45200000

Date,Visit Count,Revenue (VND)
2026-08-01,12,5400000
2026-08-02,9,3100000
2026-08-03,0,-200000
```

### 9.5 Export Audit Rule

Mỗi lần export thành công, hệ thống phải lưu:

- Người export
- Vai trò tại thời điểm export
- Khoảng thời gian báo cáo
- Thời điểm export
- Loại báo cáo: `OPERATIONAL_REPORT`

Phase 1 mới chốt rule và contract nghiệp vụ; phần persistence/audit implementation sẽ làm ở giai đoạn sau.

---

## 10. Error Handling Contract

### 10.1 Invalid Range

HTTP status:

- `400 Bad Request`

Ví dụ:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "from must be before or equal to to."
}
```

### 10.2 Unauthorized Role

HTTP status:

- `403 Forbidden`

Ví dụ:

```json
{
  "code": "ACCESS_DENIED",
  "message": "You do not have permission to access this report."
}
```

---

## 11. Implementation Notes

### 11.1 Backend

Giai đoạn implement nên tách:

- `ReportsController`
- `GetOperationalSummaryService`
- `GetOperationalTimelineService`
- `ExportOperationalReportService`

### 11.2 Frontend

`ReportsPage` phải chuyển sang:

- Không tự tổng hợp từ localStorage
- Chỉ render dữ liệu do backend trả về
- Dùng `summary` cho KPI
- Dùng `timeline.items` cho chart và table

### 11.3 Test Focus

Tối thiểu cần có test cho:

- Có dữ liệu lượt khám hoàn tất
- Không có dữ liệu lượt khám
- Có invoice gốc
- Có adjustment invoice và trừ đúng
- Doanh thu âm trong ngày do adjustment
- Role không hợp lệ bị chặn
- Export ghi nhận lịch sử

---

## 12. Final Decisions Locked In Phase 1

Các quyết định đã chốt:

- Báo cáo lượt khám chỉ tính **lượt khám đã xong**, tức `visit.status = COMPLETED`.
- Mốc thời gian tính lượt khám là `visit.completedAt`.
- Báo cáo doanh thu dùng công thức:
  - `SUM(invoice gốc) - SUM(adjustment invoice)`
- Mốc thời gian tính doanh thu là `invoice.createdAt`.
- Adjustment invoice được trừ vào **ngày nó được tạo**, không hồi tố về ngày invoice gốc.
- `summary` trả về tổng lượt khám và tổng doanh thu thuần.
- `timeline` trả về đủ từng ngày trong kỳ, kể cả ngày không có dữ liệu.
- `export` chốt dùng `CSV` cho Phase 1.

