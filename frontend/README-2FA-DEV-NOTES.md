# ⚠️ LƯU Ý BẢO MẬT - CHỈ ÁP DỤNG CHO MÔI TRƯỜNG DEV

Tính năng Xác thực hai lớp (2FA - NCL-01-CN-006) hiện đang dùng cơ chế **MOCK (giả lập gửi mã)**. Mã xác thực (OTP) được in trực tiếp ra console / terminal của Backend (`mvn spring-boot:run`) để phục vụ việc kiểm thử nội bộ nhanh chóng thay vì gửi qua SMS/Email thật.

---

## 🚨 BẮT BUỘC THỰC HIỆN TRƯỚC KHI TRIỂN KHAI PRODUCTION (NGƯỜI DÙNG THẬT)

1. **Gỡ bỏ log mã OTP dạng plaintext ở Backend**:
   - Truy cập file: `backend/src/main/java/com/benhsoan/infrastructure/notification/MockTwoFactorCodeDeliveryAdapter.java`
   - Xóa bỏ hoặc tắt dòng `log.warn(...)` in `code` ra console.
2. **Tích hợp nhà cung cấp SMS / Email thật**:
   - Thay thế `MockTwoFactorCodeDeliveryAdapter` bằng Adapter kết nối với dịch vụ SMS/Email Gateway thật (như Twilio, SendGrid, ESMS, Viettel SMS Brand...).
3. **Tắt biểu ngữ DEV MODE ở Frontend**:
   - Truy cập file: `frontend/src/pages/TwoFactorVerifyPage.jsx`
   - Chuyển cờ cấu hình `const SHOW_DEV_HINT = false` (hoặc xóa khối Banner).
4. **Kiểm tra nhật ký kiểm toán (Audit Check)**:
   - Rà soát toàn bộ log hệ thống ở Backend và Frontend để đảm bảo tuyệt đối không có mật khẩu hay mã OTP nào bị rò rỉ ra console hay DevTools.
