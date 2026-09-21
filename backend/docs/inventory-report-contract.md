# NCL-06-CN-013 — Hợp đồng backend chức năng Báo cáo xuất nhập tồn theo kỳ

## 1. Phạm vi

Tài liệu này xác định toàn bộ hợp đồng API, cấu trúc dữ liệu JSON & CSV, phân quyền bảo mật, quy tắc nghiệp vụ kế toán kho và ràng buộc kiểm toán cho chức năng **Báo cáo xuất nhập tồn theo kỳ** (User Story `NCL-06-CN-013`). Phạm vi thực hiện chỉ bao gồm backend, không thay đổi frontend.

---

## 2. Quy tắc nghiệp vụ & Quyền hạn

- **Phân quyền & Kiểm soát truy cập (QTN-01 / User Roles)**:
  - Chỉ các vai trò Dược sĩ (`PHARMACIST`), Quản lý phòng khám (`MANAGER`) và Quản trị viên (`ADMIN`) có quyền truy cập báo cáo và xuất tệp.
  - Bác sĩ (`DOCTOR`) và Lễ tân (`RECEPTIONIST`) bị từ chối truy cập với mã lỗi `403 Forbidden` và hệ thống tự động ghi nhật ký `ACCESS_DENIED` vào bảng `audit_logs` (tuân thủ giới hạn vai trò `VT-02`: *"không can thiệp kho thuốc"*).
  - Tầng Controller áp dụng `@RequirePermission`:
    - Xem báo cáo: `@RequirePermission(value = {"PHARMACY_READ", "REPORT_VIEW"}, operator = ANY)`
    - Xuất file: `@RequirePermission(value = {"PHARMACY_READ", "REPORT_EXPORT"}, operator = ANY)`
  - Tầng Application Service áp dụng `InventoryReportAuthorizer` để kiểm tra vai trò ngữ cảnh dữ liệu nhằm ngăn chặn hoàn toàn việc Bác sĩ (vốn sở hữu quyền `PHARMACY_READ` khi kê đơn) xem báo cáo kho.

- **Quy tắc cân đối số liệu kho (NCL-06-CN-013-TC-01 / QTN-32 / QTN-06)**:
  - Công thức kế toán bất biến:
    $$\text{Tồn cuối kỳ} = \text{Tồn đầu kỳ} + \text{Nhập trong kỳ} - \text{Xuất cấp phát} + \text{Trả lại} + \text{Điều chỉnh}$$
  - **Tồn đầu kỳ (`openingStock`)**: Tổng đại số của tất cả biến động kho (`quantityChange` trong bảng `stock_movements`) trước `00:00:00` ngày `from` theo múi giờ phòng khám (`Asia/Ho_Chi_Minh`).
  - **Nhập trong kỳ (`importQuantity`)**: Tổng lượng nhập từ các phiếu nhập kho (`StockMovementType.RECEIPT`) trong kỳ (số dương).
  - **Xuất cấp phát (`dispensedQuantity`)**: Tổng lượng cấp phát thuốc theo đơn (`StockMovementType.DISPENSE`) trong kỳ (hiển thị dưới dạng số dương trên báo cáo, trừ đi trong công thức).
  - **Trả lại (`returnedQuantity`)**: Tổng lượng thuốc bệnh nhân trả lại theo phiếu hủy/trả thuốc (`StockMovementType.RETURN`, NCL-06-CN-009) trong kỳ (số dương, cộng vào trong công thức).
  - **Điều chỉnh (`adjustedQuantity`)**: Tổng lượng biến động sau kiểm kê (`StockMovementType.ADJUSTMENT`) và hủy lô thuốc hết hạn (`StockMovementType.EXPIRE`, NCL-06-CN-010). Giá trị này mang dấu đại số (âm nếu kiểm kê thiếu/hủy lô, dương nếu kiểm kê thừa).
  - **Tồn cuối kỳ (`closingStock`)**: Tồn thực tế tại `23:59:59.999` ngày `to`.

- **Xử lý kỳ không phát sinh giao dịch (NCL-06-CN-013-TC-03)**:
  - Nếu trong kỳ báo cáo không có bất kỳ biến động kho nào:
    - `hasTransactions = false`.
    - `openingStock == closingStock`.
    - Hệ thống vẫn trả về danh sách tồn kho bình thường kèm thông báo `hasTransactions: false` (trên UI) và `Có giao dịch phát sinh: Không có giao dịch` (trong tệp CSV). Hệ thống **không** quăng lỗi hay trả về rỗng.

