# NCL-12-CN-002 — Hợp đồng cấp mã đơn thuốc điện tử

## Phạm vi

Tài liệu này chốt cách backend thực hiện NCL-12-CN-002. Không tạo endpoint
cấp mã riêng, không thêm trạng thái đơn mới và không bao gồm giao diện.

## Quy ước nghiệp vụ

- `POST /prescriptions` là thời điểm đơn được hoàn tất lần đầu. Use case chỉ
  tạo đơn khi dữ liệu thuốc đã hợp lệ theo NCL-12-CN-001 và đơn được lưu ở
  trạng thái `PENDING_DISPENSE`.
- Chỉ bác sĩ có quyền tạo đơn; kiểm tra bác sĩ phụ trách bệnh án/lượt khám vẫn
  thuộc `PrescriptionClinicalContextValidator`.
- Trong cùng transaction tạo đơn, backend lấy số kế tiếp của prefix `RX` từ
  `prescription_code_sequences`, định dạng thành `RX` cộng sáu chữ số và gắn
  vào `prescriptions.prescription_code`.
- `prescription_code` là định danh bất biến. Luồng `PATCH /prescriptions/{id}`
  chỉ thay nội dung đơn và luôn giữ mã hiện có.
- `prescribedAt` là thời điểm mã được cấp. Trường này cùng bản ghi audit tạo
  đơn (có `prescriptionCode` trong detail) là lịch sử cấp mã; không thêm một
  cột thời gian trùng lặp.

## Bảo đảm tính duy nhất

- `prescription_code_sequences` cấp số kế tiếp atomically tại database.
- Unique constraint `uk_prescriptions_code` là hàng rào cuối cùng, không cho
  hai đơn lưu cùng mã.
- Số sequence bị bỏ qua do transaction thất bại được chấp nhận; yêu cầu chỉ
  đòi mã duy nhất, không đòi dãy số liền nhau.

## Hợp đồng API liên quan

`POST /prescriptions` trả `201 Created` cùng `prescriptionCode` và
`prescribedAt`. Các API đọc và cập nhật đơn cũng trả lại cùng hai trường này.
Client không gửi mã đơn hoặc thời điểm cấp mã trong request.

## Tiêu chí kiểm chứng backend

1. Tạo đơn hợp lệ trả mã `RX` duy nhất và `prescribedAt` khác rỗng.
2. Các yêu cầu tạo đơn đồng thời nhận các mã khác nhau.
3. Chỉnh sửa đơn `PENDING_DISPENSE` không đổi mã và không đổi `prescribedAt`.
4. Bản ghi audit tạo đơn chứa mã đơn; constraint unique chặn mọi bản ghi trùng.
