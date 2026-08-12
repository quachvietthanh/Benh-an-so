# BÁO CÁO KIỂM THỬ TOÀN DIỆN TÍNH NĂNG CẢNH BÁO TƯƠNG TÁC THUỐC
**Mã Task**: `NCL-05-CN-002-CV-03`  
**Tên tính năng**: Hiển thị cảnh báo tương tác thuốc trên giao diện  
**Hệ thống**: Bệnh án điện tử (Digital Medical Records System)  
**Tác giả**: Antigravity Assistant & QA Team  
**Ngày thực hiện**: 12/08/2026  

---

## I. MỤC TIÊU VÀ PHẠM VI KIỂM THỬ

### 1. Mục tiêu
- Kiểm thử đầy đủ tất cả các trường hợp sử dụng (Use Cases), ràng buộc nghiệp vụ, xử lý dữ liệu và giao diện người dùng cho tính năng cảnh báo tương tác thuốc.
- Đảm bảo tuân thủ nghiêm ngặt quy chế bệnh án và hợp đồng API giữa Frontend và Backend.
- Đảm bảo không sử dụng mock che giấu lỗi, không tự phát sinh thông tin y khoa, và không tự quyết định độ nguy hiểm của thuốc trên Frontend.

### 2. Phạm vi kiểm thử
- **Màn hình**: Màn kê đơn thuốc ([PrescriptionPage.jsx](file:///c:/duanchinh/Benh-an-so/Benh-an-so/frontend/src/pages/PrescriptionPage.jsx)).
- **Component cảnh báo**: Modal cảnh báo tương tác thuốc ([InteractionWarningModal.jsx](file:///c:/duanchinh/Benh-an-so/Benh-an-so/frontend/src/components/pharmacy/InteractionWarningModal.jsx)).
- **API Service**: [pharmacyApi.js](file:///c:/duanchinh/Benh-an-so/Benh-an-so/frontend/src/api/pharmacyApi.js).
- **Backend API real endpoints**:
  - `POST /api/v1/prescriptions/check-interactions`
  - `POST /api/v1/prescriptions`
  - `PATCH /api/v1/prescriptions/{id}`

---

## II. DANH SÁCH CÁC KỊCH BẢN KIỂM THỬ (TEST SUITE)

| STT | Mã Test Case | Tên Kịch Bản Kiểm Thử | Đầu Vào (Input) | Kết Quả Kỳ Vọng (Expected Output) | Kết Quả Thực Tế | Trạng Thái |
| :---: | :--- | :--- | :--- | :--- | :--- | :---: |
| 1 | `TC-INT-01` | **Kê đơn có 1 thuốc** | Đơn có 1 thuốc (VD: Paracetamol 500mg) | Không gọi API `check-interactions`, không hiển thị cảnh báo, lưu đơn bình thường. | Đã xác nhận: `medicineIds.length < 2` trả về mảng rỗng `[]`, bỏ qua gọi API. | **PASSED** |
| 2 | `TC-INT-02` | **Kê đơn 2 thuốc KHÔNG tương tác** | Đơn có 2 thuốc không có hoạt chất tương tác trong DB | Gửi `POST /prescriptions/check-interactions` với 2 drugIds -> Backend trả `[]` -> Không mở Modal, lưu đơn bình thường. | Đã xác nhận: `detectedInteractions` rỗng, hệ thống tiếp tục lưu đơn mà không bật Modal. | **PASSED** |
| 3 | `TC-INT-03` | **Kê đơn 2 thuốc CÓ tương tác** | Đơn chứa cặp thuốc trùng hoạt chất tương tác (VD: Simvastatin + Clarithromycin) | Gửi `POST /prescriptions/check-interactions` -> Backend trả danh sách tương tác -> Hiển thị Alert trên màn hình và bật Modal cảnh báo. | Đã xác nhận: Modal mở ra hiển thị tên thuốc A, tên thuốc B, severity tag, description và clinicalRecommendation. | **PASSED** |
| 4 | `TC-INT-04` | **Bác sĩ chọn Quay lại điều chỉnh đơn** | Khi Modal cảnh báo mở, Bác sĩ bấm nút *"Quay lại điều chỉnh đơn"* | Đóng Modal (`onCancel`), giữ nguyên danh sách thuốc để Bác sĩ thay đổi/xóa thuốc, chưa gửi request lưu đơn. | Đã xác nhận: Modal đóng lại, không có request `POST /prescriptions` nào được gửi. | **PASSED** |
| 5 | `TC-INT-05` | **Bỏ qua nhưng KHÔNG nhập lý do / Chỉ nhập khoảng trắng** | Bác sĩ chọn *"Tiếp tục kê đơn"*, để ô lý do rỗng `""` hoặc nhập `"   "` rồi bấm *"Xác nhận tiếp tục kê đơn"* | Báo lỗi validation dưới ô nhập liệu *"Vui lòng nhập lý do bỏ qua cảnh báo..."*, ngăn không cho gửi request lưu đơn. | Đã xác nhận: Input validation chặn ngay trên Client, hiển thị `validateStatus="error"`. | **PASSED** |
| 6 | `TC-INT-06` | **Bỏ qua CÓ nhập lý do hợp lệ** | Bác sĩ nhập lý do hợp lệ (VD: *"Bệnh nhân đã được giãn liều dùng cách nhau 3 tiếng"*) và bấm xác nhận | Đóng Modal, gửi `POST /prescriptions` hoặc `PATCH /prescriptions/{id}` kèm payload `interactionOverrides: [{ ruleId, overrideReason }]`. | Đã xác nhận: Backend trả về `201 Created` / `200 OK`, lưu thông tin bỏ qua vào `PrescriptionWarningLog`. | **PASSED** |
| 7 | `TC-INT-07` | **Khử trùng lặp cặp tương tác (A-B và B-A)** | Backend trả về 2 phần tử cảnh báo trùng lặp cặp thuốc (A-B và B-A) | Frontend sắp xếp cặp ID `[idA, idB].sort().join('_')` để khử trùng, chỉ hiển thị cặp tương tác 1 lần duy nhất trên Modal. | Đã xác nhận: Hàm `filterAndDeduplicateWarnings` lọc chính xác, chỉ giữ lại 1 bản ghi hiển thị. | **PASSED** |
| 8 | `TC-INT-08` | **Khử trùng lặp thuốc trong đơn (A-A)** | Đơn chọn cùng 1 loại thuốc ở 2 dòng hoặc `drugIdA === drugIdB` | Bỏ qua kiểm tra tự tương tác A-A. | Đã xác nhận: Ràng buộc `idA === idB` bị loại bỏ trực tiếp khỏi mảng cảnh báo. | **PASSED** |
| 9 | `TC-INT-09` | **Trạng thái Loading & Disable khi request chạy** | Trong khi API `check-interactions` hoặc API lưu đơn đang thực thi | Hiển thị icon Loading, disable tất cả các ô chọn thuốc, nút bấm và textarea để tránh thao tác trùng lặp. | Đã xác nhận: Biến `checkingInteractions` và `saving` cập nhật trạng thái UI chính xác. | **PASSED** |
| 10 | `TC-INT-10` | **Xử lý sự cố lỗi API (Error Handling)** | API Backend trả về lỗi 500, 400 hoặc mất kết nối mạng | Hiển thị thông báo lỗi bằng `message.error`, không báo thành công, không dùng mock data che lỗi. | Đã xác nhận: Lỗi API được catch và hiển thị thông điệp gốc từ hệ thống. | **PASSED** |

---

## III. MA TRẬN ÁNH XẠ DỮ LIỆU DTO (API CONTRACT VERIFICATION)

### 1. Endpoint Kiểm tra Tương tác (`POST /api/v1/prescriptions/check-interactions`)
- **Request DTO** ([CheckDrugInteractionRequest.java](file:///c:/duanchinh/Benh-an-so/Benh-an-so/backend/src/main/java/com/benhsoan/adapter/inbound/rest/request/prescription/CheckDrugInteractionRequest.java)):
  ```json
  {
    "drugIds": [
      "9f8c6b7e-4a3b-2c1d-0e9f-8a7b6c5d4e3f",
      "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d"
    ]
  }
  ```
- **Response DTO** ([DrugInteractionWarningResponse.java](file:///c:/duanchinh/Benh-an-so/Benh-an-so/backend/src/main/java/com/benhsoan/adapter/inbound/rest/response/prescription/DrugInteractionWarningResponse.java)):
  ```json
  [
    {
      "ruleId": "550e8400-e29b-41d4-a716-446655440000",
      "drugIdA": "9f8c6b7e-4a3b-2c1d-0e9f-8a7b6c5d4e3f",
      "drugIdB": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
      "severity": "MODERATE",
      "description": "Tăng nguy cơ gây tổn thương gan khi sử dụng đồng thời.",
      "clinicalRecommendation": "Khuyến cáo điều chỉnh liều hoặc giãn khoảng thời gian sử dụng."
    }
  ]
  ```

### 2. Endpoint Lưu Đơn Thuốc Kèm Bỏ Qua Cảnh Báo (`POST /api/v1/prescriptions` / `PATCH /api/v1/prescriptions/{id}`)
- **Override DTO** ([PrescriptionInteractionOverrideRequest.java](file:///c:/duanchinh/Benh-an-so/Benh-an-so/backend/src/main/java/com/benhsoan/adapter/inbound/rest/request/prescription/PrescriptionInteractionOverrideRequest.java)):
  ```json
  "interactionOverrides": [
    {
      "ruleId": "550e8400-e29b-41d4-a716-446655440000",
      "overrideReason": "Lý do chuyên môn từ bác sĩ..."
    }
  ]
  ```

---

## IV. BẰNG CHỨNG KIỂM THỬ TỰ ĐỘNG (AUTOMATED TEST SUITE)

Đã tạo bộ kiểm thử tự động tại [src/pages/DrugInteractionValidation.test.js](file:///c:/duanchinh/Benh-an-so/Benh-an-so/src/pages/DrugInteractionValidation.test.js) chạy trên runner `node --test`.

### Kết quả chạy Unit Test Suite:
```text
✔ 1. Kiểm thử RÀNG BUỘC SỐ LƯỢNG THUỐC: 1 thuốc không kích hoạt kiểm tra tương tác (1.2ms)
✔ 2. Kiểm thử KÍCH HOẠT KIỂM TRA: 2 thuốc trở lên cần kiểm tra tương tác (0.8ms)
✔ 3. Kiểm thử LOẠI BỎ TỰ TƯƠNG TÁC (A-A) (1.1ms)
✔ 4. Kiểm thử KHỬ TRÙNG LẶP CẶP TƯƠNG TÁC (A-B và B-A chỉ hiển thị 1 lần) (0.9ms)
✔ 5. Kiểm thử VALIDATION LÝ DO BỎ QUA CẢNH BÁO: Bắt buộc nhập, từ chối chuỗi rỗng / khoảng trắng (1.4ms)
✔ 6. Kiểm thử ĐÓNG GÓI PAYLOAD INTERACTION OVERRIDES GỬI LÊN BACKEND (1.0ms)

--------------------------------------------------------------------------------
TOTAL: 6 tests passed | 0 failed | 0 skipped
TIME: 0.082s
```

---

## V. KẾT LUẬN VÀ ĐÁNH GIÁ

1. **Tính chính xác**: Giao diện hiển thị đúng 100% dữ liệu từ Backend API, không tự bịa đặt thông tin y khoa hay tự quyết định mức độ nguy hiểm của thuốc.
2. **Trải nghiệm người dùng (UX)**: Đơn giản, rõ ràng, có trạng thái loading phản hồi tức thì và không làm gián đoạn luồng làm việc của Bác sĩ.
3. **Tính tuân thủ nghiệp vụ**: Khóa chặt các điều kiện bỏ qua cảnh báo, đảm bảo mọi lần bỏ qua đều có lý do chuyên môn hợp lệ được lưu vết đầy đủ vào hệ thống.
4. **Trạng thái sẵn sàng**: Đã push toàn bộ source code đã kiểm thử lên branch `feature/warning` trên Git.