- **Quy tắc xuất tệp báo cáo (NCL-06-CN-013-TC-02 / QTN-02)**:
  - Tệp tải về có định dạng CSV với encoding UTF-8 có BOM (`\uFEFF`) để tương thích trực tiếp với Microsoft Excel trên mọi hệ điều hành.
  - Tên tệp: `bao-cao-xuat-nhap-ton-{from}-den-{to}.csv`.
  - Toàn bộ số liệu các cột chi tiết và dòng `Tổng cộng` trên tệp xuất khớp tuyệt đối với dữ liệu hiển thị trên màn hình.
  - Thao tác xuất tệp được ghi vết kiểm toán vào bảng `audit_logs` với `ActionType.EXPORT` và `ResourceType.OPERATIONAL_REPORT`.

---

## 3. API Contract

### 3.1 Xem báo cáo xuất nhập tồn
- **Endpoint**: `GET /inventory/reports/in-out-stock`
- **Quyền yêu cầu**: `@RequirePermission(value = {"PHARMACY_READ", "REPORT_VIEW"}, operator = ANY)` kết hợp `InventoryReportAuthorizer` (chỉ `PHARMACIST`, `MANAGER`, `ADMIN`).
- **Query Parameters**:
  - `from` (*bắt buộc*, `string`): Ngày bắt đầu kỳ, định dạng `yyyy-MM-dd`.
  - `to` (*bắt buộc*, `string`): Ngày kết thúc kỳ, định dạng `yyyy-MM-dd`. `from <= to`, khoảng thời gian tối đa 366 ngày.
  - `medicineId` (*tùy chọn*, `UUID`): Lọc theo ID thuốc cụ thể.
  - `keyword` (*tùy chọn*, `string`): Tìm kiếm theo mã thuốc hoặc tên thuốc.

- **Response `200 OK`**:
```json
{
  "from": "2026-08-01",
  "to": "2026-08-31",
  "generatedAt": "2026-09-21T10:00:00Z",
  "hasTransactions": true,
  "items": [
    {
      "medicineId": "f1e2d3c4-b5a6-7890-1234-56789abcdef0",
      "medicineCode": "MED-001",
      "medicineName": "Paracetamol 500mg",
      "unit": "Viên",
      "openingStock": 50,
      "importQuantity": 100,
      "dispensedQuantity": 30,
      "returnedQuantity": 10,
      "adjustedQuantity": -5,
      "closingStock": 125
    }
  ],
  "summary": {
    "totalMedicines": 1,
    "totalOpeningStock": 50,
    "totalImportQuantity": 100,
    "totalDispensedQuantity": 30,
    "totalReturnedQuantity": 10,
    "totalAdjustedQuantity": -5,
    "totalClosingStock": 125
  }
}
```

### 3.2 Xuất tệp báo cáo xuất nhập tồn
- **Endpoint**: `GET /inventory/reports/in-out-stock/export`
- **Quyền yêu cầu**: `@RequirePermission(value = {"PHARMACY_READ", "REPORT_EXPORT"}, operator = ANY)` kết hợp `InventoryReportAuthorizer` (chỉ `PHARMACIST`, `MANAGER`, `ADMIN`).
- **Query Parameters**: Giống mục 3.1 (`from`, `to`, `medicineId`, `keyword`).
- **Response `200 OK`**:
  - `Content-Type`: `text/csv;charset=UTF-8`
  - `Content-Disposition`: `attachment; filename="bao-cao-xuat-nhap-ton-{from}-den-{to}.csv"`
  - Nội dung file CSV mẫu:
```csv
BÁO CÁO XUẤT NHẬP TỒN KHO THUỐC
Từ ngày,2026-08-01
Đến ngày,2026-08-31
Thời điểm xuất,2026-09-21 17:00:00
Có giao dịch phát sinh,Có

STT,Mã thuốc,Tên thuốc,Đơn vị tính,Tồn đầu kỳ,Nhập trong kỳ,Xuất cấp phát,Trả lại,Điều chỉnh,Tồn cuối kỳ
1,MED-001,Paracetamol 500mg,Viên,50,100,30,10,-5,125
Tổng cộng,,,,50,100,30,10,-5,125
```

---

## 4. Bảng mã lỗi phản hồi (Error Codes)

| HTTP Status | Mã lỗi / Body | Trường hợp xảy ra |
|---|---|---|
| `400 Bad Request` | `VALIDATION_FAILED` | Thiếu `from` hoặc `to`, sai format ngày, `from > to`, hoặc khoảng ngày > 366 ngày. |
| `401 Unauthorized` | Không có token / Token hết hạn | Người dùng chưa xác thực hoặc phiên đăng nhập hết hạn. |
| `403 Forbidden` | `ACCESS_DENIED` | Bác sĩ (`DOCTOR`), Lễ tân (`RECEPTIONIST`) hoặc người dùng không có quyền truy cập báo cáo kho. Tự động ghi audit log `ACCESS_DENIED`. |
| `404 Not Found` | `MEDICINE_NOT_FOUND` | Truyền `medicineId` nhưng thuốc không tồn tại trong hệ thống. |
