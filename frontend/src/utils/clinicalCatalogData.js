export const clinicalCategories = [
  { key: 'ALL', label: 'Tất cả dịch vụ' },
  { key: 'XET_NGHIEM', label: 'Xét nghiệm Y học' },
  { key: 'CDHA', label: 'Chẩn đoán hình ảnh' },
  { key: 'THAM_DO_CHUC_NANG', label: 'Thăm dò chức năng' },
  { key: 'THU_THUAT', label: 'Thủ thuật / Khác' },
]

export const clinicalServiceCatalog = [
  { id: 'ORD-001', code: 'XN-XNM', name: 'Công thức máu toàn phần (22 thông số)', category: 'XET_NGHIEM', price: 95000, department: 'Phòng Xét nghiệm', preparation: 'Không cần nhịn ăn' },
  { id: 'ORD-002', code: 'XN-SHM-GLU', name: 'Đường huyết lúc đói (Glucose)', category: 'XET_NGHIEM', price: 45000, department: 'Phòng Xét nghiệm', preparation: 'Nhịn ăn từ 8 tiếng trước' },
  { id: 'ORD-003', code: 'XN-SHM-LIP', name: 'Bộ mỡ máu (Cholesterol, Triglyceride, HDL-C, LDL-C)', category: 'XET_NGHIEM', price: 180000, department: 'Phòng Xét nghiệm', preparation: 'Nhịn ăn 10-12 tiếng' },
  { id: 'ORD-004', code: 'XN-SHM-GAN', name: 'Men gan (AST/SGOT, ALT/SGPT, GGT)', category: 'XET_NGHIEM', price: 120000, department: 'Phòng Xét nghiệm', preparation: 'Không uống rượu bia trước 24h' },
  { id: 'ORD-005', code: 'XN-SHM-THAN', name: 'Chức năng thận (Ure, Creatinine)', category: 'XET_NGHIEM', price: 90000, department: 'Phòng Xét nghiệm', preparation: 'Không cần nhịn ăn' },
  { id: 'ORD-006', code: 'XN-NT', name: 'Tổng phân tích nước tiểu (10 thông số)', category: 'XET_NGHIEM', price: 50000, department: 'Phòng Xét nghiệm', preparation: 'Lấy nước tiểu giữa dòng' },
  { id: 'ORD-007', code: 'XN-DGD', name: 'Điện giải đồ (Na+, K+, Cl-)', category: 'XET_NGHIEM', price: 110000, department: 'Phòng Xét nghiệm', preparation: 'Không cần nhịn ăn' },
  { id: 'ORD-008', code: 'XN-HBA1C', name: 'Xét nghiệm HbA1c (Đánh giá đường huyết 3 tháng)', category: 'XET_NGHIEM', price: 160000, department: 'Phòng Xét nghiệm', preparation: 'Không cần nhịn ăn' },

  { id: 'ORD-010', code: 'CDHA-XQ-N', name: 'X-quang ngực thẳng (Tim phổi thẳng)', category: 'CDHA', price: 120000, department: 'Phòng X-quang', preparation: 'Tháo bỏ đồ kim loại vùng ngực' },
  { id: 'ORD-011', code: 'CDHA-XQ-CS', name: 'X-quang cột sống thắt lưng (2 tư thế)', category: 'CDHA', price: 180000, department: 'Phòng X-quang', preparation: 'Nhịn ăn nhẹ nếu chụp đường tiêu hóa' },
  { id: 'ORD-012', code: 'CDHA-SA-B', name: 'Siêu âm bụng tổng quát', category: 'CDHA', price: 150000, department: 'Phòng Siêu âm', preparation: 'Uống nhiều nước, nhịn tiểu' },
  { id: 'ORD-013', code: 'CDHA-SA-T', name: 'Siêu âm tim màu Doppler', category: 'CDHA', price: 350000, department: 'Phòng Siêu âm', preparation: 'Không cần chuẩn bị đặc biệt' },
  { id: 'ORD-014', code: 'CDHA-SA-G', name: 'Siêu âm tuyến giáp', category: 'CDHA', price: 180000, department: 'Phòng Siêu âm', preparation: 'Bộc lộ vùng cổ' },
  { id: 'ORD-015', code: 'CDHA-CT-SN', name: 'Chụp CT Scanner sọ não không tiêm thuốc', category: 'CDHA', price: 950000, department: 'Khoa CĐHA High-tech', preparation: 'Nhịn ăn 4 tiếng trước khi chụp' },
  { id: 'ORD-016', code: 'CDHA-MRI-CS', name: 'Chụp MRI cột sống thắt lưng', category: 'CDHA', price: 2200000, department: 'Khoa CĐHA High-tech', preparation: 'Tháo tất cả vật dụng kim loại, máy tạo nhịp' },

  { id: 'ORD-020', code: 'TD-ECG', name: 'Điện tâm đồ (ECG 12 chuyển đạo)', category: 'THAM_DO_CHUC_NANG', price: 80000, department: 'Phòng Điện tim', preparation: 'Nghỉ ngơi 15 phút trước khi đo' },
  { id: 'ORD-021', code: 'TD-NS-DD', name: 'Nội soi dạ dày - thực quản chẩn đoán', category: 'THAM_DO_CHUC_NANG', price: 600000, department: 'Phòng Nội soi', preparation: 'Nhịn ăn ít nhất 6-8 tiếng' },
  { id: 'ORD-022', code: 'TD-CNHH', name: 'Đo chức năng hô hấp (Hô hấp ký)', category: 'THAM_DO_CHUC_NANG', price: 200000, department: 'Phòng Thăm dò CNHH', preparation: 'Ngừng thuốc giãn phế quản 6-12h' },

  { id: 'ORD-030', code: 'TT-RVT', name: 'Rửa và băng vết thương nhỏ', category: 'THU_THUAT', price: 70000, department: 'Phòng Thủ thuật', preparation: 'Vệ sinh vùng thương tổn' },
  { id: 'ORD-031', code: 'TT-CBT', name: 'Cắt chỉ vết thương', category: 'THU_THUAT', price: 50000, department: 'Phòng Thủ thuật', preparation: 'Theo lịch hẹn cắt chỉ' },
]

export const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0)
}
