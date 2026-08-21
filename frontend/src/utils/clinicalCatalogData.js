export const clinicalCategories = [
  { key: 'ALL', label: 'Tất cả dịch vụ' },
  { key: 'XET_NGHIEM', label: 'Xét nghiệm Y học' },
  { key: 'CDHA', label: 'Chẩn đoán hình ảnh' },
  { key: 'THAM_DO_CHUC_NANG', label: 'Thăm dò chức năng' },
  { key: 'THU_THUAT', label: 'Thủ thuật / Khác' },
]

export const clinicalServiceCatalog = [
  { id: 'c1000000-0000-0000-0000-000000000003', code: 'LAB-CBC', name: 'Công thức máu toàn bộ', category: 'XET_NGHIEM', price: 95000, department: 'Phòng Xét nghiệm', preparation: 'Không cần nhịn ăn' },
  { id: 'c1000000-0000-0000-0000-000000000001', code: 'LAB-GLU', name: 'Đường huyết lúc đói (Glucose)', category: 'XET_NGHIEM', price: 45000, department: 'Phòng Xét nghiệm', preparation: 'Nhịn ăn từ 8 tiếng trước' },
  { id: 'c1000000-0000-0000-0000-000000000007', code: 'LAB-HBA1C', name: 'Xét nghiệm HbA1c', category: 'XET_NGHIEM', price: 160000, department: 'Phòng Xét nghiệm', preparation: 'Không cần nhịn ăn' },
  { id: 'c1000000-0000-0000-0000-000000000004', code: 'LAB-HGB', name: 'Định lượng Hemoglobin', category: 'XET_NGHIEM', price: 40000, department: 'Phòng Xét nghiệm', preparation: 'Không cần nhịn ăn' },
  { id: 'c1000000-0000-0000-0000-000000000005', code: 'LAB-PLT', name: 'Đếm số lượng tiểu cầu', category: 'XET_NGHIEM', price: 40000, department: 'Phòng Xét nghiệm', preparation: 'Không cần nhịn ăn' },
  { id: 'c1000000-0000-0000-0000-000000000008', code: 'LAB-UREA', name: 'Định lượng Ure máu', category: 'XET_NGHIEM', price: 45000, department: 'Phòng Xét nghiệm', preparation: 'Không cần nhịn ăn' },
  { id: 'c1000000-0000-0000-0000-000000000009', code: 'LAB-CREA', name: 'Định lượng Creatinin máu', category: 'XET_NGHIEM', price: 45000, department: 'Phòng Xét nghiệm', preparation: 'Không cần nhịn ăn' },
  { id: 'c1000000-0000-0000-0000-000000000010', code: 'LAB-AST', name: 'Men gan AST (SGOT)', category: 'XET_NGHIEM', price: 40000, department: 'Phòng Xét nghiệm', preparation: 'Không uống rượu bia trước 24h' },
  { id: 'c1000000-0000-0000-0000-000000000011', code: 'LAB-ALT', name: 'Men gan ALT (SGPT)', category: 'XET_NGHIEM', price: 40000, department: 'Phòng Xét nghiệm', preparation: 'Không uống rượu bia trước 24h' },
  { id: 'c1000000-0000-0000-0000-000000000012', code: 'LAB-CHOL', name: 'Cholesterol toàn phần', category: 'XET_NGHIEM', price: 45000, department: 'Phòng Xét nghiệm', preparation: 'Nhịn ăn 10-12 tiếng' },
  { id: 'c1000000-0000-0000-0000-000000000013', code: 'LAB-TG', name: 'Triglycerid', category: 'XET_NGHIEM', price: 45000, department: 'Phòng Xét nghiệm', preparation: 'Nhịn ăn 10-12 tiếng' },
  { id: 'c1000000-0000-0000-0000-000000000014', code: 'LAB-LDL', name: 'Cholesterol LDL', category: 'XET_NGHIEM', price: 50000, department: 'Phòng Xét nghiệm', preparation: 'Nhịn ăn 10-12 tiếng' },
  { id: 'c1000000-0000-0000-0000-000000000015', code: 'LAB-HDL', name: 'Cholesterol HDL', category: 'XET_NGHIEM', price: 50000, department: 'Phòng Xét nghiệm', preparation: 'Nhịn ăn 10-12 tiếng' },
  { id: 'c1000000-0000-0000-0000-000000000016', code: 'LAB-URIC', name: 'Acid uric máu', category: 'XET_NGHIEM', price: 45000, department: 'Phòng Xét nghiệm', preparation: 'Không cần nhịn ăn' },
  { id: 'c1000000-0000-0000-0000-000000000017', code: 'LAB-CRP', name: 'Protein C phản ứng định lượng (CRP)', category: 'XET_NGHIEM', price: 90000, department: 'Phòng Xét nghiệm', preparation: 'Không cần nhịn ăn' },
  { id: 'c1000000-0000-0000-0000-000000000018', code: 'LAB-HBSAG', name: 'Xét nghiệm HBsAg', category: 'XET_NGHIEM', price: 80000, department: 'Phòng Xét nghiệm', preparation: 'Không cần nhịn ăn' },
  { id: 'c1000000-0000-0000-0000-000000000021', code: 'LAB-URINE', name: 'Tổng phân tích nước tiểu (10 thông số)', category: 'XET_NGHIEM', price: 40000, department: 'Phòng Xét nghiệm', preparation: 'Lấy nước tiểu giữa dòng' },

  { id: 'c1000000-0000-0000-0000-000000000023', code: 'IMG-CXR', name: 'X-quang ngực thẳng', category: 'CDHA', price: 90000, department: 'Phòng X-quang', preparation: 'Tháo bỏ đồ kim loại vùng ngực' },
  { id: 'c1000000-0000-0000-0000-000000000024', code: 'IMG-AXR', name: 'X-quang bụng không chuẩn bị', category: 'CDHA', price: 90000, department: 'Phòng X-quang', preparation: 'Tháo bỏ đồ kim loại' },
  { id: 'c1000000-0000-0000-0000-000000000025', code: 'IMG-US-ABD', name: 'Siêu âm bụng tổng quát', category: 'CDHA', price: 120000, department: 'Phòng Siêu âm', preparation: 'Uống nhiều nước, nhịn tiểu' },
  { id: 'c1000000-0000-0000-0000-000000000026', code: 'IMG-US-THY', name: 'Siêu âm tuyến giáp', category: 'CDHA', price: 120000, department: 'Phòng Siêu âm', preparation: 'Bộc lộ vùng cổ' },
  { id: 'c1000000-0000-0000-0000-000000000002', code: 'IMG-CTH', name: 'CT scanner sọ não không tiêm thuốc', category: 'CDHA', price: 950000, department: 'Khoa CĐHA', preparation: 'Nhịn ăn 4 tiếng trước khi chụp' },
  { id: 'c1000000-0000-0000-0000-000000000027', code: 'IMG-CT-ABD', name: 'CT scanner ổ bụng', category: 'CDHA', price: 1200000, department: 'Khoa CĐHA', preparation: 'Nhịn ăn 4 tiếng trước khi chụp' },
  { id: 'c1000000-0000-0000-0000-000000000028', code: 'IMG-MRI-BRAIN', name: 'MRI sọ não', category: 'CDHA', price: 1800000, department: 'Khoa CĐHA', preparation: 'Tháo tất cả kim loại, máy tạo nhịp' },

  { id: 'c1000000-0000-0000-0000-000000000030', code: 'OTH-ECG', name: 'Điện tâm đồ (ECG)', category: 'THAM_DO_CHUC_NANG', price: 50000, department: 'Phòng Điện tim', preparation: 'Nghỉ ngơi 15 phút trước khi đo' },
  { id: 'c1000000-0000-0000-0000-000000000031', code: 'OTH-ECHO', name: 'Siêu âm tim Doppler màu', category: 'THAM_DO_CHUC_NANG', price: 250000, department: 'Phòng Siêu âm tim', preparation: 'Không cần chuẩn bị đặc biệt' },
  { id: 'c1000000-0000-0000-0000-000000000032', code: 'OTH-SPIRO', name: 'Đo chức năng hô hấp (Hô hấp ký)', category: 'THAM_DO_CHUC_NANG', price: 150000, department: 'Phòng Thăm dò CNHH', preparation: 'Ngừng thuốc giãn phế quản 6-12h' },
  { id: 'c1000000-0000-0000-0000-000000000033', code: 'OTH-ENDO-GI', name: 'Nội soi dạ dày tá tràng chẩn đoán', category: 'THAM_DO_CHUC_NANG', price: 600000, department: 'Phòng Nội soi', preparation: 'Nhịn ăn ít nhất 6-8 tiếng' },
  { id: 'c1000000-0000-0000-0000-000000000034', code: 'OTH-ENDO-COLON', name: 'Nội soi đại tràng toàn bộ', category: 'THAM_DO_CHUC_NANG', price: 800000, department: 'Phòng Nội soi', preparation: 'Làm sạch ruột theo hướng dẫn' },
  { id: 'c1000000-0000-0000-0000-000000000037', code: 'OTH-DEXA', name: 'Đo mật độ xương DEXA', category: 'THAM_DO_CHUC_NANG', price: 200000, department: 'Phòng Đo mật độ xương', preparation: 'Không uống canxi trước 24h' },
]

export const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0)
}
